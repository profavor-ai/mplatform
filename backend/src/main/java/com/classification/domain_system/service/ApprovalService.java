package com.classification.domain_system.service;

import com.classification.domain_system.entity.ApprovalRequest;
import com.classification.domain_system.entity.ApprovalStep;
import com.classification.domain_system.entity.Domain;
import com.classification.domain_system.entity.Record;
import com.classification.domain_system.entity.ClassificationNode;
import com.classification.domain_system.repository.ApprovalRequestRepository;
import com.classification.domain_system.repository.ApprovalStepRepository;
import com.classification.domain_system.repository.RecordRepository;
import com.classification.domain_system.repository.ClassificationNodeRepository;
import com.classification.domain_system.repository.WorkflowConfigRepository;
import com.classification.domain_system.repository.RecordHistoryRepository;
import com.classification.domain_system.entity.WorkflowConfig;
import com.classification.domain_system.entity.RecordHistory;
import com.classification.domain_system.dto.RecordRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.classification.domain_system.entity.FieldDefinition;

@Service
@RequiredArgsConstructor
public class ApprovalService {
    
    private final ApprovalRequestRepository approvalRepository;
    private final ApprovalStepRepository stepRepository;
    private final RecordRepository recordRepository;
    private final ClassificationNodeRepository nodeRepository;
    private final WorkflowConfigRepository workflowConfigRepository;
    private final DataQualityService dqService;
    private final RecordHistoryRepository recordHistoryRepository;
    private final FieldDefinitionService fieldDefinitionService;
    private final MatchingService matchingService;
    
    private void logHistory(Record record, String changeType, UUID changedBy, String prevData, String newData, UUID approvalRequestId) {
        RecordHistory history = new RecordHistory();
        history.setRecordId(record.getId());
        history.setChangeType(changeType);
        history.setChangedBy(changedBy);
        history.setPreviousData(prevData);
        history.setNewData(newData);
        history.setApprovalRequestId(approvalRequestId);
        history.setVersion(record.getVersion());
        history.setSourceSystem(record.getSourceSystem());
        recordHistoryRepository.save(history);
    }

    /**
     * Recompute all CALCULATED field values in the data JSON based on field definitions.
     * This ensures calculated fields are always up-to-date when source fields change.
     */
    private String recomputeCalculatedFields(UUID nodeId, String dataJson) {
        if (dataJson == null || dataJson.isBlank()) return dataJson;
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> data = mapper.readValue(dataJson, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
            
            List<FieldDefinition> fields = fieldDefinitionService.getEffectiveFields(nodeId);
            
            for (FieldDefinition field : fields) {
                if ("CALCULATED".equals(field.getType()) && field.getOptions() != null) {
                    try {
                        com.fasterxml.jackson.databind.JsonNode opts = mapper.readTree(field.getOptions());
                        if (opts.has("formula")) {
                            String formula = opts.get("formula").asText();
                            Double result = evaluateFormula(formula, data);
                            if (result != null && !result.isNaN() && !result.isInfinite()) {
                                data.put(field.getKey(), result);
                            }
                        }
                    } catch (Exception e) {
                        // Skip individual field errors
                    }
                }
            }
            
            return mapper.writeValueAsString(data);
        } catch (Exception e) {
            return dataJson;
        }
    }
    
    /**
     * Evaluate a formula string like "${PER} * ${PBR}" using data values.
     */
    private Double evaluateFormula(String formula, Map<String, Object> data) {
        try {
            Pattern pattern = Pattern.compile("\\$\\{([^}]+)}");
            Matcher matcher = pattern.matcher(formula);
            StringBuilder sb = new StringBuilder();
            while (matcher.find()) {
                String key = matcher.group(1);
                Object val = data.get(key);
                double numVal = 0;
                if (val instanceof Number) {
                    numVal = ((Number) val).doubleValue();
                } else if (val != null) {
                    try { numVal = Double.parseDouble(val.toString()); } catch (Exception e) { numVal = 0; }
                }
                matcher.appendReplacement(sb, String.valueOf(numVal));
            }
            matcher.appendTail(sb);
            
            return evalExpr(sb.toString().trim());
        } catch (Exception e) {
            return null;
        }
    }
    
