package com.classification.domain_system.controller;

import com.classification.domain_system.entity.Record;
import com.classification.domain_system.repository.RecordRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@RestController
@RequestMapping("/api/records")
@RequiredArgsConstructor
public class GlobalRecordController {
    private final RecordRepository recordRepository;
    private final com.classification.domain_system.service.ApprovalService approvalService;
    
    @GetMapping("/{id}")
    public ResponseEntity<Record> getRecord(@PathVariable UUID id) {
        return recordRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/update-request")
    public ResponseEntity<?> updateRecordRequest(
            @PathVariable UUID id, 
            @RequestBody com.classification.domain_system.dto.RecordRequest request) {
        try {
            return ResponseEntity.ok(approvalService.requestRecordUpdate(id, request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{id}/delete-request")
    public ResponseEntity<?> deleteRecordRequest(
            @PathVariable UUID id,
            @RequestBody com.classification.domain_system.dto.RecordRequest request) {
        try {
            return ResponseEntity.ok(approvalService.requestRecordDeletion(id, request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/domain/{domainId}")
    public ResponseEntity<java.util.List<Record>> getRecordsByDomain(
            @PathVariable UUID domainId,
            @RequestParam java.util.Map<String, String> allParams) {
        
        java.util.Map<String, String> searchParams = new java.util.HashMap<>();
        for (java.util.Map.Entry<String, String> entry : allParams.entrySet()) {
            if (entry.getKey().startsWith("search_")) {
                searchParams.put(entry.getKey().substring(7), entry.getValue());
            }
        }

        if (searchParams.isEmpty()) {
            java.util.List<Record> records = recordRepository.findByDomainId(domainId).stream()
                    .filter(r -> !"REJECTED".equals(r.getStatus()) 
                              && !"MISMATCHED".equals(r.getStatus()))
                    .toList();
            return ResponseEntity.ok(records);
        } else {
            java.util.List<Record> records = recordRepository.findDynamicRecordsByDomain(domainId, searchParams);
            return ResponseEntity.ok(records);
        }
    }
}
