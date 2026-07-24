package com.classification.domain_system.controller;

import com.classification.domain_system.entity.WorkflowConfig;
import com.classification.domain_system.repository.WorkflowConfigRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import java.util.List;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

import com.classification.domain_system.exception.BusinessException;
import com.classification.domain_system.exception.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;

import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/workflow-configs")
@RequiredArgsConstructor
public class WorkflowConfigController {

    private final WorkflowConfigRepository repository;
    private final ObjectMapper mapper = new ObjectMapper();

    private void validateWorkflowConfig(WorkflowConfig config) {
        if (config == null || config.getStepsConfig() == null || config.getStepsConfig().isBlank()) {
            return;
        }
        try {
            JsonNode root = mapper.readTree(config.getStepsConfig());
            if (root.has("steps") && root.get("steps").isArray() && root.get("steps").size() > 0) {
                List<Integer> orders = new ArrayList<>();
                for (JsonNode stepNode : root.get("steps")) {
                    if (stepNode.has("stepOrder")) {
                        orders.add(stepNode.get("stepOrder").asInt());
                    }
                }
                Collections.sort(orders);
                if (orders.isEmpty() || orders.get(0) != 1) {
                    throw new BusinessException(
                            ErrorCode.INVALID_WORKFLOW_CONFIG,
                            "Step orders must start at 1");
                }
                for (int i = 0; i < orders.size(); i++) {
                    if (orders.get(i) != i + 1) {
                        throw new BusinessException(
                                ErrorCode.INVALID_WORKFLOW_CONFIG,
                                "Step orders must be contiguous with no gaps");
                    }
                }
            }
        } catch (BusinessException be) {
            throw be;
        } catch (Exception e) {
            throw new BusinessException(
                    ErrorCode.INVALID_WORKFLOW_CONFIG,
                    "Invalid stepsConfig JSON format");
        }
    }

    @GetMapping("/domain/{domainId}")
    @PreAuthorize("hasPermission(null, 'admin:read') or hasPermission(null, 'workflow:read')")
    public ResponseEntity<List<WorkflowConfig>> getByDomain(@PathVariable UUID domainId) {
        return ResponseEntity.ok(repository.findByDomainId(domainId).stream().filter(c -> c.getNodeId() == null).toList());
    }
    
    @GetMapping("/domain/{domainId}/all")
    @PreAuthorize("hasPermission(null, 'admin:read') or hasPermission(null, 'workflow:read')")
    public ResponseEntity<List<WorkflowConfig>> getAllByDomain(@PathVariable UUID domainId) {
        return ResponseEntity.ok(repository.findByDomainId(domainId).stream().filter(c -> c.getNodeId() == null).toList());
    }

    @GetMapping("/node/{nodeId}")
    @PreAuthorize("hasPermission(null, 'admin:read') or hasPermission(null, 'workflow:read')")
    public ResponseEntity<List<WorkflowConfig>> getByNode(@PathVariable UUID nodeId) {
        return ResponseEntity.ok(repository.findByNodeId(nodeId));
    }

    @PostMapping("/domain/{domainId}")
    @Transactional
    @PreAuthorize("hasPermission(null, 'admin:write') or hasPermission(null, 'workflow:write')")
    public ResponseEntity<List<WorkflowConfig>> saveForDomain(@PathVariable UUID domainId, @RequestBody List<WorkflowConfig> configs) {
        configs.forEach(this::validateWorkflowConfig);
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
    @PreAuthorize("hasPermission(null, 'admin:write') or hasPermission(null, 'workflow:write')")
    public ResponseEntity<List<WorkflowConfig>> saveForNode(@PathVariable UUID nodeId, @RequestBody List<WorkflowConfig> configs) {
        configs.forEach(this::validateWorkflowConfig);
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