    // Simple recursive-descent math expression evaluator (+, -, *, /, parentheses)
    private double evalExpr(String expr) {
        return new Object() {
            int pos = 0;
            
            double parse() {
                double result = parseAddSub();
                return result;
            }
            
            double parseAddSub() {
                double left = parseMulDiv();
                while (pos < expr.length()) {
                    char op = expr.charAt(pos);
                    if (op == '+' || op == '-') {
                        pos++;
                        double right = parseMulDiv();
                        left = op == '+' ? left + right : left - right;
                    } else break;
                }
                return left;
            }
            
            double parseMulDiv() {
                double left = parseUnary();
                while (pos < expr.length()) {
                    char op = expr.charAt(pos);
                    if (op == '*' || op == '/') {
                        pos++;
                        double right = parseUnary();
                        left = op == '*' ? left * right : left / right;
                    } else break;
                }
                return left;
            }
            
            double parseUnary() {
                skipSpaces();
                if (pos < expr.length() && expr.charAt(pos) == '-') {
                    pos++;
                    return -parseUnary();
                }
                return parsePrimary();
            }
            
            double parsePrimary() {
                skipSpaces();
                // Handle math functions: CEIL, FLOOR, ROUND, ABS
                for (String fn : new String[]{"CEIL", "FLOOR", "ROUND", "ABS"}) {
                    if (pos + fn.length() <= expr.length() && expr.substring(pos, pos + fn.length()).equals(fn)) {
                        pos += fn.length();
                        skipSpaces();
                        if (pos < expr.length() && expr.charAt(pos) == '(') {
                            pos++; // skip '('
                            double val = parseAddSub();
                            // Check for optional second argument (for ROUND)
                            double decimals = 0;
                            skipSpaces();
                            if (pos < expr.length() && expr.charAt(pos) == ',') {
                                pos++; // skip ','
                                decimals = parseAddSub();
                            }
                            skipSpaces();
                            if (pos < expr.length() && expr.charAt(pos) == ')') pos++; // skip ')'
                            switch (fn) {
                                case "CEIL": return Math.ceil(val);
                                case "FLOOR": return Math.floor(val);
                                case "ABS": return Math.abs(val);
                                case "ROUND": {
                                    double factor = Math.pow(10, decimals);
                                    return Math.round(val * factor) / factor;
                                }
                                default: return val;
                            }
                        }
                    }
                }
                if (pos < expr.length() && expr.charAt(pos) == '(') {
                    pos++; // skip '('
                    double val = parseAddSub();
                    skipSpaces();
                    if (pos < expr.length() && expr.charAt(pos) == ')') pos++; // skip ')'
                    return val;
                }
                int start = pos;
                while (pos < expr.length() && (Character.isDigit(expr.charAt(pos)) || expr.charAt(pos) == '.')) {
                    pos++;
                }
                if (start == pos) return 0;
                return Double.parseDouble(expr.substring(start, pos));
            }
            
            void skipSpaces() {
                while (pos < expr.length() && expr.charAt(pos) == ' ') pos++;
            }
        }.parse();
    }

    private boolean isEffectiveConfig(WorkflowConfig config) {
        return config != null && config.getStepsConfig() != null 
            && !config.getStepsConfig().isEmpty() 
            && !config.getStepsConfig().equals("{\"steps\":[],\"observerIds\":[]}");
    }

    public WorkflowConfig resolveWorkflow(UUID nodeId, String actionType) {
        ClassificationNode current = nodeRepository.findById(nodeId).orElse(null);
        while (current != null) {
            java.util.Optional<WorkflowConfig> conf = workflowConfigRepository.findByNodeIdAndActionType(
                current.getId(), actionType
            );
            if (conf.isPresent() && isEffectiveConfig(conf.get())) {
                return conf.get();
            }
            current = current.getParent();
        }
        // Fallback to domain level
        if (nodeId != null) {
            ClassificationNode node = nodeRepository.findById(nodeId).orElse(null);
            if (node != null) {
                java.util.Optional<WorkflowConfig> domainConf = workflowConfigRepository.findByDomainIdAndNodeIdIsNullAndActionType(
                    node.getDomain().getId(), actionType
                );
                if (domainConf.isPresent() && isEffectiveConfig(domainConf.get())) return domainConf.get();
            }
        }
        return null;
    }

