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

    @org.springframework.data.jpa.repository.Query(value = "SELECT f FROM FieldDefinition f WHERE f.definedAtNode.id = :nodeId OR f.domain.id = :domainId ORDER BY CASE WHEN f.domain IS NOT NULL THEN 0 ELSE 1 END ASC, f.order ASC",
            countQuery = "SELECT count(f) FROM FieldDefinition f WHERE f.definedAtNode.id = :nodeId OR f.domain.id = :domainId")
    org.springframework.data.domain.Page<FieldDefinition> findEffectiveFieldsWithPagination(@org.springframework.data.repository.query.Param("nodeId") UUID nodeId, @org.springframework.data.repository.query.Param("domainId") UUID domainId, org.springframework.data.domain.Pageable pageable);
}
