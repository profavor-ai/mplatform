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

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<UserDto>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @org.springframework.web.bind.annotation.PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@org.springframework.web.bind.annotation.PathVariable String id, @RequestBody User updateReq) {
        User u = userService.updateUserInfo(id, updateReq);
        return ResponseEntity.ok(u);
    }
    
    public static class UserDto {
        public String id;
        public String username;
        public String role;
        public java.util.UUID organizationId;
        public java.util.UUID teamId;
        public Boolean isActive;
        
        public UserDto(String id, String username, String role) {
            this(id, username, role, null, null, true);
        }

        public UserDto(String id, String username, String role, java.util.UUID organizationId, java.util.UUID teamId, Boolean isActive) {
            this.id = id;
            this.username = username;
            this.role = role;
            this.organizationId = organizationId;
            this.teamId = teamId;
            this.isActive = isActive;
        }
    }

    @org.springframework.web.bind.annotation.PostMapping("/timezone")
    public ResponseEntity<?> updateTimezone(@RequestBody TimezoneRequest request) {
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        userService.updateTimezone(username, request.getTimezone());
        return ResponseEntity.ok().build();
    }

    public static class TimezoneRequest {
        private String timezone;
        public String getTimezone() { return timezone; }
        public void setTimezone(String timezone) { this.timezone = timezone; }
    }
}
