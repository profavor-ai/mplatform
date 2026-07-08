package com.classification.domain_system.service;

import com.classification.domain_system.repository.DomainRepository;
import com.classification.domain_system.repository.RecordRepository;
import com.classification.domain_system.repository.ApprovalRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import java.util.Map;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
public class DashboardService {
    private final DomainRepository domainRepository;
    private final RecordRepository recordRepository;
    private final ApprovalRequestRepository approvalRepository;

    @Transactional(readOnly = true)
    public Map<String, Long> getStats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("totalDomains", domainRepository.count());
        stats.put("pendingApprovals", (long) approvalRepository.findByStatusOrderByCreatedAtDesc("PENDING").size());
        stats.put("activeRecords", recordRepository.findAll().stream().filter(r -> "ACTIVE".equals(r.getStatus())).count());
        return stats;
    }
}
