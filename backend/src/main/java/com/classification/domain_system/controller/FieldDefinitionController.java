package com.classification.domain_system.controller;

import com.classification.domain_system.entity.FieldDefinition;
import com.classification.domain_system.dto.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import java.util.List;
import com.classification.domain_system.service.FieldDefinitionService;
import com.classification.domain_system.dto.FieldDefinitionRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/nodes/{nodeId}/fields")
@RequiredArgsConstructor
public class FieldDefinitionController {
    
    private final FieldDefinitionService fieldService;
    
    @PostMapping
    public ResponseEntity<FieldDefinition> addField(
            @PathVariable UUID nodeId, 
            @RequestBody FieldDefinitionRequest request) {
        return ResponseEntity.ok(fieldService.addField(nodeId, request));
    }
    
    
    @GetMapping("/effective/page")
    public ResponseEntity<PageResponse<FieldDefinition>> getEffectiveFieldsPage(
            @PathVariable UUID nodeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        List<FieldDefinition> allFields = fieldService.getEffectiveFields(nodeId);
        int start = Math.min(page * size, allFields.size());
        int end = Math.min((page + 1) * size, allFields.size());
        List<FieldDefinition> content = allFields.subList(start, end);
        Page<FieldDefinition> p = new PageImpl<>(content, PageRequest.of(page, size), allFields.size());
        return ResponseEntity.ok(PageResponse.of(p));
    }
    
    @GetMapping("/effective")
    public ResponseEntity<List<FieldDefinition>> getEffectiveFields(@PathVariable UUID nodeId) {
        return ResponseEntity.ok(fieldService.getEffectiveFields(nodeId));
    }
    
    @PutMapping("/{fieldId}")
    public ResponseEntity<FieldDefinition> updateField(
            @PathVariable UUID nodeId,
            @PathVariable UUID fieldId,
            @RequestBody FieldDefinitionRequest request) {
        return ResponseEntity.ok(fieldService.updateField(nodeId, fieldId, request));
    }
}
