package com.classification.domain_system.controller;

import com.classification.domain_system.dto.DqRuleRequest;
import com.classification.domain_system.repository.DqRuleRepository;
import com.classification.domain_system.repository.FieldDefinitionRepository;
import com.classification.domain_system.service.dq.DqRuleEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class DqRuleControllerSecurityTest {

    private DqRuleController controller;
    private DqRuleRepository dqRuleRepository;
    private FieldDefinitionRepository fieldDefinitionRepository;
    private DqRuleEngine dqRuleEngine;

    @BeforeEach
    void setUp() {
        dqRuleRepository = mock(DqRuleRepository.class);
        fieldDefinitionRepository = mock(FieldDefinitionRepository.class);
        dqRuleEngine = mock(DqRuleEngine.class);

        controller = new DqRuleController(dqRuleRepository, fieldDefinitionRepository, dqRuleEngine);
    }

    private void setSecurityContext(String role) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "user", null, List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("일반 USER 권한으로 규칙 생성 시 AccessDeniedException 발생")
    void createRule_AsUser_ShouldThrowAccessDenied() {
        setSecurityContext("USER");
        DqRuleRequest req = new DqRuleRequest();
        req.setRuleType("NOT_NULL");

        assertThatThrownBy(() -> controller.createRule(UUID.randomUUID(), req))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Admin role required");
    }

    @Test
    @DisplayName("일반 USER 권한으로 규칙 수정 시 AccessDeniedException 발생")
    void updateRule_AsUser_ShouldThrowAccessDenied() {
        setSecurityContext("USER");
        DqRuleRequest req = new DqRuleRequest();

        assertThatThrownBy(() -> controller.updateRule(UUID.randomUUID(), req))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Admin role required");
    }

    @Test
    @DisplayName("일반 USER 권한으로 규칙 삭제 시 AccessDeniedException 발생")
    void deleteRule_AsUser_ShouldThrowAccessDenied() {
        setSecurityContext("USER");

        assertThatThrownBy(() -> controller.deleteRule(UUID.randomUUID()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Admin role required");
    }
}
