const fs = require('fs');
let content = fs.readFileSync('pages/schema.vue', 'utf8');
let lines = content.split('\n');

lines[81] = '                      <h4 style=\"font-size: 0.9rem; margin-bottom: 0.25rem; color: #555;\">결재선</h4>';
lines[82] = '                      <div v-if=\"workflowConfigs[action].steps.length === 0\" style=\"color: #999; font-size: 0.85rem; padding: 0.5rem 0;\">결재 단계를 추가해주세요.</div>';

fs.writeFileSync('pages/schema.vue', lines.join('\n'), 'utf8');

