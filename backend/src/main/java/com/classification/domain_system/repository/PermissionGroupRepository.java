package com.classification.domain_system.repository;

import com.classification.domain_system.entity.PermissionGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PermissionGroupRepository extends JpaRepository<PermissionGroup, String> {
    List<PermissionGroup> findAllByOrderBySortOrderAsc();
}
