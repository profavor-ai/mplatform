package com.classification.domain_system.service;

import com.classification.domain_system.entity.Role;
import com.classification.domain_system.entity.User;
import com.classification.domain_system.entity.UserRole;
import com.classification.domain_system.repository.RoleRepository;
import com.classification.domain_system.repository.UserRepository;
import com.classification.domain_system.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PermissionService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;

    @Transactional(readOnly = true)
    public Collection<GrantedAuthority> getAuthoritiesForUser(String username, String roleStr) {
        Set<String> authorities = new HashSet<>();

        if (roleStr != null && !roleStr.isBlank()) {
            for (String r : roleStr.split(",")) {
                String trimmed = r.trim();
                if (!trimmed.isEmpty()) {
                    String roleAuthority = trimmed.startsWith("ROLE_") ? trimmed : "ROLE_" + trimmed;
                    authorities.add(roleAuthority);
                }
            }
        }

        boolean isAdmin = authorities.contains("ROLE_ADMIN");

        if (isAdmin) {
            authorities.addAll(List.of(
                    "domain:*", "domain:read", "domain:write",
                    "node:*", "node:read", "node:write",
                    "field:*", "field:read", "field:write",
                    "dq:*", "dq:read", "dq:write"
            ));
        }

        if (username != null && !username.isBlank()) {
            try {
                Optional<User> userOpt = userRepository.findByUsername(username);
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    List<UserRole> userRoles = userRoleRepository.findByUserId(user.getId());
                    if (userRoles != null && !userRoles.isEmpty()) {
                        Set<UUID> roleIds = userRoles.stream()
                                .map(UserRole::getRoleId)
                                .collect(Collectors.toSet());

                        List<Role> roles = roleRepository.findAllById(roleIds);
                        for (Role role : roles) {
                            if (role.getPermissions() != null) {
                                for (String perm : role.getPermissions()) {
                                    if (perm != null && !perm.isBlank()) {
                                        authorities.add(perm.trim());
                                        expandWildcardPermission(perm.trim(), authorities);
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Failed to load fine-grained permissions for user: {}", username, e);
            }
        }

        if (authorities.isEmpty()) {
            authorities.add("ROLE_USER");
        }

        return authorities.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    private void expandWildcardPermission(String permission, Set<String> targetSet) {
        if ("domain:*".equalsIgnoreCase(permission)) {
            targetSet.add("domain:read");
            targetSet.add("domain:write");
        } else if ("node:*".equalsIgnoreCase(permission)) {
            targetSet.add("node:read");
            targetSet.add("node:write");
        } else if ("field:*".equalsIgnoreCase(permission)) {
            targetSet.add("field:read");
            targetSet.add("field:write");
        } else if ("dq:*".equalsIgnoreCase(permission)) {
            targetSet.add("dq:read");
            targetSet.add("dq:write");
        } else if ("*:*".equals(permission)) {
            targetSet.addAll(List.of(
                    "domain:*", "domain:read", "domain:write",
                    "node:*", "node:read", "node:write",
                    "field:*", "field:read", "field:write",
                    "dq:*", "dq:read", "dq:write"
            ));
        }
    }
}
