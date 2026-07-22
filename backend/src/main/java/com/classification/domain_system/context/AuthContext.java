package com.classification.domain_system.context;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Component
@RequestScope
@Getter
@Setter
public class AuthContext {
    private UUID organizationId;
    private String userId;
    private Set<String> permissions = new HashSet<>();

    public boolean hasPermission(String permission) {
        if (permissions == null || permissions.isEmpty()) {
            return false;
        }
        if (permissions.contains("org:*") || permissions.contains("*:*") || permissions.contains(permission)) {
            return true;
        }
        if (permission.contains(":")) {
            String domainPrefix = permission.split(":")[0] + ":*";
            if (permissions.contains(domainPrefix)) {
                return true;
            }
        }
        return false;
    }
}
