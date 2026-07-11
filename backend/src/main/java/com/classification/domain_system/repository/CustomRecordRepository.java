package com.classification.domain_system.repository;

import com.classification.domain_system.entity.Record;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomRecordRepository {
    Page<Record> findDynamicRecords(List<UUID> nodeIds, String status, Map<String, String> searchParams, Pageable pageable);
    Page<Record> findDynamicRecordsByDomain(UUID domainId, Map<String, String> searchParams, Pageable pageable);
}
