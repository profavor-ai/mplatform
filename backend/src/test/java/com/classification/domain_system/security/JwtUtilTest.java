package com.classification.domain_system.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private final String testSecret = "test_secret_key_for_jwt_which_must_be_at_least_256_bits_long_for_hs256";

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", testSecret);
        ReflectionTestUtils.setField(jwtUtil, "accessTokenExpirationSec", 1800L);
        jwtUtil.init();
    }

    @Test
    @DisplayName("JWT 토큰 생성 및 파싱 검증")
    void generateAndValidateToken() {
        String username = "testuser";
        String role = "USER";
        String uuid = "uuid-1234";

        String token = jwtUtil.generateToken(username, role, uuid);

        assertNotNull(token);
        assertTrue(jwtUtil.isTokenValid(token));
        assertEquals(username, jwtUtil.extractUsername(token));

        Claims claims = jwtUtil.extractAllClaims(token);
        assertEquals(role, claims.get("role"));
        assertEquals(uuid, claims.get("uuid"));
    }

    @Test
    @DisplayName("잘못된 토큰 검증 시 false 반환")
    void invalidTokenReturnsFalse() {
        assertFalse(jwtUtil.isTokenValid("invalid.jwt.token"));
    }
}
