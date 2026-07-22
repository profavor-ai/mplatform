package com.classification.domain_system.service.dq;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

/**
 * Context passed to each RuleEvaluator during evaluation.
 * Contains the full record data (for cross-field validation) and domain/node info.
 */
@Getter
public class EvaluationContext {
    private final UUID domainId;
    private final UUID nodeId;
    private final JsonNode fullRecord;
    private final UUID recordId;

    public EvaluationContext(UUID domainId, UUID nodeId, JsonNode fullRecord) {
        this(domainId, nodeId, fullRecord, null);
    }

    public EvaluationContext(UUID domainId, UUID nodeId, JsonNode fullRecord, UUID recordId) {
        this.domainId = domainId;
        this.nodeId = nodeId;
        this.fullRecord = fullRecord;
        this.recordId = recordId;
    }
}
