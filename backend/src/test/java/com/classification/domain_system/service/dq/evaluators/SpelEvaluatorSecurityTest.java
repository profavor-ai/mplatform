package com.classification.domain_system.service.dq.evaluators;

import com.classification.domain_system.entity.DqRule;
import com.classification.domain_system.entity.DqRuleType;
import com.classification.domain_system.entity.FieldDefinition;
import com.classification.domain_system.service.dq.EvaluationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SpelEvaluatorSecurityTest {

    private SpelEvaluator spelEvaluator;
    private ObjectMapper objectMapper;
    private FieldDefinition field;
    private EvaluationContext context;

    @BeforeEach
    void setUp() {
        spelEvaluator = new SpelEvaluator();
        objectMapper = new ObjectMapper();

        field = new FieldDefinition();
        field.setId(UUID.randomUUID());
        field.setKey("email");

        context = new EvaluationContext(UUID.randomUUID(), UUID.randomUUID(), null, UUID.randomUUID());
    }

    private DqRule makeRule(String expression) {
        DqRule rule = new DqRule();
        rule.setRuleType(DqRuleType.CUSTOM_SPEL);
        rule.setParams("{\"expression\":\"" + expression.replace("\"", "\\\"") + "\"}");
        return rule;
    }

    @Test
    @DisplayName("안전한 SpEL 표현식 (#value.contains('@')) 정상 작동")
    void safeExpression_ShouldPass() {
        DqRule rule = makeRule("#value.contains('@')");
        var value = objectMapper.valueToTree("user@example.com");

        Optional<String> result = spelEvaluator.evaluate(field, rule, value, context);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("안전하지 않은 SpEL 표현식 (getClass().forName) 차단 및 RCE 방지")
    void reflectionExpression_ShouldBeBlocked() {
        // Attack payload attempting reflection RCE
        DqRule rule = makeRule("#value.getClass().forName('java.lang.Runtime').getMethod('getRuntime').invoke(null)");
        var value = objectMapper.valueToTree("test");

        Optional<String> result = spelEvaluator.evaluate(field, rule, value, context);

        // Must result in violation or error, NOT execute RCE
        assertThat(result).isPresent();
        assertThat(result.get()).contains("SpEL evaluation error");
    }

    @Test
    @DisplayName("안전하지 않은 SpEL 표현식 (T() 타입 참조) 차단")
    void typeReferenceExpression_ShouldBeBlocked() {
        DqRule rule = makeRule("T(java.lang.Runtime).getRuntime()");
        var value = objectMapper.valueToTree("test");

        Optional<String> result = spelEvaluator.evaluate(field, rule, value, context);

        assertThat(result).isPresent();
        assertThat(result.get()).contains("SpEL evaluation error");
    }

    @Test
    @DisplayName("피처 플래그가 false일 때 CUSTOM_SPEL 규칙 평가 거부")
    void whenFeatureDisabled_ShouldReturnViolation() {
        spelEvaluator.setEnabled(false);
        DqRule rule = makeRule("#value.contains('@')");
        var value = objectMapper.valueToTree("user@example.com");

        Optional<String> result = spelEvaluator.evaluate(field, rule, value, context);

        assertThat(result).isPresent();
        assertThat(result.get()).contains("disabled");
    }
}
