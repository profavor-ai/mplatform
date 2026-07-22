package com.classification.domain_system.service.dq.evaluators;

import com.classification.domain_system.entity.DqRule;
import com.classification.domain_system.entity.DqRuleType;
import com.classification.domain_system.entity.FieldDefinition;
import com.classification.domain_system.service.dq.EvaluationContext;
import com.classification.domain_system.service.dq.RuleEvaluator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.expression.spel.support.StandardTypeLocator;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * CUSTOM_SPEL evaluator — executes SpEL expressions with sandboxed context.
 * Uses StandardEvaluationContext with a restricted TypeLocator that blocks
 * T() type references to prevent access to arbitrary Java classes.
 * Method calls on variables (e.g., #value.contains('@')) are allowed.
 * 
 * Available variables in expressions:
 *   #value    — current field value as String
 *   #fieldKey — current field key
 *   #record   — full record as Map<String, Object>
 * 
 * params: {"expression": "#value.contains('@')"}
 */
@Component
public class SpelEvaluator implements RuleEvaluator {
    private final SpelExpressionParser parser = new SpelExpressionParser();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public DqRuleType supports() {
        return DqRuleType.CUSTOM_SPEL;
    }

    @Override
    public Optional<String> evaluate(FieldDefinition field, DqRule rule, JsonNode value, EvaluationContext context) {
        if (value == null || value.isNull()) {
            return Optional.empty();
        }

        try {
            JsonNode paramsNode = objectMapper.readTree(rule.getParams());
            String expressionStr = paramsNode.get("expression").asText();

            String valueStr = value.isTextual() ? value.asText() : value.toString();

            // Sandboxed context — blocks T() type references to prevent arbitrary class access
            StandardEvaluationContext evalContext = new StandardEvaluationContext();
            evalContext.setTypeLocator(typeName -> {
                throw new org.springframework.expression.spel.SpelEvaluationException(
                        org.springframework.expression.spel.SpelMessage.TYPE_NOT_FOUND, typeName);
            });
            evalContext.setVariable("value", valueStr);
            evalContext.setVariable("fieldKey", field.getKey());

            if (context.getFullRecord() != null) {
                Map<String, Object> recordMap = objectMapper.convertValue(
                        context.getFullRecord(), new TypeReference<Map<String, Object>>() {});
                evalContext.setVariable("record", recordMap);
            }

            Expression exp = parser.parseExpression(expressionStr);
            Boolean result = exp.getValue(evalContext, Boolean.class);

            if (result == null || !result) {
                return Optional.of("Custom validation failed: " + expressionStr);
            }
            return Optional.empty();
        } catch (Exception e) {
            return Optional.of("SpEL evaluation error: " + e.getMessage());
        }
    }
}
