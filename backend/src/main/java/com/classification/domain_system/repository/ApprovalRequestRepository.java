package com.classification.domain_system.repository;

import com.classification.domain_system.entity.ApprovalRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;
import java.util.List;

@Repository
public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, UUID> {
    List<ApprovalRequest> findByStatusOrderByCreatedAtDesc(String status);
    List<ApprovalRequest> findByRequesterIdOrderByCreatedAtDesc(UUID requesterId);
    List<ApprovalRequest> findByTargetIdAndStatus(UUID targetId, String status);
}
