package com.classification.domain_system.context;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AuthContextTest {

    @Test
    @DisplayName("성공 - 정확한 권한 매칭 시 true 반환")
    void testExactPermissionMatch() {
        AuthContext authContext = new AuthContext();
        authContext.setPermissions(Set.of("domain:read", "domain:write"));

        assertThat(authContext.hasPermission("domain:read")).isTrue();
        assertThat(authContext.hasPermission("domain:write")).isTrue();
        assertThat(authContext.hasPermission("domain:delete")).isFalse();
    }

    @Test
    @DisplayName("성공 - org:* 또는 와일드카드 권한 보유 시 true 반환")
    void testWildcardPermissionMatch() {
        AuthContext authContext = new AuthContext();
        authContext.setPermissions(Set.of("domain:*"));

        assertThat(authContext.hasPermission("domain:read")).isTrue();
        assertThat(authContext.hasPermission("domain:write")).isTrue();
        assertThat(authContext.hasPermission("user:manage")).isFalse();

        authContext.setPermissions(Set.of("org:*"));
        assertThat(authContext.hasPermission("user:manage")).isTrue();
        assertThat(authContext.hasPermission("dq:write")).isTrue();
    }
}
