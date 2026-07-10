package com.profavor.domain.common.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MasterDataEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public void publishEvent(String domainType, Long entityId, String tenantId, String action) {
        MasterDataChangedEvent event = new MasterDataChangedEvent(this, domainType, entityId, tenantId, action);
        applicationEventPublisher.publishEvent(event);
    }
}
