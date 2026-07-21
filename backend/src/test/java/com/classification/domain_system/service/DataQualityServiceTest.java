package com.classification.domain_system.service;

import com.classification.domain_system.entity.FieldDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.classification.domain_system.repository.ClassificationNodeRepository;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class DataQualityServiceTest {

    @Mock
    private FieldDefinitionService fieldDefinitionService;

    @Mock
    private ClassificationNodeRepository nodeRepository;

    @InjectMocks
    private DataQualityService dataQualityService;

    private UUID nodeId;

    @BeforeEach
    void setUp() {
        nodeId = UUID.randomUUID();
        lenient().when(nodeRepository.findById(nodeId)).thenReturn(Optional.empty());
    }

    // ─────────────────────────────────────────────────────────────────
    // validateData - Required 체크
    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("validateData - Required 필드 검증")
    class RequiredCheck {

        @Test
        @DisplayName("required 필드가 null이면 isValid=false, 에러 메시지 추가")
        void requiredFieldNull_Invalid() {
            FieldDefinition field = makeField("name", "TEXT", true);
            when(fieldDefinitionService.getEffectiveFields(nodeId)).thenReturn(List.of(field));

            DataQualityService.DQResult result = dataQualityService.validateData(nodeId, "{\"name\":null}");

            assertThat(result.isValid).isFalse();
            assertThat(result.errors).anyMatch(e -> e.contains("required") || e.contains("is required"));
        }

        @Test
        @DisplayName("required 필드가 빈 문자열이면 isValid=false")
        void requiredFieldEmpty_Invalid() {
            FieldDefinition field = makeField("name", "TEXT", true);
            when(fieldDefinitionService.getEffectiveFields(nodeId)).thenReturn(List.of(field));

            DataQualityService.DQResult result = dataQualityService.validateData(nodeId, "{\"name\":\"   \"}");

            assertThat(result.isValid).isFalse();
        }

        @Test
        @DisplayName("required 필드가 정상 값이면 isValid=true")
        void requiredFieldPresent_Valid() {
            FieldDefinition field = makeField("name", "TEXT", true);
            when(fieldDefinitionService.getEffectiveFields(nodeId)).thenReturn(List.of(field));

            DataQualityService.DQResult result = dataQualityService.validateData(nodeId, "{\"name\":\"홍길동\"}");

            assertThat(result.isValid).isTrue();
            assertThat(result.errors).isEmpty();
        }

        @Test
        @DisplayName("required=false 필드가 누락돼도 isValid=true")
        void nonRequiredFieldMissing_Valid() {
            FieldDefinition field = makeField("remark", "TEXT", false);
            when(fieldDefinitionService.getEffectiveFields(nodeId)).thenReturn(List.of(field));

            DataQualityService.DQResult result = dataQualityService.validateData(nodeId, "{}");

            assertThat(result.isValid).isTrue();
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // validateData - 타입 체크
    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("validateData - 타입 검증")
    class TypeCheck {

        @Test
        @DisplayName("NUMBER 필드에 문자열 'abc' 입력 시 타입 에러")
        void numberField_StringValue_TypeError() {
            FieldDefinition field = makeField("price", "NUMBER", false);
            when(fieldDefinitionService.getEffectiveFields(nodeId)).thenReturn(List.of(field));

            DataQualityService.DQResult result = dataQualityService.validateData(nodeId, "{\"price\":\"abc\"}");

            assertThat(result.isValid).isFalse();
            assertThat(result.errors).anyMatch(e -> e.contains("number") || e.contains("NUMBER"));
        }

        @Test
        @DisplayName("NUMBER 필드에 '3.14' 문자열 입력 시 통과")
        void numberField_NumericString_Valid() {
            FieldDefinition field = makeField("price", "NUMBER", false);
            when(fieldDefinitionService.getEffectiveFields(nodeId)).thenReturn(List.of(field));

            DataQualityService.DQResult result = dataQualityService.validateData(nodeId, "{\"price\":\"3.14\"}");

            assertThat(result.isValid).isTrue();
        }

        @Test
        @DisplayName("NUMBER 필드에 JSON 숫자 타입 입력 시 통과")
        void numberField_JsonNumber_Valid() {
            FieldDefinition field = makeField("price", "NUMBER", false);
            when(fieldDefinitionService.getEffectiveFields(nodeId)).thenReturn(List.of(field));

            DataQualityService.DQResult result = dataQualityService.validateData(nodeId, "{\"price\":100}");

            assertThat(result.isValid).isTrue();
        }

        @Test
        @DisplayName("BOOLEAN 필드에 'true' 문자열 허용")
        void booleanField_TrueString_Valid() {
            FieldDefinition field = makeField("active", "BOOLEAN", false);
            when(fieldDefinitionService.getEffectiveFields(nodeId)).thenReturn(List.of(field));

            DataQualityService.DQResult result = dataQualityService.validateData(nodeId, "{\"active\":\"true\"}");

            assertThat(result.isValid).isTrue();
        }

        @Test
        @DisplayName("BOOLEAN 필드에 'yes' 문자열 거부")
        void booleanField_YesString_Invalid() {
            FieldDefinition field = makeField("active", "BOOLEAN", false);
            when(fieldDefinitionService.getEffectiveFields(nodeId)).thenReturn(List.of(field));

            DataQualityService.DQResult result = dataQualityService.validateData(nodeId, "{\"active\":\"yes\"}");

            assertThat(result.isValid).isFalse();
            assertThat(result.errors).anyMatch(e -> e.contains("boolean") || e.contains("BOOLEAN"));
        }

        @Test
        @DisplayName("CHECKBOX 필드에 JSON boolean true 허용")
        void checkboxField_JsonBoolean_Valid() {
            FieldDefinition field = makeField("checked", "CHECKBOX", false);
            when(fieldDefinitionService.getEffectiveFields(nodeId)).thenReturn(List.of(field));

            DataQualityService.DQResult result = dataQualityService.validateData(nodeId, "{\"checked\":true}");

            assertThat(result.isValid).isTrue();
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // validateData - 기타
    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("validateData - 기타")
    class Misc {

        @Test
        @DisplayName("잘못된 JSON 형식이면 isValid=false, 'Invalid JSON format' 에러")
        void invalidJson_ReturnsInvalidResult() {
            when(fieldDefinitionService.getEffectiveFields(nodeId)).thenReturn(List.of());

            DataQualityService.DQResult result = dataQualityService.validateData(nodeId, "NOT_JSON");

            assertThat(result.isValid).isFalse();
            assertThat(result.errors).anyMatch(e -> e.contains("Invalid JSON"));
        }

        @Test
        @DisplayName("getFieldName - en 키 있으면 en 이름 반환")
        void getFieldName_EnPresent_ReturnsEn() {
            FieldDefinition field = makeField("ticker", "TEXT", true);
            field.setName(Map.of("en", "Ticker", "ko", "종목코드"));
            when(fieldDefinitionService.getEffectiveFields(nodeId)).thenReturn(List.of(field));

            // ticker 값 누락 → required 에러 메시지에 "Ticker"가 포함되어야 함
            DataQualityService.DQResult result = dataQualityService.validateData(nodeId, "{}");

            assertThat(result.errors).anyMatch(e -> e.contains("Ticker"));
        }

        @Test
        @DisplayName("getFieldName - en 없고 ko만 있으면 ko 이름 반환")
        void getFieldName_OnlyKo_ReturnsKo() {
            FieldDefinition field = makeField("ticker", "TEXT", true);
            field.setName(Map.of("ko", "종목코드"));
            when(fieldDefinitionService.getEffectiveFields(nodeId)).thenReturn(List.of(field));

            DataQualityService.DQResult result = dataQualityService.validateData(nodeId, "{}");

            assertThat(result.errors).anyMatch(e -> e.contains("종목코드"));
        }

        @Test
        @DisplayName("getFieldName - name이 null이면 key 반환")
        void getFieldName_NullName_ReturnsKey() {
            FieldDefinition field = makeField("ticker", "TEXT", true);
            field.setName(null);
            when(fieldDefinitionService.getEffectiveFields(nodeId)).thenReturn(List.of(field));

            DataQualityService.DQResult result = dataQualityService.validateData(nodeId, "{}");

            assertThat(result.errors).anyMatch(e -> e.contains("ticker"));
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // 헬퍼 메서드
    // ─────────────────────────────────────────────────────────────────
    private FieldDefinition makeField(String key, String type, boolean required) {
        FieldDefinition f = new FieldDefinition();
        f.setId(UUID.randomUUID());
        f.setKey(key);
        f.setType(type);
        f.setRequired(required);
        f.setName(Map.of("en", key));
        return f;
    }
}
