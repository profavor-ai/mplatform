package com.classification.domain_system.controller;

import com.classification.domain_system.entity.ClassificationNode;
import com.classification.domain_system.service.ClassificationNodeService;
import com.classification.domain_system.dto.ClassificationNodeRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/domains/{domainId}/nodes")
@RequiredArgsConstructor
public class ClassificationNodeController {
    
    private final ClassificationNodeService nodeService;
    
    @PostMapping
    public ResponseEntity<ClassificationNode> createNode(
            @PathVariable UUID domainId, 
            @RequestBody ClassificationNodeRequest request) {
        return ResponseEntity.ok(nodeService.createNode(domainId, request));
    }
    
    @GetMapping("/tree")
    public ResponseEntity<List<ClassificationNode>> getTree(@PathVariable UUID domainId) {
        return ResponseEntity.ok(nodeService.getTree(domainId));
    }
    
    @PutMapping("/{nodeId}")
    public ResponseEntity<ClassificationNode> updateNode(
            @PathVariable UUID domainId,
            @PathVariable UUID nodeId,
            @RequestBody ClassificationNodeRequest request) {
        return ResponseEntity.ok(nodeService.updateNode(domainId, nodeId, request));
    }
}
