package com.classification.domain_system.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.support.MessageBuilder;

import static org.assertj.core.api.Assertions.assertThat;

class DataMappingTransformerTest {

    private final DataMappingTransformer transformer = new DataMappingTransformer();

    @Test
    @DisplayName("Root Path가 없는 단일 객체 매핑 테스트")
    void testSingleObjectMapping() {
        String payloadJson = "{\"emp_id\":\"TEST\"}";
        String mappingConfigStr = "{" +
                "\"mappings\": [" +
                "  {\"targetField\": \"employee_id\", \"sourceExpression\": \"payload['emp_id']\"}" +
                "]" +
                "}";

        String result = transformer.transformPayload(payloadJson, mappingConfigStr);
        assertThat(result).contains("\"employee_id\":\"TEST\"");
    }

    @Test
    @DisplayName("Root Path를 이용한 다중 배열 매핑 테스트")
    void testArrayMappingWithRootPath() {
        String payloadJson = "{\"data\": [{\"emp_id\":\"T1\"}, {\"emp_id\":\"T2\"}]}";
        String mappingConfigStr = "{" +
                "\"rootPath\": \"payload['data']\"," +
                "\"mappings\": [" +
                "  {\"targetField\": \"employee_id\", \"sourceExpression\": \"#this['emp_id']\"}" +
                "]" +
                "}";

        String result = transformer.transformPayload(payloadJson, mappingConfigStr);
        
        // The result should be a JSON array of objects
        assertThat(result).contains("\"employee_id\":\"T1\"");
        assertThat(result).contains("\"employee_id\":\"T2\"");
        assertThat(result).startsWith("[");
        assertThat(result).endsWith("]");
    }

    @Test
    @DisplayName("SpEL 인라인 맵을 이용한 다국어(Multilingual) 구조 매핑 테스트")
    void testMultilingualMapCreation() {
        String payloadJson = "{\"data\": [{\"emp_id\":\"T1\", \"kor_name\":\"홍길동\", \"eng_name\":\"Hong\"}]}";
        String mappingConfigStr = "{" +
                "\"rootPath\": \"payload['data']\"," +
                "\"mappings\": [" +
                "  {\"targetField\": \"emp_name\", \"sourceExpression\": \"{'ko': #this['kor_name'], 'en': #this['eng_name']}\"}" +
                "]" +
                "}";

        String result = transformer.transformPayload(payloadJson, mappingConfigStr);
        assertThat(result).contains("\"emp_name\":{\"ko\":\"홍길동\",\"en\":\"Hong\"}");
    }
}
