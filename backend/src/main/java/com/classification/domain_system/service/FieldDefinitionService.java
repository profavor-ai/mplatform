package com.classification.domain_system.service;

import com.classification.domain_system.entity.FieldDefinition;
import com.classification.domain_system.entity.ClassificationNode;
import com.classification.domain_system.repository.FieldDefinitionRepository;
import com.classification.domain_system.repository.ClassificationNodeRepository;
import com.classification.domain_system.repository.DomainRepository;
import com.classification.domain_system.dto.FieldDefinitionRequest;
import com.classification.domain_system.entity.Domain;
import com.classification.domain_system.repository.FieldGroupRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FieldDefinitionService {
    
    private final FieldDefinitionRepository fieldRepository;
    private final ClassificationNodeRepository nodeRepository;
    private final DomainRepository domainRepository;
    private final FieldGroupRepository fieldGroupRepository;
    private final JdbcTemplate jdbcTemplate;
    
    private void manageIndex(String fieldKey, boolean isSearchable) {
        if (fieldKey == null || fieldKey.trim().isEmpty()) return;
        String safeKey = fieldKey.replaceAll("[^a-zA-Z0-9_]", "_");
        String indexName = "idx_record_search_" + safeKey;
        if (isSearchable) {
            String sql = "CREATE INDEX IF NOT EXISTS " + indexName + " ON record ((data->>'" + safeKey + "'))";
            try {
                jdbcTemplate.execute(sql);
            } catch (Exception e) {
                // Ignore index already exists or syntax issues depending on DB dialect
                System.err.println("Failed to create index: " + e.getMessage());
            }
        } else {
            String sql = "DROP INDEX IF EXISTS " + indexName;
            try {
                jdbcTemplate.execute(sql);
            } catch (Exception e) {
                System.err.println("Failed to drop index: " + e.getMessage());
            }
        }
    }
    
    @Transactional
    public FieldDefinition addField(UUID nodeId, FieldDefinitionRequest request) {
        ClassificationNode node = nodeRepository.findById(nodeId)
                .orElseThrow(() -> new RuntimeException("Node not found"));
                
        FieldDefinition field = new FieldDefinition();
        field.setDefinedAtNode(node);
        field.setName(request.getName());
        
        if (request.getFieldGroupId() != null) {
            field.setFieldGroup(fieldGroupRepository.findById(request.getFieldGroupId())
                .orElseThrow(() -> new RuntimeException("FieldGroup not found")));
        } else {
            field.setFieldGroup(null);
        }
        field.setKey(request.getKey());
        field.setType(request.getType());
        field.setOptions(normalizeJsonStr(request.getOptions()));
        field.setRequired(request.getRequired() != null ? request.getRequired() : false);
        field.setDefaultValue(normalizeJsonStr(request.getDefaultValue()));
        field.setOrder(request.getOrder() != null ? request.getOrder() : 0);
        field.setIsRemoved(false);
        field.setIsMultiValue(request.getIsMultiValue() != null ? request.getIsMultiValue() : false);
        field.setIsTable(request.getIsTable() != null ? request.getIsTable() : false);
        field.setIsEncrypted(request.getIsEncrypted() != null ? request.getIsEncrypted() : false);
        field.setIsSearchable(request.getIsSearchable() != null ? request.getIsSearchable() : false);
        field.setIsReadOnly(request.getIsReadOnly() != null ? request.getIsReadOnly() : false);
        field.setIsImmutable(request.getIsImmutable() != null ? request.getIsImmutable() : false);
        field.setIsHidden(request.getIsHidden() != null ? request.getIsHidden() : false);
        
        FieldDefinition savedField = fieldRepository.save(field);
        if (Boolean.TRUE.equals(request.getIsSearchable())) {
            manageIndex(savedField.getKey(), true);
        }
        return savedField;
    }
    
    @Transactional(readOnly = true)
    public List<FieldDefinition> getEffectiveFields(UUID nodeId) {
        ClassificationNode node = nodeRepository.findById(nodeId)
                .orElseThrow(() -> new RuntimeException("Node not found"));
        
        List<FieldDefinition> domainFields = fieldRepository.findByDomainIdOrderByOrderAsc(node.getDomain().getId());
        List<FieldDefinition> nodeFields = fieldRepository.findByDefinedAtNodeIdOrderByOrderAsc(nodeId);
        
        java.util.List<FieldDefinition> effectiveFields = new java.util.ArrayList<>();
        effectiveFields.addAll(domainFields);
        effectiveFields.addAll(nodeFields);
        return effectiveFields;
    }
    
    @Transactional(readOnly = true)
    public List<FieldDefinition> getDomainFields(UUID domainId) {
        return fieldRepository.findByDomainIdOrderByOrderAsc(domainId);
    }
    
    private String normalizeJsonStr(String val) {
        if (val == null || val.trim().isEmpty()) {
            return null;
        }
        val = val.trim();
        // If it looks like JSON (starts with { or [), keep it
        if (val.startsWith("{") || val.startsWith("[")) {
            return val;
        }
        // Otherwise, assume it's a comma separated list and convert to JSON array
        String[] parts = val.split("\\s*,\\s*");
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < parts.length; i++) {
            sb.append("\"").append(parts[i].replace("\"", "\\\"")).append("\"");
            if (i < parts.length - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }
    
    @Transactional
    public FieldDefinition addDomainField(UUID domainId, FieldDefinitionRequest request) {
        Domain domain = domainRepository.findById(domainId)
                .orElseThrow(() -> new RuntimeException("Domain not found"));
                
        FieldDefinition field = new FieldDefinition();
        field.setDomain(domain);
        field.setName(request.getName());
        
        if (request.getFieldGroupId() != null) {
            field.setFieldGroup(fieldGroupRepository.findById(request.getFieldGroupId())
                .orElseThrow(() -> new RuntimeException("FieldGroup not found")));
        } else {
            field.setFieldGroup(null);
        }
        field.setKey(request.getKey());
        field.setType(request.getType());
        field.setOptions(normalizeJsonStr(request.getOptions()));
        field.setRequired(request.getRequired() != null ? request.getRequired() : false);
        field.setDefaultValue(normalizeJsonStr(request.getDefaultValue()));
        field.setOrder(request.getOrder() != null ? request.getOrder() : 0);
        field.setIsRemoved(false);
        field.setIsMultiValue(request.getIsMultiValue() != null ? request.getIsMultiValue() : false);
        field.setIsTable(request.getIsTable() != null ? request.getIsTable() : false);
        field.setIsEncrypted(request.getIsEncrypted() != null ? request.getIsEncrypted() : false);
        field.setIsSearchable(request.getIsSearchable() != null ? request.getIsSearchable() : false);
        field.setIsReadOnly(request.getIsReadOnly() != null ? request.getIsReadOnly() : false);
        field.setIsImmutable(request.getIsImmutable() != null ? request.getIsImmutable() : false);
        field.setIsHidden(request.getIsHidden() != null ? request.getIsHidden() : false);
        
        FieldDefinition savedField = fieldRepository.save(field);
        if (Boolean.TRUE.equals(request.getIsSearchable())) {
            manageIndex(savedField.getKey(), true);
        }
        return savedField;
    }
    
    @Transactional
    public FieldDefinition updateDomainField(UUID domainId, UUID fieldId, FieldDefinitionRequest request) {
        FieldDefinition field = fieldRepository.findById(fieldId)
                .orElseThrow(() -> new RuntimeException("Field not found"));
                
        if (field.getDomain() == null || !field.getDomain().getId().equals(domainId)) {
            throw new RuntimeException("Field does not belong to the specified domain");
        }
        
        field.setName(request.getName());
        
        if (request.getFieldGroupId() != null) {
            field.setFieldGroup(fieldGroupRepository.findById(request.getFieldGroupId())
                .orElseThrow(() -> new RuntimeException("FieldGroup not found")));
        } else {
            field.setFieldGroup(null);
        }
        field.setKey(request.getKey());
        field.setType(request.getType());
        field.setUnit(request.getUnit());
        field.setOptions(normalizeJsonStr(request.getOptions()));
        field.setRequired(request.getRequired() != null ? request.getRequired() : field.getRequired());
        field.setDefaultValue(normalizeJsonStr(request.getDefaultValue()));
        field.setOrder(request.getOrder() != null ? request.getOrder() : field.getOrder());
        field.setIsMultiValue(request.getIsMultiValue() != null ? request.getIsMultiValue() : field.getIsMultiValue());
        field.setIsTable(request.getIsTable() != null ? request.getIsTable() : field.getIsTable());
        Boolean wasSearchable = field.getIsSearchable();
        Boolean willBeSearchable = request.getIsSearchable() != null ? request.getIsSearchable() : field.getIsSearchable();
        
        field.setIsEncrypted(request.getIsEncrypted() != null ? request.getIsEncrypted() : field.getIsEncrypted());
        field.setIsSearchable(willBeSearchable);
        field.setIsReadOnly(request.getIsReadOnly() != null ? request.getIsReadOnly() : field.getIsReadOnly());
        field.setIsImmutable(request.getIsImmutable() != null ? request.getIsImmutable() : field.getIsImmutable());
        field.setIsHidden(request.getIsHidden() != null ? request.getIsHidden() : field.getIsHidden());
        
        FieldDefinition savedField = fieldRepository.save(field);
        
        if (Boolean.TRUE.equals(willBeSearchable) && !Boolean.TRUE.equals(wasSearchable)) {
            manageIndex(savedField.getKey(), true);
        } else if (!Boolean.TRUE.equals(willBeSearchable) && Boolean.TRUE.equals(wasSearchable)) {
            manageIndex(savedField.getKey(), false);
        }
        
        return savedField;
    }
    
    @Transactional
    public FieldDefinition updateField(UUID nodeId, UUID fieldId, FieldDefinitionRequest request) {
        FieldDefinition field = fieldRepository.findById(fieldId)
                .orElseThrow(() -> new RuntimeException("Field not found"));
                
        if (field.getDefinedAtNode() == null || !field.getDefinedAtNode().getId().equals(nodeId)) {
            throw new RuntimeException("Field does not belong to the specified node");
        }
        
        field.setName(request.getName());
        
        if (request.getFieldGroupId() != null) {
            field.setFieldGroup(fieldGroupRepository.findById(request.getFieldGroupId())
                .orElseThrow(() -> new RuntimeException("FieldGroup not found")));
        } else {
            field.setFieldGroup(null);
        }
        field.setKey(request.getKey());
        field.setType(request.getType());
        field.setUnit(request.getUnit() != null ? request.getUnit() : field.getUnit());
        field.setOptions(normalizeJsonStr(request.getOptions()));
        field.setRequired(request.getRequired() != null ? request.getRequired() : field.getRequired());
        field.setDefaultValue(normalizeJsonStr(request.getDefaultValue()));
        field.setOrder(request.getOrder() != null ? request.getOrder() : field.getOrder());
        field.setIsMultiValue(request.getIsMultiValue() != null ? request.getIsMultiValue() : field.getIsMultiValue());
        field.setIsTable(request.getIsTable() != null ? request.getIsTable() : field.getIsTable());
        
        Boolean wasSearchable = field.getIsSearchable();
        Boolean willBeSearchable = request.getIsSearchable() != null ? request.getIsSearchable() : field.getIsSearchable();
        
        field.setIsEncrypted(request.getIsEncrypted() != null ? request.getIsEncrypted() : field.getIsEncrypted());
        field.setIsSearchable(willBeSearchable);
        field.setIsReadOnly(request.getIsReadOnly() != null ? request.getIsReadOnly() : field.getIsReadOnly());
        field.setIsImmutable(request.getIsImmutable() != null ? request.getIsImmutable() : field.getIsImmutable());
        field.setIsHidden(request.getIsHidden() != null ? request.getIsHidden() : field.getIsHidden());
        
        FieldDefinition savedField = fieldRepository.save(field);
        
        if (Boolean.TRUE.equals(willBeSearchable) && !Boolean.TRUE.equals(wasSearchable)) {
            manageIndex(savedField.getKey(), true);
        } else if (!Boolean.TRUE.equals(willBeSearchable) && Boolean.TRUE.equals(wasSearchable)) {
            manageIndex(savedField.getKey(), false);
        }
        
        return savedField;
    }
}
