package com.classification.domain_system.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TenantContextTest {

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("성공 - TenantContext에 organizationId 및 userId 설정 및 조회")
    void testSetAndGetTenantContext() {
        UUID orgId = UUID.randomUUID();
        String userId = "test-user-id";

        TenantContext.setOrganizationId(orgId);
        TenantContext.setUserId(userId);

        assertThat(TenantContext.getOrganizationId()).isEqualTo(orgId);
        assertThat(TenantContext.getUserId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("성공 - clear() 호출 시 ThreadLocal 정보 제거")
    void testClearTenantContext() {
        UUID orgId = UUID.randomUUID();
        TenantContext.setOrganizationId(orgId);
        TenantContext.setUserId("user-1");

        TenantContext.clear();

        assertThat(TenantContext.getOrganizationId()).isNull();
        assertThat(TenantContext.getUserId()).isNull();
    }
}
