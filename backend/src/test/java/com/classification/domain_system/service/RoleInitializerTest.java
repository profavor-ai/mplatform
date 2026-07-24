package com.classification.domain_system.service;

import com.classification.domain_system.entity.Role;
import com.classification.domain_system.repository.RoleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class RoleInitializerTest {

    @Test
    @DisplayName("성공 - 조직 ID 전달 시 5개 기본 시스템 역할 자동 생성")
    void testCreateDefaultRolesForOrg() {
        RoleRepository roleRepository = mock(RoleRepository.class);
        RoleInitializer roleInitializer = new RoleInitializer(roleRepository);

        UUID orgId = UUID.randomUUID();
        given(roleRepository.existsByOrganizationIdAndName(eq(orgId), any())).willReturn(false);

        roleInitializer.createDefaultRolesForOrg(orgId);

        verify(roleRepository, times(7)).save(any(Role.class));
    }
}
