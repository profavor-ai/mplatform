package com.classification.domain_system.controller;

import com.classification.domain_system.entity.User;
import com.classification.domain_system.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import lombok.RequiredArgsConstructor;

import java.util.List;

import com.classification.domain_system.dto.AdminUserUpdateDto;
import com.classification.domain_system.dto.SelfUserUpdateDto;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasPermission(null, 'admin:read')")
    public ResponseEntity<List<UserDto>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<User> updateSelfUser(@RequestBody SelfUserUpdateDto updateReq) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User u = userService.updateSelfUserInfo(username, updateReq);
        return ResponseEntity.ok(u);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'admin:write')")
    public ResponseEntity<User> updateUser(@PathVariable String id, @RequestBody AdminUserUpdateDto updateReq) {
        User u = userService.updateAdminUserInfo(id, updateReq);
        return ResponseEntity.ok(u);
    }
    
    public static class UserDto {
        public String id;
        public String username;
        public String role;
        public java.util.UUID organizationId;
        public java.util.UUID departmentId;
        public java.util.UUID teamId;
        public Boolean isActive;
        
        public UserDto(String id, String username, String role) {
            this(id, username, role, null, null, null, true);
        }

        public UserDto(String id, String username, String role, java.util.UUID organizationId, java.util.UUID departmentId, java.util.UUID teamId, Boolean isActive) {
            this.id = id;
            this.username = username;
            this.role = role;
            this.organizationId = organizationId;
            this.departmentId = departmentId;
            this.teamId = teamId;
            this.isActive = isActive;
        }
    }

    @PostMapping("/timezone")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateTimezone(@RequestBody TimezoneRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        userService.updateTimezone(username, request.getTimezone());
        return ResponseEntity.ok().build();
    }

    public static class TimezoneRequest {
        private String timezone;
        public String getTimezone() { return timezone; }
        public void setTimezone(String timezone) { this.timezone = timezone; }
    }
}
