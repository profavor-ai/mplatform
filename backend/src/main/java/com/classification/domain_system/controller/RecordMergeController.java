package com.classification.domain_system.controller;

import com.classification.domain_system.entity.Record;
import com.classification.domain_system.service.RecordMergeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/records")
@RequiredArgsConstructor
public class RecordMergeController {

    private final RecordMergeService recordMergeService;

    @PostMapping("/merge")
    @PreAuthorize("hasPermission(null, 'record:write')")
    public ResponseEntity<Record> mergeRecords(
            @RequestBody RecordMergeService.MergeRequest request,
            @AuthenticationPrincipal String username) {
        Record survivor = recordMergeService.mergeRecords(request, username != null ? username : "SYSTEM");
        return ResponseEntity.ok(survivor);
    }
}
