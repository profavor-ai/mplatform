package com.classification.domain_system.controller;

import com.classification.domain_system.entity.User;
import com.classification.domain_system.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final com.classification.domain_system.repository.UserRepository userRepository;

    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        String serverOffset = OffsetDateTime.now().getOffset().getId();
        return ResponseEntity.ok(userRepository.findAll().stream()
            .map(user -> new LoginResponse(null, user.getUsername(), user.getRole(), user.getId(), user.getId(), user.getOrganizationId(), user.getDepartmentId(), user.getTimezone(), serverOffset))
            .toList());
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        try {
            String ip = httpRequest.getRemoteAddr();
            if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) ip = "127.0.0.1";
            String userAgent = httpRequest.getHeader("User-Agent");
            
            String token = authService.login(request.getUsername(), request.getPassword(), ip, userAgent);
            User user = authService.findByUsername(request.getUsername());
            
            String serverOffset = OffsetDateTime.now().getOffset().getId();
            return ResponseEntity.ok(new LoginResponse(
                token,
                user.getUsername(),
                user.getRole(),
                user.getId(),
                user.getId(),
                user.getOrganizationId(),
                user.getDepartmentId(),
                user.getTimezone(),
                serverOffset
            ));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }

    @GetMapping("/login-logs")
    public ResponseEntity<?> getLoginLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            org.springframework.security.core.Authentication authentication) {
        
        if (authentication == null || !authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ADMIN"))) {
            return ResponseEntity.status(403).body("Access denied. Admin role required.");
        }
        
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "loginAt"));
        return ResponseEntity.ok(authService.getLoginLogs(pageable));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            authService.register(request.getUsername(), request.getPassword(), request.getRole());
            return ResponseEntity.ok("User registered successfully");
        } catch (Exception e) {
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }

    @Data
    static class LoginRequest {
        private String username;
        private String password;
    }

    @Data
    static class RegisterRequest {
        private String username;
        private String password;
        private String role;
    }

    @Data
    static class LoginResponse {
        private final String token;
        private final String username;
        private final String role;
        private final String uuid;
        private final String id;
        private final UUID organizationId;
        private final UUID departmentId;
        private final String timezone;
        private final String serverOffset;
    }
}
