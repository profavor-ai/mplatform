package com.classification.domain_system.service;

import com.classification.domain_system.entity.ClassificationNode;
import com.classification.domain_system.entity.Domain;
import com.classification.domain_system.entity.Record;
import com.classification.domain_system.repository.ClassificationNodeRepository;
import com.classification.domain_system.repository.DomainRepository;
import com.classification.domain_system.repository.RecordRepository;
import com.classification.domain_system.service.dq.DqRuleEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DqScheduledScanService {

    private final DomainRepository domainRepository;
    private final ClassificationNodeRepository nodeRepository;
    private final RecordRepository recordRepository;
    private final DqRuleEngine dqRuleEngine;

    @Scheduled(cron = "${dq.scan.cron:0 0 2 * * ?}")
    public void runScheduledDqScan() {
        log.info("[DQ Schedule] Starting automated periodic DQ scan...");
        List<Domain> activeDomains = domainRepository.findAll().stream()
                .filter(Domain::isAutoDqScanEnabled)
                .toList();

        int scannedDomainCount = 0;
        for (Domain domain : activeDomains) {
            try {
                List<ClassificationNode> nodes = nodeRepository.findByDomainId(domain.getId());
                for (ClassificationNode node : nodes) {
                    List<Record> records = recordRepository.findByNodeId(node.getId(), Pageable.unpaged()).getContent();
                    for (Record r : records) {
                        if (r.getData() != null) {
                            dqRuleEngine.evaluate(node.getId(), r.getData(), r.getId());
                        }
                    }
                }
                scannedDomainCount++;
                log.info("[DQ Schedule] Completed auto scan for domain: {} ({})", domain.getName(), domain.getId());
            } catch (Exception e) {
                log.error("[DQ Schedule] Failed auto scan for domain {}: {}", domain.getId(), e.getMessage(), e);
            }
        }
        log.info("[DQ Schedule] Completed automated periodic DQ scan. Total scanned domains: {}", scannedDomainCount);
    }
}
