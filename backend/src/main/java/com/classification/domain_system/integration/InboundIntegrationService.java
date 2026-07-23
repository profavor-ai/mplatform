package com.classification.domain_system.integration;

import com.classification.domain_system.entity.ClassificationNode;
import com.classification.domain_system.entity.IntegrationChannel;
import com.classification.domain_system.entity.Record;
import com.classification.domain_system.entity.RecordHistory;
import com.classification.domain_system.repository.ClassificationNodeRepository;
import com.classification.domain_system.repository.IntegrationChannelRepository;
import com.classification.domain_system.repository.RecordHistoryRepository;
import com.classification.domain_system.repository.RecordRepository;
import com.classification.domain_system.service.MatchingService;
import com.classification.domain_system.service.dq.DqRuleEngine;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
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
    private final RecordHistoryRepository recordHistoryRepository;
    private final MatchingService matchingService;
    private final DqRuleEngine dqRuleEngine;
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

            // 채널에 연계 노드(또는 도메인)가 설정된 경우 Record로 저장
            List<UUID> savedRecordIds = new ArrayList<>();
            ClassificationNode targetNode = null;
            if (channel.getNodeId() != null) {
                targetNode = nodeRepository.findById(channel.getNodeId()).orElse(null);
            }
            if (targetNode == null) {
                UUID domainId = extractDomainIdFromConfig(channel.getConfigJson());
                if (domainId != null) {
                    List<ClassificationNode> rootNodes = nodeRepository.findByDomain_IdAndParentIsNullAndIsDeletedFalseOrderByOrderAsc(domainId);
                    if (!rootNodes.isEmpty()) {
                        targetNode = rootNodes.get(0);
                        log.info("[Inbound] Channel [{}]의 nodeId가 없으므로 도메인[{}]의 Root Node[{}]로 자동 지정합니다.", channel.getName(), domainId, targetNode.getId());
                    }
                }
            }

            if (targetNode != null) {
                savedRecordIds = saveAsRecords(targetNode, transformedPayload, channel.getName());
            } else {
                throw new IllegalArgumentException("Inbound 연계 채널에 데이터 수신 대상 도메인 및 분류 노드가 설정되어 있지 않습니다. 연계 채널 설정을 확인해 주세요.");
            }

            UUID firstRecordId = savedRecordIds.isEmpty() ? null : savedRecordIds.get(0);
            logService.logSuccess(channelId, firstRecordId, "INBOUND_RECEIVE", rawPayload, transformedPayload);

            log.info("Inbound data processed successfully for channel [{}]: saved {} record(s)", channel.getName(), savedRecordIds.size());
            return transformedPayload;
        } catch (IllegalArgumentException e) {
            // payload 파싱 오류 등 클라이언트 측 문제 → 400으로 전파 (로그만 남기고 re-throw)
            String stackTrace = getStackTraceAsString(e);
            logService.logError(channelId, null, "INBOUND_RECEIVE", rawPayload, null, e.getMessage(), stackTrace, 1);
            log.warn("Inbound data rejected for channel [{}]: {}", channelId, e.getMessage());
            throw e; // Controller에서 400 BadRequest로 처리됨
        } catch (Exception e) {
            String stackTrace = getStackTraceAsString(e);
            logService.logError(channelId, null, "INBOUND_RECEIVE", rawPayload, null, e.getMessage(), stackTrace, 1);
            log.error("Failed to process inbound data for channel [{}]: {}", channelId, e.getMessage(), e);
            throw new RuntimeException("Inbound 데이터 처리 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    private String getStackTraceAsString(Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : throwable.getStackTrace()) {
            sb.append(element.toString()).append("\n");
        }
        return sb.toString();
    }

    /**
     * 변환된 payload를 Record로 저장하고 RecordHistory를 함께 생성한다.
     * 중복 검사(MatchingService)를 거쳐 중복 데이터는 UPDATE, 신규 데이터는 INSERT하며, DQE 규칙 평가를 수행한다.
     */
    private List<UUID> saveAsRecords(ClassificationNode node, String transformedPayload, String sourceSystem) {
        List<UUID> savedIds = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(transformedPayload);
            if (root.isArray()) {
                // 배열: 각 요소를 개별 Record로 Upsert 처리
                for (JsonNode item : root) {
                    String itemJson = objectMapper.writeValueAsString(item);
                    UUID id = saveOrUpdateSingleRecord(node, itemJson, sourceSystem);
                    if (id != null) savedIds.add(id);
                }
            } else {
                // 단일 객체 Upsert 처리
                UUID id = saveOrUpdateSingleRecord(node, transformedPayload, sourceSystem);
                if (id != null) savedIds.add(id);
            }
        } catch (Exception e) {
            log.error("[Inbound] Record/RecordHistory 저장 중 오류: {}", e.getMessage(), e);
        }
        return savedIds;
    }

    private UUID saveOrUpdateSingleRecord(ClassificationNode node, String itemJson, String sourceSystem) {
        MatchingService.DuplicateResult dupCheck = matchingService.checkDuplicates(node.getId(), itemJson);

        if (dupCheck.hasDuplicates && dupCheck.duplicateRecordIds != null && !dupCheck.duplicateRecordIds.isEmpty()) {
            // UPDATE: 기존 레코드 갱신
            UUID existingId = dupCheck.duplicateRecordIds.get(0);
            Record existingRecord = recordRepository.findById(existingId).orElse(null);
            if (existingRecord != null) {
                String prevData = existingRecord.getData();
                existingRecord.setData(itemJson);
                existingRecord.setSourceSystem(sourceSystem);
                int nextVer = (existingRecord.getVersion() != null ? existingRecord.getVersion() : 1) + 1;
                existingRecord.setVersion(nextVer);
                existingRecord.setUpdatedAt(LocalDateTime.now());
                Record saved = recordRepository.save(existingRecord);

                RecordHistory history = new RecordHistory();
                history.setRecordId(saved.getId());
                history.setChangeType("UPDATE");
                history.setChangedBy(null);
                history.setPreviousData(prevData);
                history.setNewData(itemJson);
                history.setApprovalRequestId(null);
                history.setVersion(nextVer);
                history.setSourceSystem(sourceSystem);
                recordHistoryRepository.save(history);

                // DQE 평가 수행
                try {
                    dqRuleEngine.evaluate(node.getId(), itemJson, saved.getId());
                } catch (Exception e) {
                    log.error("[Inbound DQE] Error evaluating record [{}]: {}", saved.getId(), e.getMessage());
                }

                log.info("[Inbound] 기존 레코드 발견으로 UPDATE 처리 완료: id={}, ver={}", saved.getId(), nextVer);
                return saved.getId();
            }
        }

        // INSERT: 신규 레코드 생성
        Record record = new Record();
        record.setNode(node);
        record.setStatus("ACTIVE");
        record.setSourceSystem(sourceSystem);
        record.setData(itemJson);
        Record saved = recordRepository.save(record);

        RecordHistory history = new RecordHistory();
        history.setRecordId(saved.getId());
        history.setChangeType("CREATE");
        history.setChangedBy(null);
        history.setPreviousData(null);
        history.setNewData(itemJson);
        history.setApprovalRequestId(null);
        history.setVersion(1);
        history.setSourceSystem(sourceSystem);
        recordHistoryRepository.save(history);

        // DQE 평가 수행
        try {
            dqRuleEngine.evaluate(node.getId(), itemJson, saved.getId());
        } catch (Exception e) {
            log.error("[Inbound DQE] Error evaluating record [{}]: {}", saved.getId(), e.getMessage());
        }

        log.info("[Inbound] 신규 레코드 INSERT 처리 완료: id={}", saved.getId());
        return saved.getId();
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

    private UUID extractDomainIdFromConfig(String configJson) {
        if (configJson == null || configJson.isBlank()) return null;
        try {
            JsonNode config = objectMapper.readTree(configJson);
            if (config.has("domainId") && !config.get("domainId").isNull() && !config.get("domainId").asText().isBlank()) {
                return UUID.fromString(config.get("domainId").asText());
            }
        } catch (Exception e) {
            log.error("[Inbound] configJson domainId 추출 실패: {}", e.getMessage());
        }
        return null;
    }
}
