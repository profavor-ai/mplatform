package com.classification.domain_system.repository;

import com.classification.domain_system.entity.PermissionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface PermissionItemRepository extends JpaRepository<PermissionItem, UUID> {
}
