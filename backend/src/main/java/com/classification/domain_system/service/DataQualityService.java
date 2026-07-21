package com.classification.domain_system.service;

import com.classification.domain_system.entity.FieldDefinition;
import com.classification.domain_system.entity.Domain;
import com.classification.domain_system.entity.ClassificationNode;
import com.classification.domain_system.repository.ClassificationNodeRepository;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

@Service
public class DataQualityService {

    private final FieldDefinitionService fieldDefinitionService;
    private final ClassificationNodeRepository nodeRepository;
    private final ObjectMapper objectMapper;

    public DataQualityService(FieldDefinitionService fieldDefinitionService, ClassificationNodeRepository nodeRepository) {
        this.fieldDefinitionService = fieldDefinitionService;
        this.nodeRepository = nodeRepository;
        this.objectMapper = new ObjectMapper();
    }

    public static class DQResult {
        public boolean isValid;
        public List<String> errors = new ArrayList<>();
    }

    public DQResult validateData(UUID nodeId, String jsonString) {
        DQResult result = new DQResult();
        List<FieldDefinition> effectiveFields = fieldDefinitionService.getEffectiveFields(nodeId);

        ClassificationNode node = nodeRepository.findById(nodeId).orElse(null);
        Domain dom = node != null ? node.getDomain() : null;

        JsonNode dataNode = null;
        try {
            dataNode = objectMapper.readTree(jsonString);
        } catch (Exception e) {
            result.isValid = false;
            result.errors.add("Invalid JSON format");
            return result;
        }

        for (FieldDefinition field : effectiveFields) {
            JsonNode valueNode = dataNode.get(field.getKey());
            
            // 1. Required Check
            if (field.getRequired() != null && field.getRequired()) {
                boolean isAutoNumbering = false;
                if (dom != null && dom.getIdentifierFieldId() != null) {
                    String domIdStr = dom.getIdentifierFieldId().toString();
                    String fieldIdStr = field.getId() != null ? field.getId().toString() : "";
                    boolean matchesId = domIdStr.equalsIgnoreCase(fieldIdStr);
                    
                    String targetKey = effectiveFields.stream()
                        .filter(f -> f.getId() != null && domIdStr.equalsIgnoreCase(f.getId().toString()))
                        .map(FieldDefinition::getKey)
                        .findFirst()
                        .orElse(null);
                    boolean matchesKey = field.getKey() != null && field.getKey().equalsIgnoreCase(targetKey);
                    
                    if (matchesId || matchesKey) {
                        if (dom.getNumberingPattern() != null && !dom.getNumberingPattern().isBlank()) {
                            isAutoNumbering = true;
                        }
                    }
                }
                
                if (!isAutoNumbering) {
                    if (valueNode == null || valueNode.isNull() || (valueNode.isTextual() && valueNode.asText().trim().isEmpty())) {
                        result.errors.add("Field '" + getFieldName(field) + "' is required.");
                        continue;
                    }
                }
            }

            // 2. Type Check (if present)
            if (valueNode != null && !valueNode.isNull()) {
                String type = field.getType();
                if ("NUMBER".equalsIgnoreCase(type)) {
                    if (!valueNode.isNumber() && !isNumeric(valueNode.asText())) {
                        result.errors.add("Field '" + getFieldName(field) + "' must be a number.");
                    }
                } else if ("BOOLEAN".equalsIgnoreCase(type) || "CHECKBOX".equalsIgnoreCase(type)) {
                    if (!valueNode.isBoolean() && !valueNode.asText().equalsIgnoreCase("true") && !valueNode.asText().equalsIgnoreCase("false")) {
                        result.errors.add("Field '" + getFieldName(field) + "' must be a boolean.");
                    }
                }
            }
        }

        result.isValid = result.errors.isEmpty();
        return result;
    }

    private String getFieldName(FieldDefinition field) {
        if (field.getName() != null) {
            return field.getName().getOrDefault("en", field.getName().getOrDefault("ko", field.getKey()));
        }
        return field.getKey();
    }

    private boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) return false;
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
