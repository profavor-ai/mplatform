const fs = require('fs');
const file = 'c:/dev/ai/frontend/pages/approvals.vue';
let content = fs.readFileSync(file, 'utf8');

// The problematic block is around line 543
content = content.replace(
`  } catch (e) {
    const uname = getUserName(uuid);
    domainRefDisplayMap.value[uuid] = (uname && uname !== uuid) ? uname : uuid;
  }
  }
  return uid
})

const getParsedChanges = (changesString) => {`,
`  } catch (e) {
    const uname = getUserName(uuid);
    domainRefDisplayMap.value[uuid] = (uname && uname !== uuid) ? uname : uuid;
  }
}

const myUuid = computed(() => {
  let uid = currentUser.value?.uuid
  if (!uid || uid === 'test-admin-uuid') {
     uid = '935102dc-bccf-47e0-8d65-0d883186472f' // Using the mock UUID we also use in dashboard
  }
  return uid
})

const getParsedChanges = (changesString) => {`
);

fs.writeFileSync(file, content, 'utf8');
console.log('Fixed');
