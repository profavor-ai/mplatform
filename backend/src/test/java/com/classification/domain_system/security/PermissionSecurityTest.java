package com.classification.domain_system.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PermissionSecurityTest {

    @Test
    @DisplayName("domain:read 권한만 있는 사용자 SecurityContext 설정 검증")
    void testDomainReadAuthority() {
        List<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_USER"),
                new SimpleGrantedAuthority("domain:read")
        );
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("user1", null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertTrue(SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("domain:read")));
        assertFalse(SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("domain:write")));

        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("domain:* 권한을 가진 사용자는 domain:read와 domain:write 모두 허용되어야 함")
    void testDomainWildcardAuthority() {
        List<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_USER"),
                new SimpleGrantedAuthority("domain:*"),
                new SimpleGrantedAuthority("domain:read"),
                new SimpleGrantedAuthority("domain:write")
        );
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("user1", null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertTrue(SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("domain:read")));
        assertTrue(SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("domain:write")));

        SecurityContextHolder.clearContext();
    }
}
