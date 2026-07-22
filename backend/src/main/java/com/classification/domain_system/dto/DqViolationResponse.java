package com.classification.domain_system.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
public class DqViolationResponse {
    private UUID id;
    private UUID recordId;
    private String recordIdentifier;
    private Map<String, String> nodeName;
    private UUID dqRuleId;
    private String fieldKey;
    private String severity;
    private Map<String, String> message;
    private String actualValue;
    private LocalDateTime checkedAt;
    private Boolean resolved;
}
