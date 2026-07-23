package com.classification.domain_system.repository;

import com.classification.domain_system.entity.RecordFieldSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RecordFieldSourceRepository extends JpaRepository<RecordFieldSource, UUID> {
    List<RecordFieldSource> findByRecordId(UUID recordId);
    Optional<RecordFieldSource> findByRecordIdAndFieldKey(UUID recordId, String fieldKey);
}
