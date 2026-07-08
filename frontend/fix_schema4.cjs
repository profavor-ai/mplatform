const fs = require('fs');
let lines = fs.readFileSync('C:/dev/ai/frontend/pages/schema.vue', 'utf8').split('\n');

const toReplaceStart = 256;
// Find where the Domain modal starts
let toReplaceEnd = 256;
for (let i = 256; i < lines.length; i++) {
  if (lines[i].includes('<!-- Domain Modal -->')) {
    toReplaceEnd = i;
    break;
  }
}

const missingCode = `      <div v-if="['NUMBER', 'DECIMAL', 'FLOAT', 'INTEGER', 'CALCULATED'].includes(newField.type)" class="mb-4 w-full">
        <va-select
          v-model="newField.unit"
          :options="unitOptions"
          label="Unit (단위)"
          class="w-full"
          clearable
          allow-create="unique"
          :no-options-text="'직접 입력 가능'"
        />
      </div>

      <div style="display: flex; gap: 1rem; margin-top: 1rem; flex-wrap: wrap;">
        <va-checkbox v-model="newField.required" :label="currentLocale === 'ko' ? '필수' : 'Required'" />
        <va-checkbox v-model="newField.isMultiValue" :label="currentLocale === 'ko' ? '다중 값' : 'Multi-Value'" />
        <va-checkbox v-model="newField.isSearchable" :label="currentLocale === 'ko' ? '검색 가능' : 'Searchable'" />
        <va-checkbox v-model="newField.isEncrypted" :label="currentLocale === 'ko' ? '암호화' : 'Encrypted'" />
        <va-checkbox v-model="newField.isReadOnly" :label="currentLocale === 'ko' ? '읽기 전용' : 'Read-Only'" />
        <va-checkbox v-model="newField.isImmutable" :label="currentLocale === 'ko' ? '수정 금지' : 'Immutable'" />
        <va-checkbox v-model="newField.isHidden" :label="currentLocale === 'ko' ? '숨김' : 'Hidden'" />
      </div>

      <div style="display: flex; justify-content: flex-end; gap: 1rem; margin-top: 1.5rem;">
        <va-button preset="secondary" @click="showFieldModal = false">Cancel</va-button>
        <va-button @click="saveField">{{ isEditMode ? 'Save' : 'Create' }}</va-button>
      </div>
    </va-modal>

        <va-card v-else>
          <va-card-content style="text-align: center; padding: 3rem; color: #666;">
            Select a Classification Node from the tree to view or add fields.
          </va-card-content>
        </va-card>
      </div>
    </div>`;

lines.splice(toReplaceStart, toReplaceEnd - toReplaceStart, ...missingCode.split('\n'));
fs.writeFileSync('C:/dev/ai/frontend/pages/schema.vue', lines.join('\n'));
