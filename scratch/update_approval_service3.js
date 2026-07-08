const fs = require('fs')
const path = require('path')

const file = path.join('C:\\dev\\ai\\backend\\src\\main\\java\\com\\classification\\domain_system\\service\\ApprovalService.java')
let content = fs.readFileSync(file, 'utf-8')

content = content.replace(/findWorkflowConfig/g, 'resolveWorkflow')

fs.writeFileSync(file, content)
console.log('Successfully updated ApprovalService.java')
