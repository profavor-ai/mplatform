package com.classification.domain_system.event;

import org.springframework.context.ApplicationEvent;

import java.util.UUID;

public class MasterDataChangedEvent extends ApplicationEvent {

    private final UUID recordId;
    private final UUID nodeId;
    private final String changeType; // CREATE, UPDATE, DELETE
    private final String payloadJson;

    public MasterDataChangedEvent(Object source, UUID recordId, UUID nodeId, String changeType, String payloadJson) {
        super(source);
        this.recordId = recordId;
        this.nodeId = nodeId;
        this.changeType = changeType;
        this.payloadJson = payloadJson;
    }

    public UUID getRecordId() {
        return recordId;
    }

    public UUID getNodeId() {
        return nodeId;
    }

    public String getChangeType() {
        return changeType;
    }

    public String getPayloadJson() {
        return payloadJson;
    }
}
