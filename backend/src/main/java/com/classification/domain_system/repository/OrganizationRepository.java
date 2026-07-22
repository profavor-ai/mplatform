package com.classification.domain_system.repository;

import com.classification.domain_system.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.Optional;
import java.util.List;

public interface OrganizationRepository extends JpaRepository<Organization, UUID> {
    Optional<Organization> findByName(String name);
    List<Organization> findByIsActiveTrue();
}
