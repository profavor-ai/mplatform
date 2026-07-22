package com.classification.domain_system.service;

import com.classification.domain_system.entity.Role;
import com.classification.domain_system.repository.RoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
public class RoleInitializer {

    private final RoleRepository roleRepository;

    public RoleInitializer(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Transactional
    public void createDefaultRolesForOrg(UUID orgId) {
        if (orgId == null) return;

        createSystemRole(orgId, "ORG_ADMIN", "Organization Admin", "Full control over organization resources",
                Set.of("org:*", "domain:*", "node:*", "field:*", "dq:*", "user:*", "role:*"));

        createSystemRole(orgId, "DATA_STEWARD", "Data Steward", "Data quality and model management",
                Set.of("domain:read", "domain:write", "node:*", "field:*", "dq:read", "dq:write"));

        createSystemRole(orgId, "DOMAIN_EDITOR", "Domain Editor", "Create and edit domains and fields",
                Set.of("domain:read", "node:read", "field:read", "node:write", "field:write"));

        createSystemRole(orgId, "DQ_MANAGER", "DQ Manager", "Manage DQ rules and scan results",
                Set.of("dq:read", "dq:write", "dq_rule:*", "dq_scan:*"));

        createSystemRole(orgId, "VIEWER", "Viewer", "Read-only access to domains, nodes, fields and DQ",
                Set.of("domain:read", "node:read", "field:read", "dq:read"));
    }

    private void createSystemRole(UUID orgId, String name, String displayName, String description, Set<String> permissions) {
        if (!roleRepository.existsByOrganizationIdAndName(orgId, name)) {
            Role role = new Role();
            role.setOrganizationId(orgId);
            role.setName(name);
            role.setDisplayName(displayName);
            role.setDescription(description);
            role.setPermissions(permissions);
            role.setIsSystemRole(true);
            roleRepository.save(role);
        }
    }
}