    private void buildDynamicSteps(ApprovalRequest approval, WorkflowConfig config) {
        List<ApprovalStep> steps = new java.util.ArrayList<>();
        try {
            if (config != null && config.getStepsConfig() != null && !config.getStepsConfig().isEmpty()) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(config.getStepsConfig());
                
                if (root.has("steps") && root.get("steps").isArray()) {
                    for (com.fasterxml.jackson.databind.JsonNode stepNode : root.get("steps")) {
                        ApprovalStep step = new ApprovalStep();
                        step.setApprovalRequest(approval);
                        step.setStepType(stepNode.get("stepType").asText());
                        step.setAssigneeId(UUID.fromString(stepNode.get("assigneeId").asText()));
                        step.setStepOrder(stepNode.get("stepOrder").asInt());
                        step.setStatus(step.getStepOrder() == 1 ? "PENDING" : "WAITING");
                        steps.add(step);
                    }
                }
                
                if (root.has("observerIds") && root.get("observerIds").isArray()) {
                    approval.setObserverIds(mapper.writeValueAsString(root.get("observerIds")));
                } else {
                    approval.setObserverIds("[]");
                }
            } else {
                approval.setObserverIds("[]");
            }
        } catch (Exception e) {
            approval.setObserverIds("[]");
            e.printStackTrace();
        }
        approval.setSteps(steps);
    }

    @Transactional
    public ApprovalRequest requestRecordCreation(UUID nodeId, RecordRequest request) {
        ClassificationNode node = nodeRepository.findById(nodeId)
                .orElseThrow(() -> new RuntimeException("Node not found"));
                
        // Check if Domain has the required mapping fields
        Domain domain = node.getDomain();
        if (domain.getIdentifierFieldId() == null || domain.getDisplayNameFieldId() == null) {
            throw new RuntimeException("Domain is missing required field mappings (ID or Name). Please configure the domain settings first.");
        }
        
        // 1. Data Quality Check
        DataQualityService.DQResult dq = dqService.validateData(nodeId, request.getData());
        if (!dq.isValid) {
            throw new RuntimeException("Data Quality Check Failed: " + String.join(", ", dq.errors));
        }

        // 1.5. Duplicate Check (Golden Record) -> UPSERT Behavior
        MatchingService.DuplicateResult dup = matchingService.checkDuplicates(nodeId, request.getData());
        if (dup.hasDuplicates) {
            if (dup.duplicateRecordIds != null && dup.duplicateRecordIds.size() == 1) {
                // Exactly one duplicate found -> Convert to UPDATE request
                return requestRecordUpdate(dup.duplicateRecordIds.get(0), request);
            } else {
                throw new RuntimeException("Deduplication Failed: " + dup.message);
            }
        }

        // 2. Create Record in PENDING_APPROVAL status with calculated fields
        String computedData = recomputeCalculatedFields(nodeId, request.getData());
        Record record = new Record();
        record.setNode(node);
        record.setStatus("PENDING_APPROVAL");
        record.setData(computedData);
        record = recordRepository.save(record);
        
        // 3. Create ApprovalRequest
        ApprovalRequest approval = new ApprovalRequest();
        approval.setTargetType("RECORD");
        approval.setTargetId(record.getId());
        approval.setClassificationNode(record.getNode());
        approval.setRequesterId(request.getRequesterId());
        approval.setStatus("PENDING");
        approval.setChanges(computedData);
        approval.setCurrentStepOrder(1);
        
        // 4. Create Steps based on dynamic request
        WorkflowConfig config = resolveWorkflow(nodeId, "CREATE");
        buildDynamicSteps(approval, config);
        
        // 5. Add Requester's DRAFT Step (stepOrder = 0)
        ApprovalStep draftStep = new ApprovalStep();
        draftStep.setApprovalRequest(approval);
        draftStep.setStepType("DRAFT");
        draftStep.setAssigneeId(request.getRequesterId());
        draftStep.setStepOrder(0);
        draftStep.setStatus("SUBMITTED");
        draftStep.setComment(request.getComment());
        approval.getSteps().add(draftStep);
        
        return approvalRepository.save(approval);
    }
    
    @Transactional
    public ApprovalRequest requestRecordUpdate(UUID recordId, RecordRequest request) {
        Record record = recordRepository.findById(recordId)
                .orElseThrow(() -> new RuntimeException("Record not found"));
        
        if ("PENDING_APPROVAL".equals(record.getStatus())) {
            throw new RuntimeException("Cannot update a record that is pending creation approval.");
        }
        
        List<ApprovalRequest> pendingUpdates = approvalRepository.findByTargetIdAndStatus(recordId, "PENDING");
        boolean hasPendingUpdate = pendingUpdates.stream().anyMatch(a -> "RECORD_UPDATE".equals(a.getTargetType()));
        if (hasPendingUpdate) {
            throw new RuntimeException("This record is already under a pending update approval.");
        }
        
        UUID nodeId = record.getNode().getId();
        
        // 1. Data Quality Check
        DataQualityService.DQResult dq = dqService.validateData(nodeId, request.getData());
        if (!dq.isValid) {
            throw new RuntimeException("Data Quality Check Failed: " + String.join(", ", dq.errors));
        }

        // 1.5. Duplicate Check
        MatchingService.DuplicateResult dup = matchingService.checkDuplicates(nodeId, request.getData());
        if (dup.hasDuplicates) {
            // Exclude self from duplicates
            dup.duplicateRecordIds.remove(recordId);
            if (!dup.duplicateRecordIds.isEmpty()) {
                throw new RuntimeException("Deduplication Failed: " + dup.message);
            }
        }
        
        // 2. Prepare changes JSON (before and after) with calculated fields recomputed
        String beforeJson = recomputeCalculatedFields(nodeId, record.getData());
        String afterJson = recomputeCalculatedFields(nodeId, request.getData());
        String changes = String.format("{\"before\": %s, \"after\": %s}", 
            beforeJson != null && !beforeJson.isBlank() ? beforeJson : "{}", 
            afterJson != null && !afterJson.isBlank() ? afterJson : "{}");
        
        // 3. Create ApprovalRequest
        ApprovalRequest approval = new ApprovalRequest();
        approval.setTargetType("RECORD_UPDATE");
        approval.setTargetId(record.getId());
        
        record.setStatus("PENDING_APPROVAL");
        recordRepository.save(record);
        approval.setClassificationNode(record.getNode());
        approval.setRequesterId(request.getRequesterId());
        approval.setStatus("PENDING");
        approval.setChanges(changes);
        approval.setCurrentStepOrder(1);
        
        // 4. Create Steps based on dynamic request
        WorkflowConfig config = resolveWorkflow(record.getNode().getId(), "UPDATE");
        buildDynamicSteps(approval, config);
        
        // 5. Add Requester's DRAFT Step (stepOrder = 0)
        ApprovalStep draftStep = new ApprovalStep();
        draftStep.setApprovalRequest(approval);
        draftStep.setStepType("DRAFT");
        draftStep.setAssigneeId(request.getRequesterId());
        draftStep.setStepOrder(0);
        draftStep.setStatus("SUBMITTED");
        draftStep.setComment(request.getComment());
        approval.getSteps().add(draftStep);
        return approvalRepository.save(approval);
    }
    
    @Transactional
    public ApprovalRequest requestRecordDeletion(UUID recordId, RecordRequest request) {
        Record record = recordRepository.findById(recordId)
                .orElseThrow(() -> new RuntimeException("Record not found"));
        
        if ("PENDING_APPROVAL".equals(record.getStatus())) {
            throw new RuntimeException("Cannot delete a record that is pending creation approval.");
        }
        
        UUID nodeId = record.getNode().getId();
        
        // Create ApprovalRequest for DELETE
        ApprovalRequest approval = new ApprovalRequest();
        approval.setTargetType("RECORD_DELETE");
        approval.setTargetId(record.getId());
        approval.setClassificationNode(record.getNode());
        approval.setRequesterId(request.getRequesterId());
        approval.setStatus("PENDING");
        approval.setChanges(record.getData());
        approval.setCurrentStepOrder(1);
        
        // Create Steps based on dynamic request
        WorkflowConfig config = resolveWorkflow(record.getNode().getId(), "DELETE");
        buildDynamicSteps(approval, config);
        
        // Add Requester's DRAFT Step (stepOrder = 0)
        ApprovalStep draftStep = new ApprovalStep();
        draftStep.setApprovalRequest(approval);
        draftStep.setStepType("DRAFT");
        draftStep.setAssigneeId(request.getRequesterId());
        draftStep.setStepOrder(0);
        draftStep.setStatus("SUBMITTED");
        draftStep.setComment(request.getComment() != null ? request.getComment() : "Deletion requested");
        approval.getSteps().add(draftStep);
        
        // Update record status to PENDING_APPROVAL
        record.setStatus("PENDING_APPROVAL");
        recordRepository.save(record);
        
        return approvalRepository.save(approval);
    }
    
    @Transactional
    public ApprovalRequest approveStep(UUID stepId, UUID approverId, String comment) {
        ApprovalStep step = stepRepository.findById(stepId)
                .orElseThrow(() -> new RuntimeException("Step not found"));
                
        if (!step.getAssigneeId().equals(approverId)) {
            throw new RuntimeException("You are not the assignee for this step");
        }
        if (!"PENDING".equals(step.getStatus())) {
            throw new RuntimeException("Step is not pending");
        }
        
        step.setStatus("APPROVED");
        step.setComment(comment);
        stepRepository.save(step);
        
        ApprovalRequest approval = step.getApprovalRequest();
        
        // Check if all steps in the current step order are APPROVED
        boolean allApproved = approval.getSteps().stream()
                .filter(s -> s.getStepOrder().equals(approval.getCurrentStepOrder()))
                .allMatch(s -> "APPROVED".equals(s.getStatus()));
                
        if (allApproved) {
            // Find the next step order
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
                // stepRepository.save() will be handled by cascade or explicitly
                approvalRepository.save(approval);
            } else {
                // Final approval
                approval.setStatus("APPROVED");
                approvalRepository.save(approval);
            
            if ("RECORD".equals(approval.getTargetType())) {
                Record record = recordRepository.findById(approval.getTargetId())
                        .orElseThrow(() -> new RuntimeException("Record not found"));
                record.setStatus("ACTIVE");
                String finalData = recomputeCalculatedFields(record.getNode().getId(), approval.getChanges());
                record.setData(finalData);
                record.setVersion(1);
                recordRepository.save(record);
                logHistory(record, "CREATE", approval.getRequesterId(), null, finalData, approval.getId());
            } else if ("RECORD_UPDATE".equals(approval.getTargetType())) {
                Record record = recordRepository.findById(approval.getTargetId())
                        .orElseThrow(() -> new RuntimeException("Record not found"));
                try {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(approval.getChanges());
                    String prevData = record.getData();
                    if (root.has("after")) {
                        String afterData = recomputeCalculatedFields(record.getNode().getId(), root.get("after").toString());
                        record.setData(afterData);
                    }
                    record.setStatus("ACTIVE");
                    record.setVersion(record.getVersion() + 1);
                    recordRepository.save(record);
                    logHistory(record, "UPDATE", approval.getRequesterId(), prevData, record.getData(), approval.getId());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if ("RECORD_DELETE".equals(approval.getTargetType())) {
                Record record = recordRepository.findById(approval.getTargetId())
                        .orElseThrow(() -> new RuntimeException("Record not found"));
                logHistory(record, "DELETE", approval.getRequesterId(), record.getData(), null, approval.getId());
                recordRepository.delete(record);
            }
        }
        }
        
        return approval;
    }
    
    @Transactional
    public ApprovalRequest rejectStep(UUID stepId, UUID approverId, String comment) {
        ApprovalStep step = stepRepository.findById(stepId)
                .orElseThrow(() -> new RuntimeException("Step not found"));
                
        if (!step.getAssigneeId().equals(approverId)) {
            throw new RuntimeException("You are not the assignee for this step");
        }
        if (!"PENDING".equals(step.getStatus())) {
            throw new RuntimeException("Step is not pending");
        }
        
        step.setStatus("REJECTED");
        step.setComment(comment);
        stepRepository.save(step);
        
        ApprovalRequest approval = step.getApprovalRequest();
        approval.setStatus("REJECTED");
        approvalRepository.save(approval);
        
        if ("RECORD".equals(approval.getTargetType())) {
            Record record = recordRepository.findById(approval.getTargetId())
                    .orElseThrow(() -> new RuntimeException("Record not found"));
            record.setStatus("REJECTED");
            recordRepository.save(record);
        } else if ("RECORD_UPDATE".equals(approval.getTargetType())) {
            Record record = recordRepository.findById(approval.getTargetId())
                    .orElseThrow(() -> new RuntimeException("Record not found"));
            record.setStatus("ACTIVE");
            recordRepository.save(record);
        }
        
        return approval;
    }
    
    @Transactional(readOnly = true)
    public List<ApprovalRequest> getPendingRequests() {
        return approvalRepository.findByStatusOrderByCreatedAtDesc("PENDING");
    }

    @Transactional(readOnly = true)
    public List<ApprovalRequest> getAllRequests() {
        return approvalRepository.findAll();
    }
    
    @Transactional(readOnly = true)
    public List<ApprovalStep> getMyTodos(UUID assigneeId) {
        return stepRepository.findByAssigneeIdAndStatus(assigneeId, "PENDING");
    }

    @Transactional(readOnly = true)
    public List<ApprovalRequest> getMyRequests(UUID requesterId) {
        return approvalRepository.findByRequesterIdOrderByCreatedAtDesc(requesterId);
    }

    @Transactional(readOnly = true)
    public ApprovalRequest getRequestById(UUID id) {
        return approvalRepository.findById(id).orElseThrow(() -> new RuntimeException("ApprovalRequest not found"));
    }
}
