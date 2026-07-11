package com.classification.domain_system.repository;

import com.classification.domain_system.entity.ApprovalRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, UUID> {
    Page<ApprovalRequest> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    Page<ApprovalRequest> findByRequesterIdOrderByCreatedAtDesc(UUID requesterId, Pageable pageable);
    List<ApprovalRequest> findByTargetIdAndStatus(UUID targetId, String status);
}
