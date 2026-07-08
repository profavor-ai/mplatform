package com.classification.domain_system.controller;

import com.classification.domain_system.entity.ApprovalRequest;
import com.classification.domain_system.entity.ApprovalStep;
import com.classification.domain_system.service.ApprovalService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;
import java.util.Map;

@RestController
@RequestMapping("/api/approval-requests")
@RequiredArgsConstructor
public class ApprovalController {
    
    private final ApprovalService approvalService;
    
    @GetMapping("/effective-workflow/{nodeId}")
    public ResponseEntity<Boolean> hasEffectiveWorkflow(@PathVariable UUID nodeId, @RequestParam String actionType) {
        com.classification.domain_system.entity.WorkflowConfig config = approvalService.resolveWorkflow(nodeId, actionType);
        boolean hasWorkflow = config != null && config.getStepsConfig() != null && !config.getStepsConfig().isEmpty() && !config.getStepsConfig().equals("{\"steps\":[],\"observerIds\":[]}");
        return ResponseEntity.ok(hasWorkflow);
    }
    
    @GetMapping
    public ResponseEntity<List<ApprovalRequest>> getPendingRequests() {
        return ResponseEntity.ok(approvalService.getPendingRequests());
    }

    @GetMapping("/all")
    public ResponseEntity<List<ApprovalRequest>> getAllRequests() {
        return ResponseEntity.ok(approvalService.getAllRequests());
    }
    
    @GetMapping("/todos")
    public ResponseEntity<List<ApprovalStep>> getMyTodos(@RequestParam UUID assigneeId) {
        return ResponseEntity.ok(approvalService.getMyTodos(assigneeId));
    }

    @GetMapping("/my-requests")
    public ResponseEntity<List<ApprovalRequest>> getMyRequests(@RequestParam UUID requesterId) {
        return ResponseEntity.ok(approvalService.getMyRequests(requesterId));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ApprovalRequest> getRequestById(@PathVariable UUID id) {
        return ResponseEntity.ok(approvalService.getRequestById(id));
    }
    
    @PostMapping("/steps/{stepId}/approve")
    public ResponseEntity<ApprovalRequest> approveStep(
            @PathVariable UUID stepId, 
            @RequestParam UUID approverId,
            @RequestBody(required = false) Map<String, String> payload) {
        String comment = payload != null ? payload.get("comment") : null;
        return ResponseEntity.ok(approvalService.approveStep(stepId, approverId, comment));
    }
    
    @PostMapping("/steps/{stepId}/reject")
    public ResponseEntity<ApprovalRequest> rejectStep(
            @PathVariable UUID stepId, 
            @RequestParam UUID approverId,
            @RequestBody(required = false) Map<String, String> payload) {
        String comment = payload != null ? payload.get("comment") : null;
        return ResponseEntity.ok(approvalService.rejectStep(stepId, approverId, comment));
    }
}
