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
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@Component
public class RegexEvaluator implements RuleEvaluator {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public DqRuleType supports() {
        return DqRuleType.REGEX;
    }

    @Override
    public Optional<String> evaluate(FieldDefinition field, DqRule rule, JsonNode value, EvaluationContext context) {
        // Skip null/empty values — NOT_NULL handles that
        if (value == null || value.isNull() || (value.isTextual() && value.asText().trim().isEmpty())) {
            return Optional.empty();
        }

        try {
            JsonNode paramsNode = objectMapper.readTree(rule.getParams());
            String pattern = paramsNode.get("pattern").asText();
            String textValue = value.asText();

            if (!Pattern.compile(pattern).matcher(textValue).matches()) {
                return Optional.of("Value does not match pattern: " + pattern);
            }
            return Optional.empty();
        } catch (PatternSyntaxException e) {
            return Optional.of("Invalid regex pattern: " + e.getMessage());
        } catch (Exception e) {
            return Optional.of("Regex evaluation error: " + e.getMessage());
        }
    }
}
