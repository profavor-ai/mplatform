package com.classification.domain_system.repository;

import com.classification.domain_system.entity.Record;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;
import java.util.List;

@Repository
public interface RecordRepository extends JpaRepository<Record, UUID>, CustomRecordRepository {
    List<Record> findByNodeId(UUID nodeId);
    
    @org.springframework.data.jpa.repository.Query("SELECT r FROM Record r JOIN FETCH r.node WHERE r.node.id IN :nodeIds AND r.status NOT IN ('REJECTED', 'MISMATCHED')")
    List<Record> findByNodeIdIn(@org.springframework.data.repository.query.Param("nodeIds") java.util.List<UUID> nodeIds);

    List<Record> findByNodeIdAndStatus(UUID nodeId, String status);

    @org.springframework.data.jpa.repository.Query("SELECT r FROM Record r JOIN FETCH r.node WHERE r.node.domain.id = :domainId AND r.status NOT IN ('REJECTED', 'MISMATCHED')")
    List<Record> findByDomainId(@org.springframework.data.repository.query.Param("domainId") UUID domainId);
}
