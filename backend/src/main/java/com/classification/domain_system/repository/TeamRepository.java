package com.classification.domain_system.repository;

import com.classification.domain_system.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.List;
import java.util.Optional;

public interface TeamRepository extends JpaRepository<Team, UUID> {
    List<Team> findByOrganizationId(UUID organizationId);
    List<Team> findByDepartmentId(UUID departmentId);
    Optional<Team> findByOrganizationIdAndName(UUID organizationId, String name);
}
