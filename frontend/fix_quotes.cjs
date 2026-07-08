const fs = require('fs');
let content = fs.readFileSync('pages/schema.vue', 'utf8');
let lines = content.split('\n');
lines[258] = '        <va-checkbox v-model=\"newField.required\" :label=\"currentLocale === \'ko\' ? \'필수\' : \'Required\'\" />';
lines[259] = '        <va-checkbox v-model=\"newField.isMultiValue\" :label=\"currentLocale === \'ko\' ? \'다중 값\' : \'Multi-Value\'\" />';
lines[260] = '        <va-checkbox v-model=\"newField.isSearchable\" :label=\"currentLocale === \'ko\' ? \'검색 가능\' : \'Searchable\'\" />';
lines[261] = '        <va-checkbox v-model=\"newField.isEncrypted\" :label=\"currentLocale === \'ko\' ? \'암호화\' : \'Encrypted\'\" />';
fs.writeFileSync('pages/schema.vue', lines.join('\n'), 'utf8');
console.log('Fixed quotes');

