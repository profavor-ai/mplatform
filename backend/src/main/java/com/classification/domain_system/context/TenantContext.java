package com.classification.domain_system.context;

import java.util.UUID;

public class TenantContext {
    private static final ThreadLocal<UUID> organizationId = new ThreadLocal<>();
    private static final ThreadLocal<String> userId = new ThreadLocal<>();

    public static void setOrganizationId(UUID orgId) {
        organizationId.set(orgId);
    }

    public static UUID getOrganizationId() {
        return organizationId.get();
    }

    public static void setUserId(String uid) {
        userId.set(uid);
    }

    public static String getUserId() {
        return userId.get();
    }

    public static void clear() {
        organizationId.remove();
        userId.remove();
    }
}
