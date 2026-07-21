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
        if (menuRepository.count() == 0) {
            Menu dashboard = Menu.builder().name("Dashboard").path("/").icon("dashboard").sortOrder(1).build();
            Menu schema = Menu.builder().name("Schema").path("/schema").icon("schema").sortOrder(2).build();
            Menu records = Menu.builder().name("Records").path("/records").icon("table_rows").sortOrder(3).build();
            Menu approvals = Menu.builder().name("Approvals").path("/approvals").icon("check_circle").sortOrder(4).build();
            
            // Parent: Admin
            Menu admin = Menu.builder().name("Admin").path("/admin").icon("admin_panel_settings").sortOrder(5).requiredRole("ADMIN").build();
            
            menuRepository.save(dashboard);
            menuRepository.save(schema);
            menuRepository.save(records);
            menuRepository.save(approvals);
            Menu savedAdmin = menuRepository.save(admin);
            
            // Children of Admin
            Menu adminMonitor = Menu.builder().name("Admin Monitor").path("/admin").icon("monitor").parentId(savedAdmin.getId()).sortOrder(1).requiredRole("ADMIN").build();
            Menu userManagement = Menu.builder().name("User Management").path("/admin/users").icon("manage_accounts").parentId(savedAdmin.getId()).sortOrder(2).requiredRole("ADMIN").build();
            Menu menuManagement = Menu.builder().name("Menu Management").path("/admin/menus").icon("menu_open").parentId(savedAdmin.getId()).sortOrder(3).requiredRole("ADMIN").build();
            Menu menuLogs = Menu.builder().name("System Logs").path("/admin/system-logs").icon("history").parentId(savedAdmin.getId()).sortOrder(4).requiredRole("ADMIN").build();

            menuRepository.save(adminMonitor);
            menuRepository.save(userManagement);
            menuRepository.save(menuManagement);
            menuRepository.save(menuLogs);
        }
        
        // Data Recovery: Set isActive=true for any menus where it got accidentally set to false/null
        menuRepository.findAll().forEach(menu -> {
            if (menu.getIsActive() == null || !menu.getIsActive()) {
                menu.setIsActive(true);
                menuRepository.save(menu);
            }
            if (menu.getName().equals("Access Logs") || menu.getName().equals("System Logs")) {
                Menu adminMenu = menuRepository.findAll().stream().filter(m -> "Admin".equals(m.getName())).findFirst().orElse(null);
                if (adminMenu != null) menu.setParentId(adminMenu.getId());
                menu.setName("System Logs");
                menu.setPath("/admin/system-logs");
                menuRepository.save(menu);
            }
        });

        Menu adminMenu = menuRepository.findAll().stream().filter(m -> "Admin".equals(m.getName())).findFirst().orElse(null);
        if (adminMenu != null) {
            boolean channelsExist = menuRepository.findAll().stream().anyMatch(m -> "/admin/integration/channels".equals(m.getPath()));
            if (!channelsExist) {
                menuRepository.save(Menu.builder().name("Integration Channels").path("/admin/integration/channels").icon("hub").parentId(adminMenu.getId()).sortOrder(5).requiredRole("ADMIN").build());
            }
        }
    }
}
