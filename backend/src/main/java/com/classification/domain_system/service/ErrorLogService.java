package com.classification.domain_system.service;

import com.classification.domain_system.entity.ErrorLog;
import com.classification.domain_system.repository.ErrorLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ErrorLogService {

    private final ErrorLogRepository errorLogRepository;

    @Transactional
    public void logError(String userId, String requestUri, String errorMessage, String stackTrace) {
        ErrorLog log = ErrorLog.builder()
                .userId(userId)
                .requestUri(requestUri)
                .errorMessage(errorMessage != null && errorMessage.length() > 1000 ? errorMessage.substring(0, 999) : errorMessage)
                .stackTrace(stackTrace)
                .build();
        errorLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public Page<ErrorLog> getErrorLogs(Pageable pageable) {
        return errorLogRepository.findAll(pageable);
    }
}
