const fs = require('fs');
const file = 'c:/dev/ai/frontend/pages/approvals.vue';
const lines = fs.readFileSync(file, 'utf8').split('\n');

// Drop the first 129 lines
const newLines = lines.slice(129);

fs.writeFileSync(file, newLines.join('\n'), 'utf8');
console.log('Fixed file');
