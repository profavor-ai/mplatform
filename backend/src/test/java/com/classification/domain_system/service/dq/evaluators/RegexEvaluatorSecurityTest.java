package com.classification.domain_system.service.dq.evaluators;

import com.classification.domain_system.entity.DqRule;
import com.classification.domain_system.entity.DqRuleType;
import com.classification.domain_system.entity.FieldDefinition;
import com.classification.domain_system.service.dq.EvaluationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RegexEvaluatorSecurityTest {

    private RegexEvaluator evaluator;
    private ObjectMapper objectMapper;
    private FieldDefinition field;
    private EvaluationContext context;

    @BeforeEach
    void setUp() {
        evaluator = new RegexEvaluator();
        objectMapper = new ObjectMapper();

        field = new FieldDefinition();
        field.setId(UUID.randomUUID());
        field.setKey("code");

        context = new EvaluationContext(UUID.randomUUID(), UUID.randomUUID(), null, UUID.randomUUID());
    }

    private DqRule makeRule(String pattern) {
        DqRule rule = new DqRule();
        rule.setRuleType(DqRuleType.REGEX);
        rule.setParams("{\"pattern\":\"" + pattern.replace("\\", "\\\\").replace("\"", "\\\"") + "\"}");
        return rule;
    }

    @Test
    @DisplayName("정상 정규식 패턴 평가 통과")
    void validPattern_ShouldPass() {
        DqRule rule = makeRule("^[A-Z]{2}\\d{4}$");
        var value = objectMapper.valueToTree("AB1234");

        Optional<String> result = evaluator.evaluate(field, rule, value, context);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("초과 길이 정규식 패턴 거부")
    void excessivelyLongPattern_ShouldBeRejected() {
        String longPattern = "^" + "a".repeat(300) + "$";
        DqRule rule = makeRule(longPattern);
        var value = objectMapper.valueToTree("test");

        Optional<String> result = evaluator.evaluate(field, rule, value, context);

        assertThat(result).isPresent();
        assertThat(result.get()).contains("length limit");
    }

    @Test
    @DisplayName("ReDoS (재앙적 백트래킹) 패턴 타임아웃 방어")
    void redosPattern_ShouldTimeoutAndReturnError() {
        // Catastrophic backtracking pattern: (.*a){30} on "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa!"
        DqRule rule = makeRule("(.*a){30}");
        var value = objectMapper.valueToTree("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa!");

        Optional<String> result = evaluator.evaluate(field, rule, value, context);

        assertThat(result).isPresent();
        assertThat(result.get()).contains("timed out");
    }
}
