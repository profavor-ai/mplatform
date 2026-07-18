package com.classification.domain_system.controller;

import com.classification.domain_system.entity.ErrorLog;
import com.classification.domain_system.service.ErrorLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/error-logs")
@RequiredArgsConstructor
public class ErrorLogController {

    private final ErrorLogService errorLogService;

    @GetMapping
    public ResponseEntity<?> getErrorLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {

        if (authentication == null || !authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ADMIN"))) {
            return ResponseEntity.status(403).body("Access denied. Admin role required.");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "loggedAt"));
        Page<ErrorLog> logs = errorLogService.getErrorLogs(pageable);
        return ResponseEntity.ok(logs);
    }
}
