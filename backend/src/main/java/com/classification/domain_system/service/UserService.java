package com.classification.domain_system.service;

import com.classification.domain_system.controller.UserController.UserDto;
import com.classification.domain_system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(u -> new UserDto(u.getId(), u.getUsername(), u.getRole(), u.getOrganizationId(), u.getTeamId(), u.getIsActive()))
                .collect(Collectors.toList());
    }

    @Transactional
    public com.classification.domain_system.entity.User updateUserInfo(String userId, com.classification.domain_system.entity.User updateReq) {
        com.classification.domain_system.entity.User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        if (updateReq.getRole() != null) user.setRole(updateReq.getRole());
        if (updateReq.getOrganizationId() != null) user.setOrganizationId(updateReq.getOrganizationId());
        if (updateReq.getTeamId() != null) user.setTeamId(updateReq.getTeamId());
        if (updateReq.getIsActive() != null) user.setIsActive(updateReq.getIsActive());
        return userRepository.save(user);
    }

    @Transactional
    public com.classification.domain_system.entity.User findByUsername(String username) {
        com.classification.domain_system.entity.User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        return user;
    }

    public java.util.List<com.classification.domain_system.entity.User> getAllUsersEntity() {
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<com.classification.domain_system.entity.User> searchUsers(String search, org.springframework.data.domain.Pageable pageable) {
        if (search == null || search.trim().isEmpty()) {
            return userRepository.findAll(pageable);
        }
        return userRepository.findByUsernameContainingIgnoreCase(search, pageable);
    }

    @Transactional
    public void updateTimezone(String username, String timezone) {
        com.classification.domain_system.entity.User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        user.setTimezone(timezone);
        userRepository.save(user);
    }

    @Transactional
    public void updateUserRole(String userId, String role) {
        com.classification.domain_system.entity.User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        user.setRole(role);
        userRepository.save(user);
    }
}
