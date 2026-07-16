package com.classification.domain_system.event;

import com.classification.domain_system.entity.ApprovalRequest;
import com.classification.domain_system.entity.ApprovalStep;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ApprovalStepApprovedEvent {
    private final ApprovalRequest approvalRequest;
    private final ApprovalStep approvedStep;
}
