const fs = require('fs');
const file = 'c:/dev/ai/frontend/pages/approvals.vue';
let lines = fs.readFileSync(file, 'utf8').split('\n');

const insertText = `  }
}

const myUuid = computed(() => {
  let uid = currentUser.value?.uuid
  if (!uid || uid === 'test-admin-uuid') {
     uid = '935102dc-bccf-47e0-8d65-0d883186472f' // Using the mock UUID we also use in dashboard
  }
  return uid
})

const getParsedChanges = (changesString) => {
  if (!changesString) return null
  try {
    const parsed = JSON.parse(changesString)
    if (Object.keys(parsed).length === 0) return null
    return parsed
  } catch (e) {
    return null
  }
}

const formatDate = (dateString) => {
  if (!dateString) return ''
  const date = new Date(dateString)
  return date.toLocaleString()`.split('\n');

// Find the line that has "domainRefDisplayMap.value[uuid] = (uname && uname !== uuid) ? uname : uuid;"
const idx1 = lines.findIndex(l => l.includes('domainRefDisplayMap.value[uuid] = (uname && uname !== uuid) ? uname : uuid;'));

// Find the line that has "return date.toLocaleString()" after idx1
const idx2 = lines.findIndex((l, i) => i > idx1 && l.includes('return date.toLocaleString()'));

// Replace lines between idx1 + 1 and idx2 (inclusive)
lines.splice(idx1 + 1, idx2 - idx1, ...insertText);

fs.writeFileSync(file, lines.join('\n'), 'utf8');
console.log('Fixed completely');
