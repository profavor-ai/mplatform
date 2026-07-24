package com.classification.domain_system.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RoleDataMigrationInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        try {
            log.info("Starting safety DB data migration: Updating legacy 'ADMIN' roles to 'ROLE_ADMIN'...");

            // 1. users 테이블의 role 'ADMIN' -> 'ROLE_ADMIN'
            int updatedUsers = jdbcTemplate.update(
                    "UPDATE users SET role = 'ROLE_ADMIN' WHERE role = 'ADMIN'"
            );
            if (updatedUsers > 0) {
                log.info("Migrated {} user(s) role from 'ADMIN' to 'ROLE_ADMIN'.", updatedUsers);
            }

            // 2. users 테이블의 role 'USER' -> 'ROLE_USER'
            int updatedUsersRoleUser = jdbcTemplate.update(
                    "UPDATE users SET role = 'ROLE_USER' WHERE role = 'USER'"
            );
            if (updatedUsersRoleUser > 0) {
                log.info("Migrated {} user(s) role from 'USER' to 'ROLE_USER'.", updatedUsersRoleUser);
            }

            // 3. role 테이블의 name 'ADMIN' -> 'ROLE_ADMIN' (중복 시 자식 role_permissions 삭제 후 role 삭제 및 업데이트)
            jdbcTemplate.update("DELETE FROM role_permissions WHERE role_id IN (SELECT id FROM role WHERE name = 'ADMIN' AND organization_id IN (SELECT organization_id FROM role WHERE name = 'ROLE_ADMIN'))");
            jdbcTemplate.update("DELETE FROM role WHERE name = 'ADMIN' AND organization_id IN (SELECT organization_id FROM role WHERE name = 'ROLE_ADMIN')");
            int updatedRoles = jdbcTemplate.update(
                    "UPDATE role SET name = 'ROLE_ADMIN' WHERE name = 'ADMIN'"
            );
            if (updatedRoles > 0) {
                log.info("Migrated {} role(s) name from 'ADMIN' to 'ROLE_ADMIN'.", updatedRoles);
            }

            // 4. menu 테이블의 required_role 'ADMIN' -> 'ROLE_ADMIN'
            int updatedMenus = jdbcTemplate.update(
                    "UPDATE menu SET required_role = 'ROLE_ADMIN' WHERE required_role = 'ADMIN'"
            );
            if (updatedMenus > 0) {
                log.info("Migrated {} menu(s) required_role from 'ADMIN' to 'ROLE_ADMIN'.", updatedMenus);
            }

            // 5. 기본 시스템 역할들의 is_system_role 및 display_name 보정
            jdbcTemplate.update("UPDATE role SET is_system_role = true WHERE name IN ('ADMIN', 'ROLE_ADMIN', 'ORG_ADMIN', 'DATA_STEWARD', 'DOMAIN_EDITOR', 'DQ_MANAGER', 'VIEWER', 'USER', 'ROLE_USER')");
            jdbcTemplate.update("UPDATE role SET display_name = '{\"ko\":\"시스템 관리자\",\"en\":\"System Admin\"}' WHERE name IN ('ADMIN', 'ROLE_ADMIN') AND (display_name IS NULL OR display_name = '')");
            jdbcTemplate.update("UPDATE role SET display_name = '{\"ko\":\"조직 관리자\",\"en\":\"Organization Admin\"}' WHERE name = 'ORG_ADMIN' AND (display_name IS NULL OR display_name = '')");

            log.info("Completed safety DB data migration for ROLE_ADMIN and ORG_ADMIN.");
        } catch (Exception e) {
            log.error("Error occurred during DB role data migration", e);
        }
    }
}
