package com.classification.domain_system.controller;

import com.classification.domain_system.entity.FieldDefinition;
import com.classification.domain_system.dto.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import java.util.List;
import com.classification.domain_system.service.FieldDefinitionService;
import com.classification.domain_system.dto.FieldDefinitionRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@RestController
@RequestMapping("/api/nodes/{nodeId}/fields")
@RequiredArgsConstructor
public class FieldDefinitionController {
    
    private final FieldDefinitionService fieldService;
    
    @PostMapping
    @PreAuthorize("hasAnyAuthority('field:write', 'field:*', 'ROLE_ADMIN')")
    public ResponseEntity<FieldDefinition> addField(
            @PathVariable UUID nodeId, 
            @RequestBody FieldDefinitionRequest request) {
        return ResponseEntity.ok(fieldService.addField(nodeId, request));
    }
    
    @GetMapping("/effective/page")
    @PreAuthorize("hasAnyAuthority('field:read', 'field:*', 'ROLE_ADMIN')")
    public ResponseEntity<PageResponse<FieldDefinition>> getEffectiveFieldsPage(
            @PathVariable UUID nodeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        Page<FieldDefinition> p = fieldService.getEffectiveFieldsPage(nodeId, PageRequest.of(page, size));
        return ResponseEntity.ok(PageResponse.of(p));
    }
    
    @GetMapping("/effective")
    @PreAuthorize("hasAnyAuthority('field:read', 'field:*', 'ROLE_ADMIN')")
    public ResponseEntity<List<FieldDefinition>> getEffectiveFields(@PathVariable UUID nodeId) {
        return ResponseEntity.ok(fieldService.getEffectiveFields(nodeId));
    }
    
    @PutMapping("/{fieldId}")
    @PreAuthorize("hasAnyAuthority('field:write', 'field:*', 'ROLE_ADMIN')")
    public ResponseEntity<FieldDefinition> updateField(
            @PathVariable UUID nodeId,
            @PathVariable UUID fieldId,
            @RequestBody FieldDefinitionRequest request) {
        return ResponseEntity.ok(fieldService.updateField(nodeId, fieldId, request));
    }
}
