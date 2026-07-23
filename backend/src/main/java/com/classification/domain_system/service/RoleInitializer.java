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

        createSystemRole(orgId, "ADMIN", "시스템 관리자 (System Admin)", "Full system administration access",
                Set.of("*"));

        createSystemRole(orgId, "ORG_ADMIN", "조직 관리자 (Org Admin)", "Full control over organization resources",
                Set.of("org:*", "domain:*", "node:*", "field:*", "dq:*", "user:*", "role:*"));

        createSystemRole(orgId, "DATA_STEWARD", "데이터 스튜어드 (Data Steward)", "Data quality and model management",
                Set.of("domain:read", "domain:write", "node:*", "field:*", "dq:read", "dq:write"));

        createSystemRole(orgId, "DOMAIN_EDITOR", "도메인 편집자 (Domain Editor)", "Create and edit domains and fields",
                Set.of("domain:read", "node:read", "field:read", "node:write", "field:write"));

        createSystemRole(orgId, "DQ_MANAGER", "데이터 품질 관리자 (DQ Manager)", "Manage DQ rules and scan results",
                Set.of("dq:read", "dq:write", "dq_rule:*", "dq_scan:*"));

        createSystemRole(orgId, "VIEWER", "조회자 (Viewer)", "Read-only access to domains, nodes, fields and DQ",
                Set.of("domain:read", "node:read", "field:read", "dq:read"));

        createSystemRole(orgId, "USER", "일반 사용자 (General User)", "Basic user access",
                Set.of("domain:read", "node:read"));
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
