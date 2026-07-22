package com.classification.domain_system.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class DqEvaluationResponse {
    private boolean valid;
    private List<ViolationItem> errors;
    private List<ViolationItem> warnings;

    @Data
    public static class ViolationItem {
        private String fieldKey;
        private String ruleType;
        private String severity;
        private Map<String, String> message;
        private String actualValue;
    }
}
