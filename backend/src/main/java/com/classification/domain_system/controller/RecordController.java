package com.classification.domain_system.controller;

import com.classification.domain_system.entity.ApprovalRequest;
import com.classification.domain_system.entity.Record;
import com.classification.domain_system.service.ApprovalService;
import com.classification.domain_system.repository.RecordRepository;
import com.classification.domain_system.dto.RecordRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;
import com.classification.domain_system.entity.ClassificationNode;
import com.classification.domain_system.repository.ClassificationNodeRepository;

@RestController
@RequestMapping("/api/nodes/{nodeId}/records")
@RequiredArgsConstructor
public class RecordController {
    
    private final ApprovalService approvalService;
    private final RecordRepository recordRepository;
    private final ClassificationNodeRepository classificationNodeRepository;
    
    @PostMapping
    public ResponseEntity<ApprovalRequest> createRecordRequest(
            @PathVariable UUID nodeId, 
            @RequestBody RecordRequest request) {
        return ResponseEntity.ok(approvalService.requestRecordCreation(nodeId, request));
    }
    
    @GetMapping
    public ResponseEntity<List<Record>> getRecords(
            @PathVariable UUID nodeId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "false") boolean includeChildren,
            @RequestParam Map<String, String> allParams) {
        
        List<UUID> targetNodeIds = new ArrayList<>();
        targetNodeIds.add(nodeId);
        
        if (includeChildren) {
            List<ClassificationNode> children = classificationNodeRepository.findByParentIdAndIsDeletedFalseOrderByOrderAsc(nodeId);
            targetNodeIds.addAll(children.stream().map(ClassificationNode::getId).collect(Collectors.toList()));
        }

        java.util.Map<String, String> searchParams = new java.util.HashMap<>();
        for (Map.Entry<String, String> entry : allParams.entrySet()) {
            if (entry.getKey().startsWith("search_")) {
                searchParams.put(entry.getKey().substring(7), entry.getValue());
            }
        }

        if (searchParams.isEmpty()) {
            if (status != null && !status.isEmpty()) {
                List<Record> records = recordRepository.findByNodeIdIn(targetNodeIds).stream()
                        .filter(r -> status.equals(r.getStatus()))
                        .toList();
                return ResponseEntity.ok(records);
            }
            
            List<Record> records = recordRepository.findByNodeIdIn(targetNodeIds).stream()
                    .filter(r -> !"REJECTED".equals(r.getStatus()) 
                              && !"MISMATCHED".equals(r.getStatus()))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(records);
        } else {
            List<Record> records = recordRepository.findDynamicRecords(targetNodeIds, status, searchParams);
            return ResponseEntity.ok(records);
        }
    }
}
