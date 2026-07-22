package com.classification.domain_system.config;

import com.classification.domain_system.entity.Menu;
import com.classification.domain_system.repository.MenuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MenuDataInitializer implements CommandLineRunner {

    private final MenuRepository menuRepository;

    @Override
    public void run(String... args) throws Exception {
        // 1. 최초 초기화: DB에 메뉴가 아예 없을 때만 기본 시드 메뉴 생성
        if (menuRepository.count() == 0) {
            Menu dashboard = Menu.builder().name("Dashboard").path("/").icon("dashboard").sortOrder(1).isActive(true).build();
            Menu schema = Menu.builder().name("Schema").path("/schema").icon("schema").sortOrder(2).isActive(true).build();
            Menu records = Menu.builder().name("Records").path("/records").icon("table_rows").sortOrder(3).isActive(true).build();
            Menu approvals = Menu.builder().name("Approvals").path("/approvals").icon("check_circle").sortOrder(4).isActive(true).build();
            Menu dqDashboard = Menu.builder().name("DQ Dashboard").path("/dq-dashboard").icon("analytics").sortOrder(5).isActive(true).build();
            
            // Parent: Admin
            Menu admin = Menu.builder().name("Admin").path("/admin").icon("admin_panel_settings").sortOrder(6).requiredRole("ADMIN").isActive(true).build();
            
            menuRepository.save(dashboard);
            menuRepository.save(schema);
            menuRepository.save(records);
            menuRepository.save(approvals);
            menuRepository.save(dqDashboard);
            Menu savedAdmin = menuRepository.save(admin);
            
            // Children of Admin
            Menu adminMonitor = Menu.builder().name("Admin Monitor").path("/admin").icon("monitor").parentId(savedAdmin.getId()).sortOrder(1).requiredRole("ADMIN").isActive(true).build();
            Menu userManagement = Menu.builder().name("User Management").path("/admin/users").icon("manage_accounts").parentId(savedAdmin.getId()).sortOrder(2).requiredRole("ADMIN").isActive(true).build();
            Menu menuManagement = Menu.builder().name("Menu Management").path("/admin/menus").icon("menu_open").parentId(savedAdmin.getId()).sortOrder(3).requiredRole("ADMIN").isActive(true).build();
            Menu menuLogs = Menu.builder().name("System Logs").path("/admin/system-logs").icon("history").parentId(savedAdmin.getId()).sortOrder(4).requiredRole("ADMIN").isActive(true).build();
            Menu channels = Menu.builder().name("Integration Channels").path("/admin/integration/channels").icon("hub").parentId(savedAdmin.getId()).sortOrder(5).requiredRole("ADMIN").isActive(true).build();

            menuRepository.save(adminMonitor);
            menuRepository.save(userManagement);
            menuRepository.save(menuManagement);
            menuRepository.save(menuLogs);
            menuRepository.save(channels);
            return;
        }

        // 2. 증분 안전 체크: 기존 관리자가 변경한 메뉴 설정(비활성화, 권한 변경 등)은 절대 덮어쓰지 않고,
        // 누락된 신규 시스템 메뉴 경로만 안전하게 1회 보충 등록함.
        boolean dqExist = menuRepository.findAll().stream().anyMatch(m -> "/dq-dashboard".equals(m.getPath()));
        if (!dqExist) {
            menuRepository.save(Menu.builder()
                    .name("DQ Dashboard")
                    .path("/dq-dashboard")
                    .icon("analytics")
                    .sortOrder(5)
                    .isActive(true)
                    .build());
        }

        Menu adminMenu = menuRepository.findAll().stream().filter(m -> "Admin".equals(m.getName())).findFirst().orElse(null);
        if (adminMenu != null) {
            boolean orgExist = menuRepository.findAll().stream().anyMatch(m -> "/admin/organizations".equals(m.getPath()));
            if (!orgExist) {
                menuRepository.save(Menu.builder()
                        .name("Organization Management")
                        .path("/admin/organizations")
                        .icon("corporate_fare")
                        .parentId(adminMenu.getId())
                        .sortOrder(1)
                        .requiredRole("ADMIN")
                        .isActive(true)
                        .build());
            }

            boolean channelsExist = menuRepository.findAll().stream().anyMatch(m -> "/admin/integration/channels".equals(m.getPath()));
            if (!channelsExist) {
                menuRepository.save(Menu.builder()
                        .name("Integration Channels")
                        .path("/admin/integration/channels")
                        .icon("hub")
                        .parentId(adminMenu.getId())
                        .sortOrder(6)
                        .requiredRole("ADMIN")
                        .isActive(true)
                        .build());
            }
        }
    }
}
