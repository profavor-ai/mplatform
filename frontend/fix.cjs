const fs = require('fs');
let content = fs.readFileSync('pages/schema.vue', 'utf8');

// First, read lines
let lines = content.split('\n');
lines[98] = '                          <va-button size=\"small\" preset=\"secondary\" icon=\"person_add\" @click=\"addUserToStep(action, sIdx)\" style=\"font-size: 0.75rem; padding: 0.25rem 0.5rem; height: 24px;\">병렬 추가</va-button>';
lines[101] = '                      <va-button size=\"small\" preset=\"secondary\" icon=\"add\" @click=\"addStep(action)\">단계 추가</va-button>';
lines[105] = '                      <h4 style=\"font-size: 0.9rem; margin-bottom: 0.25rem; color: #555;\">통보자 지정</h4>';
lines[112] = '                        placeholder=\"통보자 다중 선택\"';

fs.writeFileSync('pages/schema.vue', lines.join('\n'), 'utf8');
console.log('Fixed');

