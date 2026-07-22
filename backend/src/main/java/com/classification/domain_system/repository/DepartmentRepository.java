package com.classification.domain_system.repository;

import com.classification.domain_system.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.List;
import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department, UUID> {
    List<Department> findByOrganizationId(UUID organizationId);
    List<Department> findByOrganizationIdAndIsActiveTrue(UUID organizationId);
    Optional<Department> findByOrganizationIdAndName(UUID organizationId, String name);
}
