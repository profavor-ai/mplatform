const fs = require('fs');
let lines = fs.readFileSync('C:/dev/ai/frontend/pages/schema.vue', 'utf8').split('\n');

const missingLines = `            &nbsp;&nbsp;• <code>CEIL(값)</code> : 올림<br/>
            &nbsp;&nbsp;• <code>FLOOR(값)</code> : 내림<br/>
            &nbsp;&nbsp;• <code>ABS(값)</code> : 절대값<br/>
          <span style="color: #d9534f; font-weight: bold;">주의:</span> 참조하는 필드는 반드시 숫자(NUMBER, DECIMAL, FLOAT, INTEGER)이거나 다른 계산 필드(CALCULATED)여야 합니다.
        </va-alert>
      </div>`;

lines.splice(250, 0, ...missingLines.split('\n'));
fs.writeFileSync('C:/dev/ai/frontend/pages/schema.vue', lines.join('\n'));
