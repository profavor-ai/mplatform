package com.classification.domain_system.service.dq.evaluators;

import com.classification.domain_system.entity.DqRule;
import com.classification.domain_system.entity.DqRuleType;
import com.classification.domain_system.entity.FieldDefinition;
import com.classification.domain_system.service.dq.EvaluationContext;
import com.classification.domain_system.service.dq.RuleEvaluator;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class NotNullEvaluator implements RuleEvaluator {
    @Override
    public DqRuleType supports() {
        return DqRuleType.NOT_NULL;
    }

    @Override
    public Optional<String> evaluate(FieldDefinition field, DqRule rule, JsonNode value, EvaluationContext context) {
        if (value == null || value.isNull()
                || (value.isTextual() && value.asText().trim().isEmpty())) {
            return Optional.of("Value is required");
        }
        return Optional.empty();
    }
}
