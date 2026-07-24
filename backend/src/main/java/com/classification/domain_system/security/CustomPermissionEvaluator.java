package com.classification.domain_system.security;

import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@Component
public class CustomPermissionEvaluator implements PermissionEvaluator {

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        if (authentication == null || !authentication.isAuthenticated() || permission == null) {
            return false;
        }

        String permissionString = permission.toString().trim();
        String resource;
        String action;

        if (targetDomainObject != null && !targetDomainObject.toString().isBlank()) {
            resource = targetDomainObject.toString().trim().toLowerCase();
            action = permissionString.toLowerCase();
        } else {
            // targetDomainObject가 없으면 permissionString 자체가 "resource:action" 형태인지 확인
            if (permissionString.contains(":")) {
                String[] parts = permissionString.split(":", 2);
                resource = parts[0].trim().toLowerCase();
                action = parts[1].trim().toLowerCase();
            } else {
                return checkAuthority(authentication, permissionString);
            }
        }

        return checkResourceActionPermission(authentication, resource, action);
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        if (authentication == null || !authentication.isAuthenticated() || permission == null) {
            return false;
        }
        String resource = targetType != null ? targetType.trim().toLowerCase() : "";
        String action = permission.toString().trim().toLowerCase();
        return checkResourceActionPermission(authentication, resource, action);
    }

    private boolean checkResourceActionPermission(Authentication authentication, String resource, String action) {
        String requiredPermission = resource + ":" + action;
        String wildcardResourcePermission = resource + ":*";

        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String auth = authority.getAuthority().trim();

            // 1. 전역 슈퍼 관리자 권한 (*:*, *, ROLE_ADMIN)
            if ("*:*".equalsIgnoreCase(auth) || "*".equals(auth) || "ROLE_ADMIN".equalsIgnoreCase(auth)) {
                return true;
            }

            // 2. 리소스 단위 와일드카드 (예: record:*)
            if (wildcardResourcePermission.equalsIgnoreCase(auth)) {
                return true;
            }

            // 3. 정확한 리소스:액션 일치 (예: record:write)
            if (requiredPermission.equalsIgnoreCase(auth)) {
                return true;
            }
        }

        return false;
    }

    private boolean checkAuthority(Authentication authentication, String rawPermission) {
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String auth = authority.getAuthority().trim();
            if ("*:*".equalsIgnoreCase(auth) || "*".equals(auth) || "ROLE_ADMIN".equalsIgnoreCase(auth)) {
                return true;
            }
            if (rawPermission.equalsIgnoreCase(auth)) {
                return true;
            }
        }
        return false;
    }
}
