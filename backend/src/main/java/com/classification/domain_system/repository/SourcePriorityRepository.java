package com.classification.domain_system.repository;

import com.classification.domain_system.entity.SourcePriority;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SourcePriorityRepository extends JpaRepository<SourcePriority, UUID> {
    List<SourcePriority> findByDomainIdOrderByPriorityAsc(UUID domainId);
    Optional<SourcePriority> findByDomainIdAndSourceSystem(UUID domainId, String sourceSystem);
}
