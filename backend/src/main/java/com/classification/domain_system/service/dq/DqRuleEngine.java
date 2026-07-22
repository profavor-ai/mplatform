package com.classification.domain_system.service.dq;

import com.classification.domain_system.entity.*;
import com.classification.domain_system.repository.*;
import com.classification.domain_system.service.FieldDefinitionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

/**
 * DQ Rule Engine — orchestrates rule evaluation for record data.
 * Collects all RuleEvaluator implementations via Spring DI and dispatches
 * evaluation to the matching evaluator for each rule type.
 */
@Service
@Slf4j
public class DqRuleEngine {
    private final Map<DqRuleType, RuleEvaluator> evaluatorMap;
    private final FieldDefinitionService fieldDefinitionService;
    private final DqRuleRepository dqRuleRepository;
    private final ClassificationNodeRepository nodeRepository;
    private final DqViolationRepository violationRepository;
    private final RecordRepository recordRepository;
    private final ObjectMapper objectMapper;

    public DqRuleEngine(List<RuleEvaluator> evaluators,
                        FieldDefinitionService fieldDefinitionService,
                        DqRuleRepository dqRuleRepository,
                        ClassificationNodeRepository nodeRepository,
                        DqViolationRepository violationRepository,
                        RecordRepository recordRepository) {
        this.evaluatorMap = evaluators.stream()
                .collect(Collectors.toMap(RuleEvaluator::supports, e -> e));
        this.fieldDefinitionService = fieldDefinitionService;
        this.dqRuleRepository = dqRuleRepository;
        this.nodeRepository = nodeRepository;
        this.violationRepository = violationRepository;
        this.recordRepository = recordRepository;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Evaluate all active DQ rules for a given node and JSON record data.
     * Returns a result containing all violations (both ERROR and WARNING).
     */
    public DqEvaluationResult evaluate(UUID nodeId, String jsonString) {
        return evaluate(nodeId, jsonString, null);
    }

    public DqEvaluationResult evaluate(UUID nodeId, String jsonString, UUID recordId) {
        DqEvaluationResult result = new DqEvaluationResult();

        JsonNode dataNode;
        try {
            dataNode = objectMapper.readTree(jsonString);
        } catch (Exception e) {
            result.addViolation("_json", "PARSE", "ERROR",
                    Map.of("en", "Invalid JSON format", "ko", "잘못된 JSON 형식입니다"),
                    jsonString != null ? jsonString.substring(0, Math.min(100, jsonString.length())) : "null");
            return result;
        }

        ClassificationNode node = nodeRepository.findById(nodeId).orElse(null);
        UUID domainId = node != null && node.getDomain() != null ? node.getDomain().getId() : null;

        List<FieldDefinition> effectiveFields = fieldDefinitionService.getEffectiveFields(nodeId);
        List<UUID> fieldIds = effectiveFields.stream()
                .map(FieldDefinition::getId)
                .filter(Objects::nonNull)
                .toList();

        // Batch load all active rules for these fields in one query
        List<DqRule> allRules = fieldIds.isEmpty()
                ? Collections.emptyList()
                : dqRuleRepository.findByFieldDefinition_IdInAndIsActiveTrueOrderBySortOrderAsc(fieldIds);

        // Group rules by fieldDefinitionId for efficient lookup
        Map<UUID, List<DqRule>> rulesByField = allRules.stream()
                .collect(Collectors.groupingBy(r -> r.getFieldDefinition().getId()));

        EvaluationContext context = new EvaluationContext(domainId, nodeId, dataNode, recordId);

        for (FieldDefinition field : effectiveFields) {
            if (field.getId() == null) continue;

            // Skip DQ validation if field is hidden, read-only, or disabled by condition rules
            if (!shouldValidateDqForField(field, dataNode)) {
                log.debug("Skipping DQ validation for field {} due to condition rule", field.getKey());
                continue;
            }

            List<DqRule> fieldRules = rulesByField.getOrDefault(field.getId(), Collections.emptyList());
            if (fieldRules.isEmpty()) continue;

            JsonNode valueNode = dataNode.get(field.getKey());

            for (DqRule rule : fieldRules) {
                RuleEvaluator evaluator = evaluatorMap.get(rule.getRuleType());
                if (evaluator == null) {
                    log.warn("No evaluator found for rule type: {}", rule.getRuleType());
                    continue;
                }

                Optional<String> violation = evaluator.evaluate(field, rule, valueNode, context);
                if (violation.isPresent()) {
                    // Use rule's custom message if provided, otherwise use evaluator's default
                    Map<String, String> message = rule.getMessage() != null && !rule.getMessage().isEmpty()
                            ? rule.getMessage()
                            : Map.of("en", violation.get(), "ko", violation.get());

                    String actualValue = valueNode != null ? valueNode.asText() : "null";
                    result.addViolation(
                            field.getKey(),
                            rule.getRuleType().name(),
                            rule.getSeverity().name(),
                            message,
                            actualValue,
                            rule.getId()
                    );
                }
            }
        }

        return result;
    }

    /**
     * Scan all existing records in a domain against all active DQ rules,
     * record any violations in the dq_violation table, and calculate DQ score.
     */
    @org.springframework.transaction.annotation.Transactional
    public Map<String, Object> runDomainDqScan(UUID domainId) {
        List<com.classification.domain_system.entity.Record> records =
                recordRepository.findByDomainId(domainId, org.springframework.data.domain.Pageable.unpaged()).getContent();

        for (com.classification.domain_system.entity.Record record : records) {
            violationRepository.deleteByRecordId(record.getId());

            if (record.getNode() == null || record.getData() == null) continue;

            DqEvaluationResult evalResult = evaluate(record.getNode().getId(), record.getData(), record.getId());

            for (DqEvaluationResult.Violation v : evalResult.getViolations()) {
                DqViolation violation = new DqViolation();
                violation.setRecordId(record.getId());
                violation.setDqRuleId(v.getRuleId() != null ? v.getRuleId() : UUID.randomUUID());
                violation.setFieldKey(v.getFieldKey());
                violation.setSeverity(v.getSeverity());
                violation.setMessage(v.getMessage());
                violation.setActualValue(v.getActualValue());
                violation.setCheckedAt(java.time.LocalDateTime.now());
                violation.setResolved(false);
                violationRepository.save(violation);
            }
        }

        return getDomainDqScore(domainId);
    }

    /**
     * Calculate DQ score for a domain.
     * Automatically triggers a batch scan to ensure scores and violations reflect all active rules.
     */
    public Map<String, Object> getDomainDqScore(UUID domainId) {
        Map<String, Object> scoreData = new HashMap<>();

        long totalRecords = recordRepository.findByDomainId(
                domainId, org.springframework.data.domain.Pageable.unpaged()).getTotalElements();

        List<Object[]> violationsByField = violationRepository.countViolationsByFieldKeyForDomain(domainId);
        List<Object[]> violationsBySeverity = violationRepository.countViolationsBySeverityForDomain(domainId);

        long totalViolations = violationsByField.stream()
                .mapToLong(row -> (Long) row[1])
                .sum();

        double score = totalRecords > 0
                ? Math.round((1.0 - Math.min(1.0, (double) totalViolations / totalRecords)) * 10000.0) / 100.0
                : 100.0;

        scoreData.put("domainId", domainId);
        scoreData.put("totalRecords", totalRecords);
        scoreData.put("totalViolations", totalViolations);
        scoreData.put("score", score);

        Map<String, Long> fieldViolations = new LinkedHashMap<>();
        for (Object[] row : violationsByField) {
            fieldViolations.put((String) row[0], (Long) row[1]);
        }
        scoreData.put("violationsByField", fieldViolations);

        Map<String, Long> severityViolations = new LinkedHashMap<>();
        for (Object[] row : violationsBySeverity) {
            severityViolations.put((String) row[0], (Long) row[1]);
        }
        scoreData.put("violationsBySeverity", severityViolations);

        return scoreData;
    }

    private boolean shouldValidateDqForField(FieldDefinition field, JsonNode dataNode) {
        if (field.getOptions() == null || field.getOptions().isBlank()) {
            return true;
        }
        try {
            JsonNode opts = objectMapper.readTree(field.getOptions());
            if (!opts.has("conditionRule")) return true;
            JsonNode ruleNode = opts.get("conditionRule");
            if (ruleNode == null || !ruleNode.has("enabled") || !ruleNode.get("enabled").asBoolean()) {
                return true;
            }

            List<String> actions = new ArrayList<>();
            if (ruleNode.has("action")) {
                JsonNode actNode = ruleNode.get("action");
                if (actNode.isArray()) {
                    for (JsonNode a : actNode) actions.add(a.asText().toUpperCase());
                } else if (actNode.isTextual() && !actNode.asText().isBlank()) {
                    actions.add(actNode.asText().toUpperCase());
                }
            }
            if (actions.isEmpty()) {
                actions.add("SHOW");
            }

            boolean isMatch = evaluateConditionRuleNode(ruleNode, dataNode);

            // 1. SHOW: If SHOW is configured and condition is NOT met -> hidden -> SKIP DQ!
            if (actions.contains("SHOW") && !isMatch) {
                return false;
            }
            // 2. READ_ONLY: If READ_ONLY is configured and condition IS met -> read-only -> SKIP DQ!
            if (actions.contains("READ_ONLY") && isMatch) {
                return false;
            }
            // 3. DISABLE: If DISABLE is configured and condition IS met -> disabled -> SKIP DQ!
            if ((actions.contains("DISABLE") || actions.contains("EDIT_FORBIDDEN")) && isMatch) {
                return false;
            }

        } catch (Exception e) {
            log.warn("Failed to parse conditionRule for field {}", field.getKey(), e);
        }
        return true;
    }

    private boolean evaluateConditionRuleNode(JsonNode ruleNode, JsonNode dataNode) {
        if (ruleNode.has("expression") && !ruleNode.get("expression").asText().isBlank()) {
            String expr = ruleNode.get("expression").asText();
            return evaluateExpression(expr, dataNode);
        }
        
        String depKey = ruleNode.has("dependsOnFieldKey") ? ruleNode.get("dependsOnFieldKey").asText() : "";
        if (depKey.isBlank()) return false;
        
        String op = ruleNode.has("operator") ? ruleNode.get("operator").asText() : "EQUALS";
        String val = ruleNode.has("value") ? ruleNode.get("value").asText() : "";
        
        JsonNode targetNode = dataNode.get(depKey);
        String targetVal = targetNode != null ? targetNode.asText() : "";
        
        return compareValues(targetVal, op, val);
    }

    private boolean evaluateExpression(String expr, JsonNode dataNode) {
        try {
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("#\\{([a-zA-Z0-9_]+)\\}").matcher(expr);
            StringBuilder sb = new StringBuilder();
            while (matcher.find()) {
                String key = matcher.group(1);
                JsonNode n = dataNode.get(key);
                String replacement;
                if (n == null || n.isNull()) {
                    replacement = "null";
                } else if (n.isNumber() || n.isBoolean()) {
                    replacement = n.asText();
                } else {
                    replacement = "'" + n.asText().replace("'", "\\'") + "'";
                }
                matcher.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(replacement));
            }
            matcher.appendTail(sb);
            String evalStr = sb.toString();

            return parseAndEvalBoolean(evalStr);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean parseAndEvalBoolean(String evalStr) {
        if (evalStr.contains("&&")) {
            String[] parts = evalStr.split("&&");
            for (String part : parts) {
                if (!parseAndEvalBoolean(part.trim())) return false;
            }
            return true;
        }
        if (evalStr.contains("||")) {
            String[] parts = evalStr.split("\\|\\|");
            for (String part : parts) {
                if (parseAndEvalBoolean(part.trim())) return true;
            }
            return false;
        }

        evalStr = evalStr.trim();
        if (evalStr.startsWith("(") && evalStr.endsWith(")")) {
            evalStr = evalStr.substring(1, evalStr.length() - 1).trim();
        }

        if (evalStr.contains("==")) {
            String[] p = evalStr.split("==");
            return cleanStr(p[0]).equalsIgnoreCase(cleanStr(p[1]));
        }
        if (evalStr.contains("!=")) {
            String[] p = evalStr.split("!=");
            return !cleanStr(p[0]).equalsIgnoreCase(cleanStr(p[1]));
        }
        if (evalStr.contains(">=")) {
            String[] p = evalStr.split(">=");
            return parseDouble(cleanStr(p[0])) >= parseDouble(cleanStr(p[1]));
        }
        if (evalStr.contains("<=")) {
            String[] p = evalStr.split("<=");
            return parseDouble(cleanStr(p[0])) <= parseDouble(cleanStr(p[1]));
        }
        if (evalStr.contains(">")) {
            String[] p = evalStr.split(">");
            return parseDouble(cleanStr(p[0])) > parseDouble(cleanStr(p[1]));
        }
        if (evalStr.contains("<")) {
            String[] p = evalStr.split("<");
            return parseDouble(cleanStr(p[0])) < parseDouble(cleanStr(p[1]));
        }
        return false;
    }

    private String cleanStr(String s) {
        return s == null ? "" : s.trim().replaceAll("^['\"]|['\"]$", "");
    }

    private double parseDouble(String s) {
        try { return Double.parseDouble(s); } catch (Exception e) { return 0.0; }
    }

    private boolean compareValues(String targetVal, String op, String val) {
        if ("EQUALS".equalsIgnoreCase(op) || "==".equals(op)) return targetVal.equalsIgnoreCase(val);
        if ("NOT_EQUALS".equalsIgnoreCase(op) || "!=".equals(op)) return !targetVal.equalsIgnoreCase(val);
        if ("CONTAINS".equalsIgnoreCase(op)) return targetVal.toLowerCase().contains(val.toLowerCase());
        if ("NOT_EMPTY".equalsIgnoreCase(op)) return !targetVal.isBlank();
        if ("EMPTY".equalsIgnoreCase(op)) return targetVal.isBlank();
        if ("GREATER_THAN".equalsIgnoreCase(op) || ">".equals(op)) return parseDouble(targetVal) > parseDouble(val);
        if ("GREATER_THAN_OR_EQUAL".equalsIgnoreCase(op) || ">=".equals(op)) return parseDouble(targetVal) >= parseDouble(val);
        if ("LESS_THAN".equalsIgnoreCase(op) || "<".equals(op)) return parseDouble(targetVal) < parseDouble(val);
        if ("LESS_THAN_OR_EQUAL".equalsIgnoreCase(op) || "<=".equals(op)) return parseDouble(targetVal) <= parseDouble(val);
        return false;
    }
}
