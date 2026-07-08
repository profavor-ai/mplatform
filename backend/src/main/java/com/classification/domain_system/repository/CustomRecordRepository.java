package com.classification.domain_system.repository;

import com.classification.domain_system.entity.Record;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface CustomRecordRepository {
    List<Record> findDynamicRecords(List<UUID> nodeIds, String status, Map<String, String> searchParams);
    List<Record> findDynamicRecordsByDomain(UUID domainId, Map<String, String> searchParams);
}
