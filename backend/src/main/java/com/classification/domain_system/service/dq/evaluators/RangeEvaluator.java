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

@Component
public class RangeEvaluator implements RuleEvaluator {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public DqRuleType supports() {
        return DqRuleType.RANGE;
    }

    @Override
    public Optional<String> evaluate(FieldDefinition field, DqRule rule, JsonNode value, EvaluationContext context) {
        if (value == null || value.isNull() || (value.isTextual() && value.asText().trim().isEmpty())) {
            return Optional.empty();
        }

        double numericValue;
        try {
            if (value.isNumber()) {
                numericValue = value.asDouble();
            } else {
                numericValue = Double.parseDouble(value.asText());
            }
        } catch (NumberFormatException e) {
            return Optional.of("Value must be a number for range check");
        }

        try {
            JsonNode paramsNode = objectMapper.readTree(rule.getParams());
            JsonNode minNode = paramsNode.get("min");
            JsonNode maxNode = paramsNode.get("max");

            if (minNode != null && !minNode.isNull() && numericValue < minNode.asDouble()) {
                return Optional.of("Value must be >= " + minNode.asDouble());
            }
            if (maxNode != null && !maxNode.isNull() && numericValue > maxNode.asDouble()) {
                return Optional.of("Value must be <= " + maxNode.asDouble());
            }
            return Optional.empty();
        } catch (Exception e) {
            return Optional.of("Range evaluation error: " + e.getMessage());
        }
    }
}
