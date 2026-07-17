const fs = require('fs');
const content = fs.readFileSync('c:/dev/ai/frontend/pages/schema.vue', 'utf8');
const idx = content.indexOf('</style>');
if (idx !== -1) {
    const newContent = content.substring(0, idx + 8) + `

<style scoped>
.mb-4 { margin-bottom: 1rem; }
.w-full { width: 100%; }

/* Tree Container */
:deep(.va-tree) {
  overflow-x: hidden;
}

/* Compact Form Styles for Modals */
.compact-form :deep(.va-input-wrapper__container),
.compact-form :deep(.va-input-wrapper__field),
.compact-form :deep(.va-input-wrapper__text),
.compact-form :deep(.va-select__container),
.compact-form :deep(.va-select__field) {
  min-height: 28px !important;
  height: 28px !important;
  padding-top: 0 !important;
  padding-bottom: 0 !important;
  display: flex !important;
  align-items: center !important;
}
.compact-form :deep(input),
.compact-form :deep(.va-input__content) {
  height: 100% !important;
  min-height: 28px !important;
  line-height: 28px !important;
  padding-top: 0 !important;
  padding-bottom: 0 !important;
  box-sizing: border-box !important;
}
.compact-form :deep(.va-select__content) {
  padding-top: 0 !important;
  padding-bottom: 0 !important;
  min-height: 28px !important;
  height: 28px !important;
  display: flex !important;
  align-items: center !important;
}
.compact-form :deep(.va-select-option-list) {
  max-height: 200px;
}
</style>
`;
    fs.writeFileSync('c:/dev/ai/frontend/pages/schema.vue', newContent, 'utf8');
}
