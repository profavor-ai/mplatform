package com.classification.domain_system.controller;

import com.classification.domain_system.entity.WorkflowConfig;
import com.classification.domain_system.repository.WorkflowConfigRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import java.util.List;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/workflow-configs")
@RequiredArgsConstructor
public class WorkflowConfigController {

    private final WorkflowConfigRepository repository;

    @GetMapping("/domain/{domainId}")
    public ResponseEntity<List<WorkflowConfig>> getByDomain(@PathVariable UUID domainId) {
        return ResponseEntity.ok(repository.findByDomainId(domainId).stream().filter(c -> c.getNodeId() == null).toList());
    }
    
    @GetMapping("/domain/{domainId}/all")
    public ResponseEntity<List<WorkflowConfig>> getAllByDomain(@PathVariable UUID domainId) {
        return ResponseEntity.ok(repository.findByDomainId(domainId).stream().filter(c -> c.getNodeId() == null).toList());
    }

    @GetMapping("/node/{nodeId}")
    public ResponseEntity<List<WorkflowConfig>> getByNode(@PathVariable UUID nodeId) {
        return ResponseEntity.ok(repository.findByNodeId(nodeId));
    }

    @PostMapping("/domain/{domainId}")
    @Transactional
    public ResponseEntity<List<WorkflowConfig>> saveForDomain(@PathVariable UUID domainId, @RequestBody List<WorkflowConfig> configs) {
        List<WorkflowConfig> existing = repository.findByDomainId(domainId).stream().filter(c -> c.getNodeId() == null).toList();
        repository.deleteAll(existing);
        
        configs.forEach(c -> {
            c.setDomainId(domainId);
            c.setNodeId(null);
            c.setId(null); // Force new
        });
        return ResponseEntity.ok(repository.saveAll(configs));
    }

    @PostMapping("/node/{nodeId}")
    @Transactional
    public ResponseEntity<List<WorkflowConfig>> saveForNode(@PathVariable UUID nodeId, @RequestBody List<WorkflowConfig> configs) {
        List<WorkflowConfig> existing = repository.findByNodeId(nodeId);
        repository.deleteAll(existing);
        
        configs.forEach(c -> {
            c.setNodeId(nodeId);
            // domainId should also be set if we know it, but nodeId is enough for the repository
            c.setId(null); 
        });
        return ResponseEntity.ok(repository.saveAll(configs));
    }
}
