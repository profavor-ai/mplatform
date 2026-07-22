package com.classification.domain_system.dto;

import lombok.Data;
import java.util.Map;
import java.util.UUID;

@Data
public class DqRuleRequest {
    private UUID fieldDefinitionId;
    private UUID domainId;
    private UUID nodeId;
    private String ruleType;
    private String severity;
    private String params;
    private Map<String, String> message;
    private Integer sortOrder;
    private Boolean isActive;
}
