package com.classification.domain_system.service;

import com.classification.domain_system.entity.Menu;
import com.classification.domain_system.entity.MenuAccessLog;
import com.classification.domain_system.repository.MenuAccessLogRepository;
import com.classification.domain_system.repository.MenuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MenuService {

    private final MenuRepository menuRepository;
    private final MenuAccessLogRepository menuAccessLogRepository;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMenuTree() {
        List<Menu> allMenus = menuRepository.findAllByIsActiveTrueOrderBySortOrderAsc();
        return buildTree(allMenus, null);
    }

    private List<Map<String, Object>> buildTree(List<Menu> allMenus, Long parentId) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Menu menu : allMenus) {
            if (Objects.equals(menu.getParentId(), parentId)) {
                Map<String, Object> node = new HashMap<>();
                node.put("id", menu.getId());
                node.put("name", menu.getName());
                node.put("path", menu.getPath());
                node.put("icon", menu.getIcon());
                node.put("sortOrder", menu.getSortOrder());
                node.put("requiredRole", menu.getRequiredRole());
                node.put("parentId", menu.getParentId());
                node.put("isActive", menu.getIsActive());
                
                List<Map<String, Object>> children = buildTree(allMenus, menu.getId());
                if (!children.isEmpty()) {
                    node.put("children", children);
                }
                result.add(node);
            }
        }
        return result;
    }

    @Transactional
    public Menu createMenu(Menu menu) {
        return menuRepository.save(menu);
    }

    @Transactional
    public Menu updateMenu(Long id, Menu menuDetails) {
        Menu menu = menuRepository.findById(id).orElseThrow(() -> new RuntimeException("Menu not found"));
        if (menuDetails.getName() != null) menu.setName(menuDetails.getName());
        if (menuDetails.getPath() != null) menu.setPath(menuDetails.getPath());
        if (menuDetails.getIcon() != null) menu.setIcon(menuDetails.getIcon());
        menu.setParentId(menuDetails.getParentId()); // Allow setting parentId to null for root menus
        if (menuDetails.getSortOrder() != null) menu.setSortOrder(menuDetails.getSortOrder());
        if (menuDetails.getRequiredRole() != null) menu.setRequiredRole(menuDetails.getRequiredRole());
        if (menuDetails.getIsActive() != null) menu.setIsActive(menuDetails.getIsActive());
        return menuRepository.save(menu);
    }

    @Transactional
    public void deleteMenu(Long id) {
        menuRepository.deleteById(id);
    }

    @Transactional
    public void logAccess(Long menuId, String menuPath, String userId) {
        logAccess(menuId, menuPath, userId, null, null);
    }

    @Transactional
    public void logAccess(Long menuId, String menuPath, String userId, String userAgent) {
        logAccess(menuId, menuPath, userId, userAgent, null);
    }

    @Transactional
    public void logAccess(Long menuId, String menuPath, String userId, String userAgent, String clientIp) {
        MenuAccessLog log = MenuAccessLog.builder()
                .menuId(menuId)
                .menuPath(menuPath)
                .userId(userId)
                .userAgent(userAgent)
                .clientIp(clientIp)
                .build();
        menuAccessLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public Page<MenuAccessLog> getMenuAccessLogs(String userId, String menuPath, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        Specification<MenuAccessLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (userId != null && !userId.trim().isEmpty()) {
                predicates.add(cb.equal(root.get("userId"), userId));
            }
            if (menuPath != null && !menuPath.trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("menuPath")), "%" + menuPath.toLowerCase() + "%"));
            }
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("accessedAt"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("accessedAt"), endDate));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return menuAccessLogRepository.findAll(spec, pageable);
    }
}
