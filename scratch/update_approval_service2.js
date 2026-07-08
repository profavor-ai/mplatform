const fs = require('fs')
const path = require('path')

const file = path.join('C:\\dev\\ai\\backend\\src\\main\\java\\com\\classification\\domain_system\\service\\ApprovalService.java')
let content = fs.readFileSync(file, 'utf-8')

// Replace buildDynamicSteps method
const startIndex = content.indexOf('private void buildDynamicSteps(ApprovalRequest approval, com.classification.domain_system.dto.RecordRequest request) {')
const endIndex = content.indexOf('}', content.indexOf('approval.setObserverIds("[]");', startIndex) + 30) + 1 // Find end of buildDynamicSteps

const newBuildDynamicSteps = `private void buildDynamicSteps(ApprovalRequest approval, WorkflowConfig config) {
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
    }`

if (startIndex !== -1) {
    content = content.substring(0, startIndex) + newBuildDynamicSteps + content.substring(endIndex)
}

// Replace method calls
content = content.replace(/buildDynamicSteps\(approval, request\);/g, 'buildDynamicSteps(approval, config);')

fs.writeFileSync(file, content)
console.log('Successfully updated ApprovalService.java')
