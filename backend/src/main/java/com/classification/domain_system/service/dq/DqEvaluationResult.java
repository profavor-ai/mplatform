package com.classification.domain_system.service.dq;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Result of a DQ evaluation containing all violations found.
 */
@Getter
public class DqEvaluationResult {
    private final List<Violation> violations = new ArrayList<>();

    public boolean isValid() {
        return violations.stream().noneMatch(v -> "ERROR".equals(v.getSeverity()));
    }

    public boolean hasWarnings() {
        return violations.stream().anyMatch(v -> "WARNING".equals(v.getSeverity()));
    }

    public List<String> getErrorMessages() {
        return violations.stream()
                .filter(v -> "ERROR".equals(v.getSeverity()))
                .map(v -> v.getMessage() != null && !v.getMessage().isEmpty()
                        ? v.getMessage().getOrDefault("en", v.getMessage().values().iterator().next())
                        : "Validation failed")
                .toList();
    }

    public List<String> getWarningMessages() {
        return violations.stream()
                .filter(v -> "WARNING".equals(v.getSeverity()))
                .map(v -> v.getMessage() != null && !v.getMessage().isEmpty()
                        ? v.getMessage().getOrDefault("en", v.getMessage().values().iterator().next())
                        : "Validation warning")
                .toList();
    }

    public void addViolation(String fieldKey, String ruleType, String severity,
                             Map<String, String> message, String actualValue) {
        violations.add(new Violation(fieldKey, ruleType, severity, message, actualValue, null));
    }

    public void addViolation(String fieldKey, String ruleType, String severity,
                             Map<String, String> message, String actualValue, java.util.UUID ruleId) {
        violations.add(new Violation(fieldKey, ruleType, severity, message, actualValue, ruleId));
    }

    @Getter
    public static class Violation {
        private final String fieldKey;
        private final String ruleType;
        private final String severity;
        private final Map<String, String> message;
        private final String actualValue;
        private final java.util.UUID ruleId;

        public Violation(String fieldKey, String ruleType, String severity,
                         Map<String, String> message, String actualValue) {
            this(fieldKey, ruleType, severity, message, actualValue, null);
        }

        public Violation(String fieldKey, String ruleType, String severity,
                         Map<String, String> message, String actualValue, java.util.UUID ruleId) {
            this.fieldKey = fieldKey;
            this.ruleType = ruleType;
            this.severity = severity;
            this.message = message;
            this.actualValue = actualValue;
            this.ruleId = ruleId;
        }
    }
}
