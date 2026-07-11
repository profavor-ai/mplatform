package com.classification.domain_system.controller;

import com.classification.domain_system.entity.Domain;
import com.classification.domain_system.service.DomainService;
import com.classification.domain_system.dto.DomainRequest;
import com.classification.domain_system.dto.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

import com.classification.domain_system.service.FieldDefinitionService;
import com.classification.domain_system.dto.FieldDefinitionRequest;
import com.classification.domain_system.entity.FieldDefinition;
import com.classification.domain_system.service.SectorService;
import com.classification.domain_system.service.FieldGroupService;
import com.classification.domain_system.dto.SectorRequest;
import com.classification.domain_system.dto.FieldGroupRequest;
import com.classification.domain_system.entity.Sector;
import com.classification.domain_system.entity.FieldGroup;

@RestController
@RequestMapping("/api/domains")
@RequiredArgsConstructor
public class DomainController {
    
    private final DomainService domainService;
    private final FieldDefinitionService fieldService;
    private final SectorService sectorService;
    private final FieldGroupService fieldGroupService;
    
    @PostMapping
    public ResponseEntity<Domain> createDomain(@RequestBody DomainRequest request) {
        return ResponseEntity.ok(domainService.createDomain(request));
    }
    
    @GetMapping
    public ResponseEntity<List<Domain>> getAllDomains() {
        return ResponseEntity.ok(domainService.getAllDomains());
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Domain> updateDomain(@PathVariable UUID id, @RequestBody DomainRequest request) {
        return ResponseEntity.ok(domainService.updateDomain(id, request));
    }
    
    
    @GetMapping("/{domainId}/fields/page")
    public ResponseEntity<PageResponse<FieldDefinition>> getDomainFieldsPage(
            @PathVariable UUID domainId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        List<FieldDefinition> allFields = fieldService.getDomainFields(domainId);
        int start = Math.min(page * size, allFields.size());
        int end = Math.min((page + 1) * size, allFields.size());
        List<FieldDefinition> content = allFields.subList(start, end);
        Page<FieldDefinition> p = new PageImpl<>(content, PageRequest.of(page, size), allFields.size());
        return ResponseEntity.ok(PageResponse.of(p));
    }
    
    @GetMapping("/{domainId}/fields")
    public ResponseEntity<List<FieldDefinition>> getDomainFields(@PathVariable UUID domainId) {
        return ResponseEntity.ok(fieldService.getDomainFields(domainId));
    }
    
    @PostMapping("/{domainId}/fields")
    public ResponseEntity<FieldDefinition> addDomainField(
            @PathVariable UUID domainId,
            @RequestBody FieldDefinitionRequest request) {
        return ResponseEntity.ok(fieldService.addDomainField(domainId, request));
    }
    
    @PutMapping("/{domainId}/fields/{fieldId}")
    public ResponseEntity<FieldDefinition> updateDomainField(
            @PathVariable UUID domainId,
            @PathVariable UUID fieldId,
            @RequestBody FieldDefinitionRequest request) {
        return ResponseEntity.ok(fieldService.updateDomainField(domainId, fieldId, request));
    }

    // Sectors
    @GetMapping("/{domainId}/sectors")
    public ResponseEntity<List<Sector>> getSectors(@PathVariable UUID domainId) {
        return ResponseEntity.ok(sectorService.getSectorsByDomain(domainId));
    }

    @PostMapping("/{domainId}/sectors")
    public ResponseEntity<Sector> createSector(
            @PathVariable UUID domainId,
            @RequestBody SectorRequest request) {
        return ResponseEntity.ok(sectorService.createSector(domainId, request));
    }

    @PutMapping("/{domainId}/sectors/{sectorId}")
    public ResponseEntity<Sector> updateSector(
            @PathVariable UUID domainId, // for path consistency
            @PathVariable UUID sectorId,
            @RequestBody SectorRequest request) {
        return ResponseEntity.ok(sectorService.updateSector(sectorId, request));
    }

    @DeleteMapping("/{domainId}/sectors/{sectorId}")
    public ResponseEntity<Void> deleteSector(
            @PathVariable UUID domainId,
            @PathVariable UUID sectorId) {
        sectorService.deleteSector(sectorId);
        return ResponseEntity.ok().build();
    }

    // FieldGroups
    @GetMapping("/{domainId}/groups")
    public ResponseEntity<List<FieldGroup>> getGroups(@PathVariable UUID domainId) {
        return ResponseEntity.ok(fieldGroupService.getGroupsByDomain(domainId));
    }

    @PostMapping("/{domainId}/groups")
    public ResponseEntity<FieldGroup> createGroup(
            @PathVariable UUID domainId,
            @RequestBody FieldGroupRequest request) {
        return ResponseEntity.ok(fieldGroupService.createGroup(domainId, request));
    }

    @PutMapping("/{domainId}/groups/{groupId}")
    public ResponseEntity<FieldGroup> updateGroup(
            @PathVariable UUID domainId,
            @PathVariable UUID groupId,
            @RequestBody FieldGroupRequest request) {
        return ResponseEntity.ok(fieldGroupService.updateGroup(groupId, request));
    }

    @DeleteMapping("/{domainId}/groups/{groupId}")
    public ResponseEntity<Void> deleteGroup(
            @PathVariable UUID domainId,
            @PathVariable UUID groupId) {
        fieldGroupService.deleteGroup(groupId);
        return ResponseEntity.ok().build();
    }
}
