package com.classification.domain_system.integration;

import com.classification.domain_system.entity.IntegrationLog;
import com.classification.domain_system.repository.IntegrationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IntegrationLogService {

    private final IntegrationLogRepository logRepository;
    private final com.classification.domain_system.repository.IntegrationChannelRepository channelRepository;
    private final org.springframework.context.ApplicationContext applicationContext;

    @Transactional
    public void logSuccess(UUID channelId, UUID recordId, String eventType, String originalPayload, String mappedPayload) {
        IntegrationLog log = new IntegrationLog();
        log.setChannelId(channelId);
        log.setRecordId(recordId);
        log.setEventType(eventType);
        log.setOriginalPayload(originalPayload);
        log.setMappedPayload(mappedPayload);
        log.setStatus("SUCCESS");
        logRepository.save(log);
    }

    @Transactional
    public void logError(UUID channelId, UUID recordId, String eventType, String originalPayload, String mappedPayload, String errorMessage, String stackTrace, int retryCount) {
        IntegrationLog log = new IntegrationLog();
        log.setChannelId(channelId);
        log.setRecordId(recordId);
        log.setEventType(eventType);
        log.setOriginalPayload(originalPayload);
        log.setMappedPayload(mappedPayload);
        log.setStatus("FAIL");
        // truncate error message if too long
        if (errorMessage != null && errorMessage.length() > 5000) {
            errorMessage = errorMessage.substring(0, 5000);
        }
        log.setErrorMessage(errorMessage);
        log.setStackTrace(stackTrace);
        log.setRetryCount(retryCount);
        logRepository.save(log);
    }

    @Transactional
    public void retryLog(UUID logId) {
        IntegrationLog log = logRepository.findById(logId)
                .orElseThrow(() -> new IllegalArgumentException("Log not found"));
        
        com.classification.domain_system.entity.IntegrationChannel channel = channelRepository.findById(log.getChannelId())
                .orElseThrow(() -> new IllegalArgumentException("Channel not found"));

        org.springframework.messaging.Message<String> retryMessage = org.springframework.messaging.support.MessageBuilder
                .withPayload(log.getOriginalPayload())
                .setHeader("RECORD_ID", log.getRecordId())
                .setHeader("EVENT_TYPE", log.getEventType())
                .setHeader("CHANNEL_ID", channel.getId())
                .setHeader("CHANNEL_TYPE", channel.getType())
                .setHeader("MAPPING_CONFIG", channel.getMappingConfigJson())
                .setHeader("CHANNEL_CONFIG", channel.getConfigJson())
                .setHeader("ORIGINAL_PAYLOAD", log.getOriginalPayload())
                .setHeader("IS_RETRY", true)
                .build();

        org.springframework.messaging.MessageChannel channelBean = applicationContext.getBean("integrationProcessingChannel", org.springframework.messaging.MessageChannel.class);
        channelBean.send(retryMessage);
    }
}
