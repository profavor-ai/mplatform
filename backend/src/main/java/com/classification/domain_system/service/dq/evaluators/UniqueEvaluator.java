package com.classification.domain_system.service.dq.evaluators;

import com.classification.domain_system.entity.DqRule;
import com.classification.domain_system.entity.DqRuleType;
import com.classification.domain_system.entity.FieldDefinition;
import com.classification.domain_system.service.dq.EvaluationContext;
import com.classification.domain_system.service.dq.RuleEvaluator;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * UNIQUE evaluator — checks domain-wide uniqueness via native PostgreSQL JSONB query.
 */
@Component
public class UniqueEvaluator implements RuleEvaluator {
    private final JdbcTemplate jdbcTemplate;

    public UniqueEvaluator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public DqRuleType supports() {
        return DqRuleType.UNIQUE;
    }

    @Override
    public Optional<String> evaluate(FieldDefinition field, DqRule rule, JsonNode value, EvaluationContext context) {
        if (value == null || value.isNull()
                || (value.isTextual() && value.asText().trim().isEmpty())) {
            return Optional.empty(); // null values are not checked for uniqueness
        }
        if (context.getDomainId() == null) {
            return Optional.empty();
        }

        String textValue = value.asText();
        String fieldKey = field.getKey();
        
        String sql;
        Integer count;
        if (context.getRecordId() != null) {
            sql = "SELECT COUNT(*) FROM record r " +
                  "JOIN classification_node cn ON r.node_id = cn.id " +
                  "WHERE cn.domain_id = ? AND r.data->>? = ? " +
                  "AND r.status NOT IN ('REJECTED','MISMATCHED') AND r.id <> ?";
            count = jdbcTemplate.queryForObject(sql, Integer.class,
                    context.getDomainId(), fieldKey, textValue, context.getRecordId());
        } else {
            sql = "SELECT COUNT(*) FROM record r " +
                  "JOIN classification_node cn ON r.node_id = cn.id " +
                  "WHERE cn.domain_id = ? AND r.data->>? = ? " +
                  "AND r.status NOT IN ('REJECTED','MISMATCHED')";
            count = jdbcTemplate.queryForObject(sql, Integer.class,
                    context.getDomainId(), fieldKey, textValue);
        }

        if (count != null && count > 0) {
            return Optional.of("Value '" + textValue + "' already exists. Must be unique within the domain.");
        }
        return Optional.empty();
    }
}
