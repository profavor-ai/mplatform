package com.classification.domain_system.repository;

import com.classification.domain_system.entity.DqViolation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

@Repository
public interface DqViolationRepository extends JpaRepository<DqViolation, UUID> {
    Page<DqViolation> findByRecordIdOrderByCheckedAtDesc(UUID recordId, Pageable pageable);

    List<DqViolation> findByRecordIdAndResolvedFalse(UUID recordId);

    long countByRecordIdAndResolvedFalse(UUID recordId);

    @Query("SELECT v.fieldKey, COUNT(v) FROM DqViolation v WHERE v.recordId IN " +
           "(SELECT r.id FROM Record r WHERE r.node.domain.id = :domainId) " +
           "AND v.resolved = false GROUP BY v.fieldKey")
    List<Object[]> countViolationsByFieldKeyForDomain(@Param("domainId") UUID domainId);

    @Query("SELECT v.severity, COUNT(v) FROM DqViolation v WHERE v.recordId IN " +
           "(SELECT r.id FROM Record r WHERE r.node.domain.id = :domainId) " +
           "AND v.resolved = false GROUP BY v.severity")
    List<Object[]> countViolationsBySeverityForDomain(@Param("domainId") UUID domainId);

    void deleteByRecordId(UUID recordId);
}
