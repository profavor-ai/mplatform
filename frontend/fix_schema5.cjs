const fs = require('fs');
let lines = fs.readFileSync('C:/dev/ai/frontend/pages/schema.vue', 'utf8').split('\n');

const alertContent = `          <strong>{{ currentLocale === 'ko' ? '수식 작성 가이드' : 'Formula Guide' }}</strong><br/>
          - <strong>필드 참조</strong>: <code>\${필드_KEY}</code> 형식으로 입력하세요. (예: <code>\${PRICE}</code>)<br/>
          - <strong>기본 연산</strong>: <code>+</code> (더하기), <code>-</code> (빼기), <code>*</code> (곱하기), <code>/</code> (나누기)<br/>
          - <strong>수학 함수</strong>:<br/>
            &nbsp;&nbsp;• <code>ROUND(값, 자리수)</code> : 반올림(예: <code>ROUND(\${PRICE}, 2)</code>)<br/>
            &nbsp;&nbsp;• <code>CEIL(값)</code> : 올림<br/>
            &nbsp;&nbsp;• <code>FLOOR(값)</code> : 내림<br/>
            &nbsp;&nbsp;• <code>ABS(값)</code> : 절대값<br/>
          <span style="color: #d9534f; font-weight: bold;">주의:</span> 참조하는 필드는 반드시 숫자(NUMBER, DECIMAL, FLOAT, INTEGER)이거나 다른 계산 필드(CALCULATED)여야 합니다.`;

// Replace lines 246 to 254 (inclusive)
lines.splice(245, 10, ...alertContent.split('\n'));
fs.writeFileSync('C:/dev/ai/frontend/pages/schema.vue', lines.join('\n'));
