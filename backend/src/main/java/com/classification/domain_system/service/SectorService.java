package com.classification.domain_system.service;

import com.classification.domain_system.entity.Domain;
import com.classification.domain_system.entity.Sector;
import com.classification.domain_system.repository.DomainRepository;
import com.classification.domain_system.repository.SectorRepository;
import com.classification.domain_system.dto.SectorRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SectorService {

    private final SectorRepository sectorRepository;
    private final DomainRepository domainRepository;

    @Transactional(readOnly = true)
    public List<Sector> getSectorsByDomain(UUID domainId) {
        return sectorRepository.findByDomainIdOrderBySortOrderAsc(domainId);
    }

    @Transactional
    public Sector createSector(UUID domainId, SectorRequest request) {
        Domain domain = domainRepository.findById(domainId)
            .orElseThrow(() -> new RuntimeException("Domain not found"));

        Sector sector = new Sector();
        sector.setDomain(domain);
        sector.setName(request.getName());
        sector.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        return sectorRepository.save(sector);
    }

    @Transactional
    public Sector updateSector(UUID sectorId, SectorRequest request) {
        Sector sector = sectorRepository.findById(sectorId)
            .orElseThrow(() -> new RuntimeException("Sector not found"));
        
        sector.setName(request.getName());
        if (request.getSortOrder() != null) {
            sector.setSortOrder(request.getSortOrder());
        }
        return sectorRepository.save(sector);
    }

    @Transactional
    public void deleteSector(UUID sectorId) {
        sectorRepository.deleteById(sectorId);
    }
}
