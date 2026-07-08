const fs = require('fs');
let c = fs.readFileSync('C:/dev/ai/frontend/pages/schema.vue', 'utf8').split('\n');
c.splice(169, 34,
'          text-by="text"',
'          label="Description Field (Optional)"',
'          class="mb-4 w-full"',
'          clearable',
'        />',
'      </div>');
fs.writeFileSync('C:/dev/ai/frontend/pages/schema.vue', c.join('\n'));
console.log("Success");
