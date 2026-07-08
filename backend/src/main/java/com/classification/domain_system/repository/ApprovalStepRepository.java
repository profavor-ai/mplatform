package com.classification.domain_system.repository;

import com.classification.domain_system.entity.ApprovalStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.List;

@Repository
public interface ApprovalStepRepository extends JpaRepository<ApprovalStep, UUID> {
    List<ApprovalStep> findByAssigneeIdAndStatus(UUID assigneeId, String status);
}
