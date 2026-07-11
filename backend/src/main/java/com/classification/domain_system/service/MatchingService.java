package com.classification.domain_system.service;

import com.classification.domain_system.entity.MatchingRule;
import com.classification.domain_system.entity.Record;
import com.classification.domain_system.repository.MatchingRuleRepository;
import com.classification.domain_system.repository.RecordRepository;
import com.classification.domain_system.entity.ClassificationNode;
import com.classification.domain_system.repository.ClassificationNodeRepository;
import com.classification.domain_system.repository.FieldDefinitionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import java.util.*;
import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
public class MatchingService {
    
    private final MatchingRuleRepository matchingRuleRepository;
    private final RecordRepository recordRepository;
    private final ClassificationNodeRepository nodeRepository;
    private final FieldDefinitionRepository fieldDefinitionRepository;
    private final ObjectMapper mapper = new ObjectMapper();

    public static class DuplicateResult {
        public boolean hasDuplicates;
        public List<UUID> duplicateRecordIds;
        public String message;
    }

    public DuplicateResult checkDuplicates(UUID nodeId, String dataJson) {
        DuplicateResult result = new DuplicateResult();
        result.hasDuplicates = false;
        result.duplicateRecordIds = new ArrayList<>();

        ClassificationNode node = nodeRepository.findById(nodeId).orElse(null);
        if (node == null) return result;

        List<MatchingRule> rules = matchingRuleRepository.findByDomainIdAndIsActiveTrue(node.getDomain().getId());

        try {
            Map<String, Object> data = mapper.readValue(dataJson, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
            
            // 1. Default Check: Domain Identifier Field
            UUID idFieldId = node.getDomain().getIdentifierFieldId();
            if (idFieldId != null) {
                com.classification.domain_system.entity.FieldDefinition idDef = fieldDefinitionRepository.findById(idFieldId).orElse(null);
                if (idDef != null && data.containsKey(idDef.getKey())) {
                    Object val = data.get(idDef.getKey());
                    if (val != null && !val.toString().isBlank()) {
                        Map<String, String> searchParams = new HashMap<>();
                        searchParams.put(idDef.getKey(), val.toString());
                        searchParams.put("op_" + idDef.getKey(), "EQ");
                        
                        List<Record> duplicates = recordRepository.findDynamicRecords(List.of(nodeId), null, searchParams, Pageable.unpaged()).getContent();
                        if (!duplicates.isEmpty()) {
                            result.hasDuplicates = true;
                            duplicates.forEach(d -> result.duplicateRecordIds.add(d.getId()));
                            result.message = "Duplicate found based on Identifier Field (" + idDef.getName() + ")";
                            return result;
                        }
                    }
                }
            }

            // 2. Additional Custom Rules
            if (rules.isEmpty()) return result;
            
            for (MatchingRule rule : rules) {
                // Check if rule applies to this node
                if (rule.getNodeId() != null && !rule.getNodeId().equals(nodeId)) {
                    continue;
                }
                
                String[] fields = mapper.readValue(rule.getTargetFieldKeys(), String[].class);
                if (fields.length == 0) continue;

                // Build search params
                Map<String, String> searchParams = new HashMap<>();
                boolean hasAllFields = true;
                for (String field : fields) {
                    Object val = data.get(field);
                    if (val == null || val.toString().isBlank()) {
                        hasAllFields = false;
                        break;
                    }
                    searchParams.put(field, val.toString());
                    searchParams.put("op_" + field, "EQ");
                }

                if (hasAllFields) {
                    // Search for existing records matching these fields EXACTLY
                    // Using findDynamicRecords which utilizes GIN index
                    List<Record> duplicates = recordRepository.findDynamicRecords(List.of(nodeId), null, searchParams, Pageable.unpaged()).getContent();
                    if (!duplicates.isEmpty()) {
                        result.hasDuplicates = true;
                        duplicates.forEach(d -> result.duplicateRecordIds.add(d.getId()));
                        result.message = "Potential duplicate found based on rule: " + rule.getRuleName() + " (fields: " + Arrays.toString(fields) + ")";
                        return result;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }
}
