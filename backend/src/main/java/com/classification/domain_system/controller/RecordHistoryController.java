package com.classification.domain_system.controller;

import com.classification.domain_system.entity.RecordHistory;
import com.classification.domain_system.repository.RecordHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/records")
public class RecordHistoryController {

    @Autowired
    private RecordHistoryRepository recordHistoryRepository;

    @GetMapping("/{id}/history")
    @PreAuthorize("hasPermission(null, 'record:read')")
    public ResponseEntity<List<RecordHistory>> getRecordHistory(@PathVariable UUID id) {
        return ResponseEntity.ok(recordHistoryRepository.findByRecordIdOrderByChangedAtDesc(id));
    }
}
