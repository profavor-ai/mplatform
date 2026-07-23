package com.classification.domain_system.controller;

import com.classification.domain_system.dto.PageResponse;
import com.classification.domain_system.entity.SchemaHistory;
import com.classification.domain_system.repository.SchemaHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class SchemaHistoryController {

    private final SchemaHistoryRepository historyRepository;

    @GetMapping("/api/domains/{domainId}/schema-history")
    @PreAuthorize("hasAnyAuthority('domain:read', 'domain:*', 'ROLE_ADMIN')")
    public ResponseEntity<PageResponse<SchemaHistory>> getSchemaHistory(
            @PathVariable UUID domainId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(PageResponse.of(historyRepository.findByDomainIdOrderByChangedAtDesc(domainId, PageRequest.of(page, size))));
    }
}
