package com.classification.domain_system.service;

import com.classification.domain_system.entity.ClassificationNode;
import com.classification.domain_system.entity.Domain;
import com.classification.domain_system.repository.ClassificationNodeRepository;
import com.classification.domain_system.repository.DomainRepository;
import com.classification.domain_system.dto.ClassificationNodeRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClassificationNodeService {
    
    private final ClassificationNodeRepository nodeRepository;
    private final DomainRepository domainRepository;
    
    @Transactional
    public ClassificationNode createNode(UUID domainId, ClassificationNodeRequest request) {
        Domain domain = domainRepository.findById(domainId)
                .orElseThrow(() -> new RuntimeException("Domain not found"));
                
        ClassificationNode parent = null;
        String domainNameStr = domain.getName() != null ? domain.getName().getOrDefault("ko", domain.getName().get("en")) : "domain";
        String path = "/" + domainNameStr;
        int depth = 0;
        
        String reqNameStr = request.getName() != null ? request.getName().getOrDefault("ko", request.getName().get("en")) : "node";

        if (request.getParentId() != null) {
            parent = nodeRepository.findById(request.getParentId())
                    .orElseThrow(() -> new RuntimeException("Parent node not found"));
            path = parent.getPath() + "/" + reqNameStr;
            depth = parent.getDepth() + 1;
        } else {
            path = path + "/" + reqNameStr;
            depth = 1;
        }
        
        ClassificationNode node = new ClassificationNode();
        node.setDomain(domain);
        node.setParent(parent);
        node.setName(request.getName());
        node.setPath(path);
        node.setDepth(depth);
        node.setOrder(request.getOrder() != null ? request.getOrder() : 0);
        node.setIsDeleted(false);
        
        return nodeRepository.save(node);
    }
    
    @Transactional(readOnly = true)
    public List<ClassificationNode> getTree(UUID domainId) {
        return nodeRepository.findByDomain_IdAndParentIsNullAndIsDeletedFalse(domainId);
    }
    
    @Transactional
    public ClassificationNode updateNode(UUID domainId, UUID nodeId, ClassificationNodeRequest request) {
        ClassificationNode node = nodeRepository.findById(nodeId)
                .orElseThrow(() -> new RuntimeException("Node not found"));
                
        if (!node.getDomain().getId().equals(domainId)) {
            throw new RuntimeException("Node does not belong to the specified domain");
        }
        
        node.setName(request.getName());
        node.setOrder(request.getOrder() != null ? request.getOrder() : node.getOrder());
        
        return nodeRepository.save(node);
    }
}
