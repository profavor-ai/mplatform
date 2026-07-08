package com.classification.domain_system.service;

import com.classification.domain_system.entity.Domain;
import com.classification.domain_system.repository.DomainRepository;
import com.classification.domain_system.dto.DomainRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DomainService {
    
    private final DomainRepository domainRepository;
    
    @Transactional
    public Domain createDomain(DomainRequest request) {
        Domain domain = new Domain();
        domain.setName(request.getName());
        domain.setDescription(request.getDescription());
        domain.setIdentifierFieldId(request.getIdentifierFieldId());
        domain.setDisplayNameFieldId(request.getDisplayNameFieldId());
        domain.setDescriptionFieldId(request.getDescriptionFieldId());
        return domainRepository.save(domain);
    }
    
    @Transactional(readOnly = true)
    public List<Domain> getAllDomains() {
        return domainRepository.findAll();
    }
    
    @Transactional(readOnly = true)
    public Domain getDomain(UUID id) {
        return domainRepository.findById(id).orElseThrow(() -> new RuntimeException("Domain not found"));
    }
    
    @Transactional
    public Domain updateDomain(UUID id, DomainRequest request) {
        if (request.getIdentifierFieldId() == null) {
            throw new IllegalArgumentException("Identifier Field (ID) is required.");
        }
        if (request.getDisplayNameFieldId() == null) {
            throw new IllegalArgumentException("Display Name Field is required.");
        }
        
        Domain domain = getDomain(id);
        domain.setName(request.getName());
        domain.setDescription(request.getDescription());
        domain.setIdentifierFieldId(request.getIdentifierFieldId());
        domain.setDisplayNameFieldId(request.getDisplayNameFieldId());
        domain.setDescriptionFieldId(request.getDescriptionFieldId());
        return domainRepository.save(domain);
    }
}
