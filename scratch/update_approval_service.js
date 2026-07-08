const fs = require('fs')
const path = require('path')

const file = path.join('C:\\dev\\ai\\backend\\src\\main\\java\\com\\classification\\domain_system\\service\\ApprovalService.java')
let content = fs.readFileSync(file, 'utf-8')

const oldBuildDynamicSteps = `    private void buildDynamicSteps(ApprovalRequest approval, com.classification.domain_system.dto.RecordRequest request) {
        List<ApprovalStep> steps = new java.util.ArrayList<>();
        if (request.getSteps() != null) {
            for (com.classification.domain_system.dto.ApprovalStepRequest reqStep : request.getSteps()) {
                ApprovalStep step = new ApprovalStep();
                step.setApprovalRequest(approval);
                step.setStepType(reqStep.getStepType());
                step.setAssigneeId(reqStep.getAssigneeId());
                step.setStepOrder(reqStep.getStepOrder());
                step.setStatus("PENDING");
                steps.add(step);
            }
        }
        approval.setSteps(steps);
        
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            if (request.getObserverIds() != null && !request.getObserverIds().isEmpty()) {
                approval.setObserverIds(mapper.writeValueAsString(request.getObserverIds()));
            } else {
                approval.setObserverIds("[]");
            }
        } catch (Exception e) {
            approval.setObserverIds("[]");
        }
    }`

const newBuildDynamicSteps = `    private void buildDynamicSteps(ApprovalRequest approval, WorkflowConfig config) {
        List<ApprovalStep> steps = new java.util.ArrayList<>();
        try {
            if (config != null && config.getStepsConfig() != null) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(config.getStepsConfig());
                
                if (root.has("steps") && root.get("steps").isArray()) {
                    for (com.fasterxml.jackson.databind.JsonNode stepNode : root.get("steps")) {
                        ApprovalStep step = new ApprovalStep();
                        step.setApprovalRequest(approval);
                        step.setStepType(stepNode.get("stepType").asText());
                        step.setAssigneeId(UUID.fromString(stepNode.get("assigneeId").asText()));
                        step.setStepOrder(stepNode.get("stepOrder").asInt());
                        step.setStatus("PENDING");
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
    }`

content = content.replace(oldBuildDynamicSteps, newBuildDynamicSteps)

// Now we need to update the calls to buildDynamicSteps
content = content.replace(/buildDynamicSteps\(approval, request\);/g, 'buildDynamicSteps(approval, config);')

fs.writeFileSync(file, content)
console.log('Updated ApprovalService.java')
