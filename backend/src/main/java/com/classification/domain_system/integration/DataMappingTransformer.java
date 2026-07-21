package com.classification.domain_system.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class DataMappingTransformer {

    private final ObjectMapper mapper = new ObjectMapper()
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .setDateFormat(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
    private final ExpressionParser parser = new SpelExpressionParser();

    public String transform(Message<?> message) {
        try {
            String payloadJson = (String) message.getPayload();
            String mappingConfigStr = message.getHeaders().get("MAPPING_CONFIG", String.class);
            
            if (mappingConfigStr == null || mappingConfigStr.isBlank()) {
                return payloadJson; // No mapping defined, return original
            }

            // Read source payload
            Map<String, Object> payload = mapper.readValue(payloadJson, new TypeReference<>() {});
            
            // Setup SpEL context
            Map<String, Object> rootContext = new HashMap<>();
            rootContext.put("payload", payload);
            StandardEvaluationContext context = new StandardEvaluationContext(rootContext);
            context.addPropertyAccessor(new org.springframework.context.expression.MapAccessor());

            // Read mapping config
            JsonNode mappingConfig = mapper.readTree(mappingConfigStr);
            JsonNode mappings = mappingConfig.get("mappings");

            if (mappings == null || !mappings.isArray() || mappings.isEmpty()) {
                return payloadJson;
            }

            Map<String, Object> targetPayload = new HashMap<>();
            
            for (JsonNode mapping : mappings) {
                String targetField = mapping.get("targetField").asText();
                String sourceExpression = mapping.get("sourceExpression").asText();
                
                try {
                    Expression exp = parser.parseExpression(sourceExpression);
                    Object value = exp.getValue(context);
                    targetPayload.put(targetField, value);
                } catch (Exception e) {
                    System.err.println("Error evaluating expression '" + sourceExpression + "': " + e.getMessage());
                    // Put null or ignore? Let's put null or error string for visibility
                    targetPayload.put(targetField, null);
                }
            }

            return mapper.writeValueAsString(targetPayload);
            
        } catch (Exception e) {
            e.printStackTrace();
            return (String) message.getPayload(); // fallback to original on fatal error
        }
    }
}
