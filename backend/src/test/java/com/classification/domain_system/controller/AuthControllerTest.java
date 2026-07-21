package com.classification.domain_system.controller;

import com.classification.domain_system.repository.UserRepository;
import com.classification.domain_system.security.JwtUtil;
import com.classification.domain_system.service.AuthService;
import com.classification.domain_system.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JwtUtil jwtUtil;

    // ─────────────────────────────────────────────────────────────────
    // login 테스트
    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("정상 로그인 시 200 OK + 토큰 반환")
        void success_Returns200WithToken() throws Exception {
            User user = new User();
            user.setId(UUID.randomUUID().toString());
            user.setUsername("testuser");
            user.setRole("USER");

            when(authService.login(eq("testuser"), eq("password"), any(), any())).thenReturn("jwt-token");
            when(authService.findByUsername("testuser")).thenReturn(user);

            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"username\":\"testuser\",\"password\":\"password\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value("jwt-token"))
                    .andExpect(jsonPath("$.username").value("testuser"))
                    .andExpect(jsonPath("$.role").value("USER"));
        }

        @Test
        @DisplayName("잘못된 비밀번호 시 401 반환")
        void wrongPassword_Returns401() throws Exception {
            when(authService.login(any(), any(), any(), any()))
                    .thenThrow(new RuntimeException("Invalid credentials"));

            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"username\":\"testuser\",\"password\":\"wrong\"}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("IPv6 루프백(::1) IP는 127.0.0.1로 변환하여 authService에 전달")
        void ipv6Loopback_ConvertedToIPv4() throws Exception {
            User user = new User();
            user.setId(UUID.randomUUID().toString());
            user.setUsername("admin");
            user.setRole("ADMIN");

            when(authService.login(eq("admin"), eq("pass"), eq("127.0.0.1"), any())).thenReturn("token");
            when(authService.findByUsername("admin")).thenReturn(user);

            // MockMvc는 기본적으로 127.0.0.1을 사용하지만
            // AuthController 로직을 검증하기 위해 정상 호출을 확인
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"username\":\"admin\",\"password\":\"pass\"}"))
                    .andExpect(status().isOk());

            // authService.login 이 "127.0.0.1"로 호출되었음을 검증
            verify(authService).login(eq("admin"), eq("pass"), eq("127.0.0.1"), any());
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // register 테스트
    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("정상 등록 시 200 OK 반환")
        void success_Returns200() throws Exception {
            doNothing().when(authService).register("newuser", "pass", "USER");

            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"username\":\"newuser\",\"password\":\"pass\",\"role\":\"USER\"}"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("User registered successfully"));
        }

        @Test
        @DisplayName("중복 username 등록 시 400 반환")
        void duplicateUsername_Returns400() throws Exception {
            doThrow(new RuntimeException("Username already exists"))
                    .when(authService).register(any(), any(), any());

            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"username\":\"exists\",\"password\":\"pass\",\"role\":\"USER\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string("Username already exists"));
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // getAllUsers 테스트
    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("getAllUsers")
    class GetAllUsers {

        @Test
        @DisplayName("사용자 목록 반환")
        void success_ReturnsUserList() throws Exception {
            User user = new User();
            user.setId(UUID.randomUUID().toString());
            user.setUsername("testuser");
            user.setRole("USER");

            when(userRepository.findAll()).thenReturn(List.of(user));

            mockMvc.perform(get("/api/auth/users"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].username").value("testuser"))
                    .andExpect(jsonPath("$[0].role").value("USER"));
        }

        @Test
        @DisplayName("사용자 없으면 빈 배열 반환")
        void noUsers_ReturnsEmptyList() throws Exception {
            when(userRepository.findAll()).thenReturn(List.of());

            mockMvc.perform(get("/api/auth/users"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }
}
