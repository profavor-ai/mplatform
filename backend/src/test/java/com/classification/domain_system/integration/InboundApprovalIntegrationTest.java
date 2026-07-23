package com.classification.domain_system.integration;

import com.classification.domain_system.entity.IntegrationChannel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InboundApprovalIntegrationTest {

    @Test
    @DisplayName("IntegrationChannel에 requiresApproval 기본값은 false이다")
    void testIntegrationChannelRequiresApprovalDefault() {
        IntegrationChannel channel = new IntegrationChannel();
        assertFalse(channel.isRequiresApproval());
    }

    @Test
    @DisplayName("IntegrationChannel에 requiresApproval 설정 및 조회가 가능하다")
    void testIntegrationChannelRequiresApprovalSet() {
        IntegrationChannel channel = new IntegrationChannel();
        channel.setRequiresApproval(true);
        assertTrue(channel.isRequiresApproval());
    }
}
