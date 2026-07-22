package com.classification.domain_system.service.dq.evaluators;

import com.classification.domain_system.entity.DqRule;
import com.classification.domain_system.entity.DqRuleType;
import com.classification.domain_system.entity.FieldDefinition;
import com.classification.domain_system.service.dq.EvaluationContext;
import com.classification.domain_system.service.dq.RuleEvaluator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * CROSS_FIELD evaluator — compares current field value with another field's value.
 * params: {"targetFieldKey": "endDate", "operator": ">="}
 * Supported operators: ==, !=, >, >=, <, <=
 */
@Component
public class CrossFieldEvaluator implements RuleEvaluator {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public DqRuleType supports() {
        return DqRuleType.CROSS_FIELD;
    }

    @Override
    public Optional<String> evaluate(FieldDefinition field, DqRule rule, JsonNode value, EvaluationContext context) {
        if (value == null || value.isNull()) {
            return Optional.empty();
        }
        if (context.getFullRecord() == null) {
            return Optional.empty();
        }

        try {
            JsonNode paramsNode = objectMapper.readTree(rule.getParams());
            String targetFieldKey = paramsNode.get("targetFieldKey").asText();
            String operator = paramsNode.get("operator").asText();

            JsonNode targetValue = context.getFullRecord().get(targetFieldKey);
            if (targetValue == null || targetValue.isNull()) {
                return Optional.empty(); // skip if target field is absent
            }

            int comparison = compareValues(value, targetValue);

            boolean passed = switch (operator) {
                case "==" -> comparison == 0;
                case "!=" -> comparison != 0;
                case ">"  -> comparison > 0;
                case ">=" -> comparison >= 0;
                case "<"  -> comparison < 0;
                case "<=" -> comparison <= 0;
                default -> true; // unknown operator, skip
            };

            if (!passed) {
                return Optional.of("Field '" + field.getKey() + "' must be " + operator + " '" + targetFieldKey + "'");
            }
            return Optional.empty();
        } catch (Exception e) {
            return Optional.of("Cross-field evaluation error: " + e.getMessage());
        }
    }

    private int compareValues(JsonNode a, JsonNode b) {
        // Try numeric comparison first
        if ((a.isNumber() || isNumeric(a.asText())) && (b.isNumber() || isNumeric(b.asText()))) {
            return Double.compare(
                    a.isNumber() ? a.asDouble() : Double.parseDouble(a.asText()),
                    b.isNumber() ? b.asDouble() : Double.parseDouble(b.asText())
            );
        }
        // Fall back to lexicographic (works for ISO date strings too)
        return a.asText().compareTo(b.asText());
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
