package com.classification.domain_system.security;

import com.classification.domain_system.entity.Role;
import com.classification.domain_system.entity.User;
import com.classification.domain_system.entity.UserRole;
import com.classification.domain_system.repository.RoleRepository;
import com.classification.domain_system.repository.UserRepository;
import com.classification.domain_system.repository.UserRoleRepository;
import com.classification.domain_system.service.PermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.core.GrantedAuthority;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class PermissionServiceTest {

    private UserRepository userRepository;
    private UserRoleRepository userRoleRepository;
    private RoleRepository roleRepository;
    private PermissionService permissionService;

    @BeforeEach
    void setUp() {
        userRepository = Mockito.mock(UserRepository.class);
        userRoleRepository = Mockito.mock(UserRoleRepository.class);
        roleRepository = Mockito.mock(RoleRepository.class);
        permissionService = new PermissionService(userRepository, userRoleRepository, roleRepository);
    }

    @Test
    @DisplayName("domain:* 와일드카드 권한 소지 시 domain:read, domain:write 권한 자동 확장 검증")
    void testWildcardPermissionExpansion() {
        String username = "manager";
        User user = new User();
        user.setId(UUID.randomUUID().toString());
        user.setUsername(username);
        user.setRole("USER");

        Role role = new Role();
        role.setId(UUID.randomUUID());
        role.setName("DOMAIN_MANAGER");
        role.setPermissions(Set.of("domain:*", "node:read"));

        UserRole userRole = new UserRole();
        userRole.setUserId(user.getId());
        userRole.setRoleId(role.getId());

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(userRoleRepository.findByUserId(user.getId())).thenReturn(List.of(userRole));
        when(roleRepository.findAllById(Set.of(role.getId()))).thenReturn(List.of(role));

        Collection<GrantedAuthority> authorities = permissionService.getAuthoritiesForUser(username, user.getRole());
        Set<String> authStrings = new HashSet<>();
        authorities.forEach(a -> authStrings.add(a.getAuthority()));

        assertTrue(authStrings.contains("domain:*"));
        assertTrue(authStrings.contains("domain:read"));
        assertTrue(authStrings.contains("domain:write"));
        assertTrue(authStrings.contains("node:read"));
        assertFalse(authStrings.contains("node:write"));
    }

    @Test
    @DisplayName("ADMIN 역할 보유 시 전역 세부 권한 자동 부여 검증")
    void testAdminRolePermissions() {
        String username = "adminUser";
        User user = new User();
        user.setId(UUID.randomUUID().toString());
        user.setUsername(username);
        user.setRole("ADMIN");

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(userRoleRepository.findByUserId(user.getId())).thenReturn(Collections.emptyList());

        Collection<GrantedAuthority> authorities = permissionService.getAuthoritiesForUser(username, "ADMIN");
        Set<String> authStrings = new HashSet<>();
        authorities.forEach(a -> authStrings.add(a.getAuthority()));

        assertTrue(authStrings.contains("ROLE_ADMIN"));
        assertTrue(authStrings.contains("domain:*"));
        assertTrue(authStrings.contains("domain:read"));
        assertTrue(authStrings.contains("domain:write"));
        assertTrue(authStrings.contains("node:*"));
        assertTrue(authStrings.contains("field:*"));
        assertTrue(authStrings.contains("dq:*"));
    }
}
