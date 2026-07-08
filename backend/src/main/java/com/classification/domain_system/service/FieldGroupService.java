package com.classification.domain_system.service;

import com.classification.domain_system.entity.Domain;
import com.classification.domain_system.entity.Sector;
import com.classification.domain_system.entity.FieldGroup;
import com.classification.domain_system.repository.DomainRepository;
import com.classification.domain_system.repository.SectorRepository;
import com.classification.domain_system.repository.FieldGroupRepository;
import com.classification.domain_system.dto.FieldGroupRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FieldGroupService {

    private final FieldGroupRepository fieldGroupRepository;
    private final SectorRepository sectorRepository;
    private final DomainRepository domainRepository;

    @Transactional(readOnly = true)
    public List<FieldGroup> getGroupsByDomain(UUID domainId) {
        return fieldGroupRepository.findByDomainIdOrderBySortOrderAsc(domainId);
    }

    @Transactional(readOnly = true)
    public List<FieldGroup> getGroupsBySector(UUID sectorId) {
        return fieldGroupRepository.findBySectorIdOrderBySortOrderAsc(sectorId);
    }

    @Transactional
    public FieldGroup createGroup(UUID domainId, FieldGroupRequest request) {
        Domain domain = domainRepository.findById(domainId)
            .orElseThrow(() -> new RuntimeException("Domain not found"));
        
        Sector sector = sectorRepository.findById(request.getSectorId())
            .orElseThrow(() -> new RuntimeException("Sector not found"));

        if (!sector.getDomain().getId().equals(domainId)) {
            throw new RuntimeException("Sector does not belong to the domain");
        }

        FieldGroup group = new FieldGroup();
        group.setDomain(domain);
        group.setSector(sector);
        group.setName(request.getName());
        group.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        if (request.getIsDefaultOpen() != null) {
            group.setIsDefaultOpen(request.getIsDefaultOpen());
        }
        
        return fieldGroupRepository.save(group);
    }

    @Transactional
    public FieldGroup updateGroup(UUID groupId, FieldGroupRequest request) {
        FieldGroup group = fieldGroupRepository.findById(groupId)
            .orElseThrow(() -> new RuntimeException("FieldGroup not found"));

        if (request.getSectorId() != null && !group.getSector().getId().equals(request.getSectorId())) {
            Sector sector = sectorRepository.findById(request.getSectorId())
                .orElseThrow(() -> new RuntimeException("Sector not found"));
            group.setSector(sector);
        }

        group.setName(request.getName());
        if (request.getSortOrder() != null) {
            group.setSortOrder(request.getSortOrder());
        }
        if (request.getIsDefaultOpen() != null) {
            group.setIsDefaultOpen(request.getIsDefaultOpen());
        }
        
        return fieldGroupRepository.save(group);
    }

    @Transactional
    public void deleteGroup(UUID groupId) {
        fieldGroupRepository.deleteById(groupId);
    }
}
