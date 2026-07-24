package com.classification.domain_system.event;

import com.classification.domain_system.entity.ApprovalRequest;
import com.classification.domain_system.entity.ApprovalStep;
import com.classification.domain_system.entity.Record;
import com.classification.domain_system.entity.FieldDefinition;
import com.classification.domain_system.entity.Domain;
import com.classification.domain_system.repository.*;
import com.classification.domain_system.service.CalculatedFieldEvaluator;
import com.classification.domain_system.service.FieldDefinitionService;
import com.classification.domain_system.service.RecordHistoryWriter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApprovalEventListener {

    private final ApprovalRequestRepository approvalRepository;
    private final ApprovalStepRepository stepRepository;
    private final RecordRepository recordRepository;
    private final RecordHistoryRepository recordHistoryRepository;
    private final FieldDefinitionService fieldDefinitionService;
    private final com.classification.domain_system.service.NumberingService numberingService;
    private final org.springframework.context.ApplicationEventPublisher applicationEventPublisher;
    private final CalculatedFieldEvaluator calculatedFieldEvaluator;
    private final RecordHistoryWriter recordHistoryWriter;

    @EventListener
    @Transactional
    public void onApprovalRequestCreated(ApprovalRequestCreatedEvent event) {
        ApprovalRequest approval = event.getApprovalRequest();
        log.info("[EVENT_DRIVEN] Received ApprovalRequestCreatedEvent for Request ID: {}", approval.getId());
        
        long realStepCount = approval.getSteps().stream().filter(s -> s.getStepOrder() > 0).count();
        if (realStepCount == 0) {
            approval.setStatus("APPROVED");
            approval.getSteps().stream().filter(s -> s.getStepOrder() == 0).forEach(s -> {
                s.setStatus("APPROVED");
                s.setComment("시스템 자동 승인 (결재선 미설정)");
                stepRepository.saveAndFlush(s);
            });
            approvalRepository.saveAndFlush(approval);
            applyFinalApproval(approval);
            return;
        }

        autoApproveStepsIfRequesterIsAssignee(approval);
    }

    @EventListener
    @Transactional
    public void onApprovalStepApproved(ApprovalStepApprovedEvent event) {
        ApprovalRequest requestFromEvent = event.getApprovalRequest();
        ApprovalStep approvedStep = event.getApprovedStep();
        log.info("[EVENT_DRIVEN] Received ApprovalStepApprovedEvent for Request ID: {}, Approved Step Order: {}", 
                requestFromEvent.getId(), approvedStep.getStepOrder());

        try {
            ApprovalRequest approval = approvalRepository.findByIdWithLock(requestFromEvent.getId())
                    .orElse(requestFromEvent);

            if (!"PENDING".equals(approval.getStatus())) {
                log.info("ApprovalRequest {} status is already {}, skipping advancement.", approval.getId(), approval.getStatus());
                return;
            }

            boolean allApproved = approval.getSteps().stream()
                    .filter(s -> s.getStepOrder().equals(approval.getCurrentStepOrder()))
                    .allMatch(s -> "APPROVED".equals(s.getStatus()));
                    
            if (allApproved) {
                Integer nextOrder = approval.getSteps().stream()
                        .map(ApprovalStep::getStepOrder)
                        .filter(order -> order > approval.getCurrentStepOrder())
                        .min(Integer::compareTo)
                        .orElse(null);
                        
                if (nextOrder != null) {
                    approval.getSteps().stream()
                            .filter(s -> s.getStepOrder().equals(nextOrder))
                            .forEach(s -> s.setStatus("PENDING"));
                    approval.setCurrentStepOrder(nextOrder);
                    approvalRepository.saveAndFlush(approval);
                    
                    autoApproveStepsIfRequesterIsAssignee(approval);
                } else {
                    approval.setStatus("APPROVED");
                    approvalRepository.saveAndFlush(approval);
                    applyFinalApproval(approval);
                }
            }
        } catch (org.springframework.orm.ObjectOptimisticLockingFailureException | jakarta.persistence.OptimisticLockException e) {
            log.warn("Concurrent modification detected for ApprovalRequest ID: {}. Another thread already advanced or finalized this request.", requestFromEvent.getId());
        }
    }

    void autoApproveStepsIfRequesterIsAssignee(ApprovalRequest approval) {
        if (approval == null || approval.getCurrentStepOrder() == null || approval.getSteps() == null) {
            return;
        }
        boolean progress = true;
        while (progress) {
            progress = false;
            final int currentOrder = approval.getCurrentStepOrder();
            List<ApprovalStep> pendingAutoSteps = approval.getSteps().stream()
                    .filter(s -> s.getStepOrder().equals(currentOrder) 
                            && "PENDING".equals(s.getStatus()) 
                            && s.getAssigneeId().equals(approval.getRequesterId()))
                    .toList();
            
            if (!pendingAutoSteps.isEmpty()) {
                for (ApprovalStep step : pendingAutoSteps) {
                    step.setStatus("APPROVED");
                    step.setComment("시스템 자동 승인 (기안자 자동 전결)");
                    stepRepository.saveAndFlush(step);
                }
                
                boolean allApproved = approval.getSteps().stream()
                        .filter(s -> s.getStepOrder().equals(currentOrder))
                        .allMatch(s -> "APPROVED".equals(s.getStatus()));
                
                if (allApproved) {
                    Integer nextOrder = approval.getSteps().stream()
                            .map(ApprovalStep::getStepOrder)
                            .filter(order -> order > currentOrder)
                            .min(Integer::compareTo)
                            .orElse(null);
                    
                    if (nextOrder != null) {
                        approval.getSteps().stream()
                                .filter(s -> s.getStepOrder().equals(nextOrder))
                                .forEach(s -> s.setStatus("PENDING"));
                        approval.setCurrentStepOrder(nextOrder);
                        approvalRepository.saveAndFlush(approval);
                        progress = true;
                    } else {
                        approval.setStatus("APPROVED");
                        approvalRepository.saveAndFlush(approval);
                        applyFinalApproval(approval);
                    }
                }
            }
        }
    }

    private void applyFinalApproval(ApprovalRequest approval) {
        if ("RECORD".equals(approval.getTargetType())) {
            Record record = recordRepository.findById(approval.getTargetId())
                    .orElseThrow(() -> new RuntimeException("Record not found"));
            record.setStatus("ACTIVE");
            
            String changesJson = approval.getChanges();
            Domain domain = record.getNode().getDomain();
            if (domain != null && domain.getNumberingPattern() != null && !domain.getNumberingPattern().isBlank()) {
                String issuedCode = numberingService.issueNextCode(domain.getId());
                if (domain.getIdentifierFieldId() != null && !issuedCode.isBlank()) {
                    String targetKey = domain.getIdentifierFieldId().toString();
                    List<FieldDefinition> fields = fieldDefinitionService.getEffectiveFields(record.getNode().getId());
                    for (FieldDefinition f : fields) {
                        if (f.getId() != null && f.getId().toString().equals(domain.getIdentifierFieldId().toString())) {
                            if (f.getKey() != null) {
                                targetKey = f.getKey();
                            }
                            break;
                        }
                    }
                    changesJson = injectIdentifierValue(changesJson, targetKey, issuedCode);
                }
            }

            String finalData = recomputeCalculatedFields(record.getNode().getId(), changesJson);
            record.setData(finalData);
            record.setVersion(1);
            recordRepository.save(record);
            logHistory(record, "CREATE", approval.getRequesterId(), null, finalData, approval.getId());
            applicationEventPublisher.publishEvent(new MasterDataChangedEvent(this, record.getId(), record.getNode().getId(), "CREATE", finalData));
        } else if ("RECORD_UPDATE".equals(approval.getTargetType())) {
            Record record = recordRepository.findById(approval.getTargetId())
                    .orElseThrow(() -> new RuntimeException("Record not found"));
            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(approval.getChanges());
                String prevData = record.getData();
                if (root.has("after")) {
                    String afterData = recomputeCalculatedFields(record.getNode().getId(), root.get("after").toString());
                    record.setData(afterData);
                }
                record.setStatus("ACTIVE");
                record.setVersion(record.getVersion() + 1);
                recordRepository.save(record);
                logHistory(record, "UPDATE", approval.getRequesterId(), prevData, record.getData(), approval.getId());
                applicationEventPublisher.publishEvent(new MasterDataChangedEvent(this, record.getId(), record.getNode().getId(), "UPDATE", record.getData()));
            } catch (Exception e) {
                log.error("Error applying final approval for RECORD_UPDATE", e);
            }
        } else if ("RECORD_DELETE".equals(approval.getTargetType())) {
            Record record = recordRepository.findById(approval.getTargetId())
                    .orElseThrow(() -> new RuntimeException("Record not found"));
            String deletedData = record.getData();
            logHistory(record, "DELETE", approval.getRequesterId(), deletedData, null, approval.getId());
            recordRepository.delete(record);
            applicationEventPublisher.publishEvent(new MasterDataChangedEvent(this, record.getId(), record.getNode().getId(), "DELETE", deletedData));
        }
    }

    private void logHistory(Record record, String changeType, UUID changedBy, String prevData, String newData, UUID approvalRequestId) {
        recordHistoryWriter.logHistory(record, changeType, changedBy, prevData, newData, approvalRequestId);
    }

    private String recomputeCalculatedFields(UUID nodeId, String dataJson) {
        return calculatedFieldEvaluator.recomputeCalculatedFields(nodeId, dataJson);
    }

    private String injectIdentifierValue(String dataJson, String fieldKey, String value) {
        if (dataJson == null || dataJson.isBlank() || fieldKey == null) return dataJson;
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> data = mapper.readValue(dataJson, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
            data.put(fieldKey, value);
            return mapper.writeValueAsString(data);
        } catch (Exception e) {
            log.error("Failed to inject identifier value into data JSON", e);
            return dataJson;
        }
    }
}
