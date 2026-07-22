package com.classification.domain_system.service.dq.evaluators;

import com.classification.domain_system.entity.DqRule;
import com.classification.domain_system.entity.DqRuleType;
import com.classification.domain_system.entity.FieldDefinition;
import com.classification.domain_system.service.dq.EvaluationContext;
import com.classification.domain_system.service.dq.RuleEvaluator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Optional;

@Component
public class DateRangeEvaluator implements RuleEvaluator {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public DqRuleType supports() {
        return DqRuleType.DATE_RANGE;
    }

    @Override
    public Optional<String> evaluate(FieldDefinition field, DqRule rule, JsonNode value, EvaluationContext context) {
        if (value == null || value.isNull() || (value.isTextual() && value.asText().trim().isEmpty())) {
            return Optional.empty();
        }

        String textValue = value.asText();
        LocalDate dateValue;
        try {
            dateValue = LocalDate.parse(textValue);
        } catch (DateTimeParseException e) {
            return Optional.of("Invalid date format. Expected ISO format (yyyy-MM-dd)");
        }

        try {
            JsonNode paramsNode = objectMapper.readTree(rule.getParams());
            JsonNode afterNode = paramsNode.get("after");
            JsonNode beforeNode = paramsNode.get("before");

            if (afterNode != null && !afterNode.isNull()) {
                LocalDate afterDate = LocalDate.parse(afterNode.asText());
                if (dateValue.isBefore(afterDate)) {
                    return Optional.of("Date must be on or after " + afterNode.asText());
                }
            }
            if (beforeNode != null && !beforeNode.isNull()) {
                LocalDate beforeDate = LocalDate.parse(beforeNode.asText());
                if (dateValue.isAfter(beforeDate)) {
                    return Optional.of("Date must be on or before " + beforeNode.asText());
                }
            }
            return Optional.empty();
        } catch (DateTimeParseException e) {
            return Optional.of("Date range parameter parse error: " + e.getMessage());
        } catch (Exception e) {
            return Optional.of("Date range evaluation error: " + e.getMessage());
        }
    }
}
