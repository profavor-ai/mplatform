package com.classification.domain_system.repository;

import com.classification.domain_system.entity.Record;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface RecordRepository extends JpaRepository<Record, UUID>, CustomRecordRepository {
    Page<Record> findByNodeId(UUID nodeId, Pageable pageable);
    
    @org.springframework.data.jpa.repository.Query(value = "SELECT r FROM Record r JOIN FETCH r.node WHERE r.node.id IN :nodeIds AND r.status NOT IN ('REJECTED', 'MISMATCHED')", countQuery = "SELECT count(r) FROM Record r WHERE r.node.id IN :nodeIds AND r.status NOT IN ('REJECTED', 'MISMATCHED')")
    Page<Record> findByNodeIdIn(@org.springframework.data.repository.query.Param("nodeIds") java.util.List<UUID> nodeIds, Pageable pageable);

    Page<Record> findByNodeIdAndStatus(UUID nodeId, String status, Pageable pageable);

    @org.springframework.data.jpa.repository.Query(value = "SELECT r FROM Record r JOIN FETCH r.node WHERE r.node.domain.id = :domainId AND r.status NOT IN ('REJECTED', 'MISMATCHED')", countQuery = "SELECT count(r) FROM Record r WHERE r.node.domain.id = :domainId AND r.status NOT IN ('REJECTED', 'MISMATCHED')")
    Page<Record> findByDomainId(@org.springframework.data.repository.query.Param("domainId") UUID domainId, Pageable pageable);
}
