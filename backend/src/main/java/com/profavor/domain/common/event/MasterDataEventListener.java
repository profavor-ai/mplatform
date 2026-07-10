package com.profavor.domain.common.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class MasterDataEventListener {

    @Async
    @EventListener
    public void handleMasterDataChangedEvent(MasterDataChangedEvent event) {
        log.info("Received Master Data Event: Domain={}, EntityID={}, Tenant={}, Action={}",
                event.getDomainType(), event.getEntityId(), event.getTenantId(), event.getAction());
        
        // TODO: Trigger Matching Engine, Data Quality Checks, or export to external systems via Kafka
    }
}
