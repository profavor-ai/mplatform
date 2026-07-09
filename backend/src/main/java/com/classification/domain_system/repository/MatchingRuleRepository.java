package com.classification.domain_system.repository;

import com.classification.domain_system.entity.MatchingRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MatchingRuleRepository extends JpaRepository<MatchingRule, UUID> {
    List<MatchingRule> findByDomainId(UUID domainId);
    List<MatchingRule> findByDomainIdAndNodeId(UUID domainId, UUID nodeId);
    List<MatchingRule> findByDomainIdAndIsActiveTrue(UUID domainId);
}
