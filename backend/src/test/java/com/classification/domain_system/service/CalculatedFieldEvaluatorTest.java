package com.classification.domain_system.service;

import com.classification.domain_system.entity.FieldDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalculatedFieldEvaluatorTest {

    @Mock
    private FieldDefinitionService fieldDefinitionService;

    @InjectMocks
    private CalculatedFieldEvaluator evaluator;

    private UUID nodeId;

    @BeforeEach
    void setUp() {
        nodeId = UUID.randomUUID();
    }

    @Test
    @DisplayName("evaluateFormula - 사칙연산 및 수학함수 평가")
    void evaluateFormula_MathFunctions() {
        Map<String, Object> data = Map.of("PER", 10.5, "PBR", 2);

        Double result = evaluator.evaluateFormula("${PER} * ${PBR}", data);
        assertThat(result).isEqualTo(21.0);

        Double roundResult = evaluator.evaluateFormula("ROUND(${PER} * ${PBR}, 0)", data);
        assertThat(roundResult).isEqualTo(21.0);

        Double ceilResult = evaluator.evaluateFormula("CEIL(10.1)", data);
        assertThat(ceilResult).isEqualTo(11.0);
    }

    @Test
    @DisplayName("recomputeCalculatedFields - CALCULATED 필드가 포함된 JSON재계산")
    void recomputeCalculatedFields_Success() {
        FieldDefinition field = new FieldDefinition();
        field.setType("CALCULATED");
        field.setKey("TARGET");
        field.setOptions("{\"formula\":\"${VAL} * 2\"}");

        when(fieldDefinitionService.getEffectiveFields(eq(nodeId))).thenReturn(List.of(field));

        String inputJson = "{\"VAL\": 15}";
        String outputJson = evaluator.recomputeCalculatedFields(nodeId, inputJson);

        assertThat(outputJson).contains("\"TARGET\":30");
    }
}
