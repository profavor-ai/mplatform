package com.classification.domain_system.integration;

import com.classification.domain_system.entity.ClassificationNode;
import com.classification.domain_system.entity.IntegrationChannel;
import com.classification.domain_system.entity.Record;
import com.classification.domain_system.repository.ClassificationNodeRepository;
import com.classification.domain_system.repository.IntegrationChannelRepository;
import com.classification.domain_system.repository.RecordRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InboundIntegrationService {

    private final IntegrationChannelRepository channelRepository;
    private final ClassificationNodeRepository nodeRepository;
    private final RecordRepository recordRepository;
    private final DataMappingTransformer mappingTransformer;
    private final IntegrationLogService logService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public String processInboundData(UUID channelId, String rawPayload, String authHeader, String xApiKeyHeader, String apiKeyParam) {
        IntegrationChannel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new IllegalArgumentException("연계 채널을 찾을 수 없습니다. ID: " + channelId));

        if (!channel.isActive()) {
            throw new IllegalArgumentException("해당 연계 채널이 비활성화 상태입니다. Channel: " + channel.getName());
        }

        if (!"INBOUND".equalsIgnoreCase(channel.getDirection())) {
            throw new IllegalArgumentException("Inbound 연계 채널이 아닙니다. Direction: " + channel.getDirection());
        }

        // Inbound Authentication Check
        validateInboundAuthentication(channel, rawPayload, authHeader, xApiKeyHeader, apiKeyParam);

        try {
            String transformedPayload = mappingTransformer.transformPayload(rawPayload, channel.getMappingConfigJson());

            // 채널에 연계 노드가 설정된 경우 Record로 저장
            List<UUID> savedRecordIds = new ArrayList<>();
            if (channel.getNodeId() != null) {
                ClassificationNode node = nodeRepository.findById(channel.getNodeId()).orElse(null);
                if (node != null) {
                    savedRecordIds = saveAsRecords(node, transformedPayload, channel.getName());
                } else {
                    log.warn("[Inbound] Channel [{}]의 nodeId [{}]에 해당하는 노드를 찾을 수 없습니다.", channel.getName(), channel.getNodeId());
                }
            } else {
                log.warn("[Inbound] Channel [{}]에 연계 노드가 설정되어 있지 않아 Record 저장을 건너뜁니다.", channel.getName());
            }

            UUID firstRecordId = savedRecordIds.isEmpty() ? null : savedRecordIds.get(0);
            logService.logSuccess(channelId, firstRecordId, "INBOUND_RECEIVE", rawPayload, transformedPayload);

            log.info("Inbound data processed successfully for channel [{}]: saved {} record(s)", channel.getName(), savedRecordIds.size());
            return transformedPayload;
        } catch (IllegalArgumentException e) {
            // payload 파싱 오류 등 클라이언트 측 문제 → 400으로 전파 (로그만 남기고 re-throw)
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            logService.logError(channelId, null, "INBOUND_RECEIVE", rawPayload, null, e.getMessage(), sw.toString(), 1);
            log.warn("Inbound data rejected for channel [{}]: {}", channelId, e.getMessage());
            throw e; // Controller에서 400 BadRequest로 처리됨
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            logService.logError(channelId, null, "INBOUND_RECEIVE", rawPayload, null, e.getMessage(), sw.toString(), 1);
            log.error("Failed to process inbound data for channel [{}]: {}", channelId, e.getMessage(), e);
            throw new RuntimeException("Inbound 데이터 처리 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 변환된 payload를 Record로 저장한다.
     * payload가 JSON 배열이면 각 요소를 개별 Record로, 단일 객체이면 1건으로 저장한다.
     */
    private List<UUID> saveAsRecords(ClassificationNode node, String transformedPayload, String sourceSystem) {
        List<UUID> savedIds = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(transformedPayload);
            if (root.isArray()) {
                // 배열: 각 요소를 개별 Record로 저장
                for (JsonNode item : root) {
                    Record record = new Record();
                    record.setNode(node);
                    record.setStatus("ACTIVE");
                    record.setSourceSystem(sourceSystem);
                    record.setData(objectMapper.writeValueAsString(item));
                    Record saved = recordRepository.save(record);
                    savedIds.add(saved.getId());
                    log.debug("[Inbound] Record 저장 완료: id={}", saved.getId());
                }
            } else {
                // 단일 객체
                Record record = new Record();
                record.setNode(node);
                record.setStatus("ACTIVE");
                record.setSourceSystem(sourceSystem);
                record.setData(transformedPayload);
                Record saved = recordRepository.save(record);
                savedIds.add(saved.getId());
                log.debug("[Inbound] Record 저장 완료: id={}", saved.getId());
            }
        } catch (Exception e) {
            log.error("[Inbound] Record 저장 중 오류: {}", e.getMessage(), e);
        }
        return savedIds;
    }


    private void validateInboundAuthentication(IntegrationChannel channel, String rawPayload, String authHeader, String xApiKeyHeader, String apiKeyParam) {
        String configJson = channel.getConfigJson();
        if (configJson == null || configJson.isBlank()) {
            return;
        }

        try {
            JsonNode config = objectMapper.readTree(configJson);
            String authType = config.has("authType") ? config.get("authType").asText() : "NONE";
            String secretToken = config.has("secretToken") ? config.get("secretToken").asText() : "";

            if ("NONE".equalsIgnoreCase(authType) || secretToken.isBlank()) {
                return; // 인증 미설정 채널
            }

            boolean isAuthenticated = false;

            if ("BEARER_TOKEN".equalsIgnoreCase(authType)) {
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    String extractedToken = authHeader.substring(7).trim();
                    if (secretToken.equals(extractedToken)) {
                        isAuthenticated = true;
                    }
                }
            } else if ("API_KEY".equalsIgnoreCase(authType)) {
                if (secretToken.equals(xApiKeyHeader) || secretToken.equals(apiKeyParam)) {
                    isAuthenticated = true;
                }
            }

            if (!isAuthenticated) {
                String errorMsg = "Inbound 인증 실패: 유효하지 않은 인증 토큰입니다. (AuthType: " + authType + ")";
                logService.logError(channel.getId(), null, "INBOUND_RECEIVE", rawPayload, null, errorMsg, "SecurityException", 1);
                throw new SecurityException(errorMsg);
            }
        } catch (SecurityException se) {
            throw se;
        } catch (Exception e) {
            log.error("Authentication check exception for channel [{}]: {}", channel.getName(), e.getMessage());
        }
    }
}
