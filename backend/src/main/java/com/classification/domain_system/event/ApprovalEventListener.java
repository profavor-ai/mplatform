package com.classification.domain_system.event;

import com.classification.domain_system.entity.ApprovalRequest;
import com.classification.domain_system.entity.ApprovalStep;
import com.classification.domain_system.entity.Record;
import com.classification.domain_system.entity.RecordHistory;
import com.classification.domain_system.entity.FieldDefinition;
import com.classification.domain_system.repository.*;
import com.classification.domain_system.service.FieldDefinitionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class ApprovalEventListener {

    private final ApprovalRequestRepository approvalRepository;
    private final ApprovalStepRepository stepRepository;
    private final RecordRepository recordRepository;
    private final RecordHistoryRepository recordHistoryRepository;
    private final FieldDefinitionService fieldDefinitionService;

    @EventListener
    @Transactional
    public void onApprovalRequestCreated(ApprovalRequestCreatedEvent event) {
        ApprovalRequest approval = event.getApprovalRequest();
        System.out.println("[EVENT_DRIVEN] Received ApprovalRequestCreatedEvent for Request ID: " + approval.getId());
        
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
        ApprovalRequest approval = event.getApprovalRequest();
        ApprovalStep approvedStep = event.getApprovedStep();
        System.out.println("[EVENT_DRIVEN] Received ApprovalStepApprovedEvent for Request ID: " + approval.getId() 
                + ", Approved Step Order: " + approvedStep.getStepOrder());
        
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
            String finalData = recomputeCalculatedFields(record.getNode().getId(), approval.getChanges());
            record.setData(finalData);
            record.setVersion(1);
            recordRepository.save(record);
            logHistory(record, "CREATE", approval.getRequesterId(), null, finalData, approval.getId());
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

    private String recomputeCalculatedFields(UUID nodeId, String dataJson) {
        if (dataJson == null || dataJson.isBlank()) return dataJson;
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> data = mapper.readValue(dataJson, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
            List<FieldDefinition> fields = fieldDefinitionService.getEffectiveFields(nodeId);
            for (FieldDefinition field : fields) {
                if ("CALCULATED".equals(field.getType()) && field.getOptions() != null) {
                    try {
                        JsonNode opts = mapper.readTree(field.getOptions());
                        if (opts.has("formula")) {
                            String formula = opts.get("formula").asText();
                            Double result = evaluateFormula(formula, data);
                            if (result != null && !result.isNaN() && !result.isInfinite()) {
                                data.put(field.getKey(), result);
                            }
                        }
                    } catch (Exception e) {
                    }
                }
            }
            return mapper.writeValueAsString(data);
        } catch (Exception e) {
            return dataJson;
        }
    }

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

    private double evalExpr(String expr) {
        return new Object() {
            int pos = 0;
            double parse() {
                return parseAddSub();
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
                for (String fn : new String[]{"CEIL", "FLOOR", "ROUND", "ABS"}) {
                    if (pos + fn.length() <= expr.length() && expr.substring(pos, pos + fn.length()).equals(fn)) {
                        pos += fn.length();
                        skipSpaces();
                        if (pos < expr.length() && expr.charAt(pos) == '(') {
                            pos++;
                            double val = parseAddSub();
                            double decimals = 0;
                            skipSpaces();
                            if (pos < expr.length() && expr.charAt(pos) == ',') {
                                pos++;
                                decimals = parseAddSub();
                            }
                            skipSpaces();
                            if (pos < expr.length() && expr.charAt(pos) == ')') pos++;
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
                    pos++;
                    double val = parseAddSub();
                    skipSpaces();
                    if (pos < expr.length() && expr.charAt(pos) == ')') pos++;
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
}
