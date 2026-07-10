package com.profavor.domain.common.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class MasterDataChangedEvent extends ApplicationEvent {
    
    private final String domainType;
    private final Long entityId;
    private final String tenantId;
    private final String action; // CREATE, UPDATE, DELETE

    public MasterDataChangedEvent(Object source, String domainType, Long entityId, String tenantId, String action) {
        super(source);
        this.domainType = domainType;
        this.entityId = entityId;
        this.tenantId = tenantId;
        this.action = action;
    }
}
