package com.classification.domain_system.repository;

import com.classification.domain_system.entity.FieldDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FieldDefinitionRepository extends JpaRepository<FieldDefinition, UUID> {
    List<FieldDefinition> findByDefinedAtNodeIdOrderByOrderAsc(UUID nodeId);
    List<FieldDefinition> findByDomainIdOrderByOrderAsc(UUID domainId);
    List<FieldDefinition> findByDefinedAtNodeIdIn(List<UUID> nodeIds);
}
