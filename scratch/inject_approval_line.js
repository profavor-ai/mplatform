const fs = require('fs')
const path = require('path')

const file = path.join('C:\\dev\\ai\\frontend\\pages\\records.vue')
let content = fs.readFileSync(file, 'utf-8')

// 1. Add modal to template
const modalTemplate = `
    <!-- Approval Line Configuration Modal -->
    <va-modal v-model="showApprovalLineModal" title="결재선 지정" hide-default-actions size="large">
      <div style="padding: 1rem;">
        <div style="margin-bottom: 1rem;">
          <h4 style="margin-bottom: 0.5rem; border-bottom: 1px solid #ccc; padding-bottom: 0.25rem;">결재 단계 설정</h4>
          <div v-for="(step, sIdx) in approvalSteps" :key="step.id" style="border: 1px solid #ddd; padding: 1rem; border-radius: 4px; margin-bottom: 1rem; background: #fafafa;">
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.5rem;">
              <strong>{{ sIdx + 1 }}단계</strong>
              <div style="display: flex; gap: 0.5rem;">
                <va-button size="small" color="primary" preset="secondary" icon="arrow_upward" @click="moveStepUp(sIdx)" :disabled="sIdx === 0"></va-button>
                <va-button size="small" color="primary" preset="secondary" icon="arrow_downward" @click="moveStepDown(sIdx)" :disabled="sIdx === approvalSteps.length - 1"></va-button>
                <va-button size="small" color="danger" preset="secondary" icon="delete" @click="removeStep(sIdx)"></va-button>
              </div>
            </div>
            <div v-for="(u, uIdx) in step.users" :key="uIdx" style="display: flex; gap: 1rem; align-items: center; margin-bottom: 0.5rem;">
              <va-select style="flex: 1;" v-model="u.stepType" :options="['CONSENSUS', 'APPROVAL']" placeholder="결재 유형" />
              <va-select style="flex: 2;" v-model="u.assigneeId" :options="userList" value-by="id" text-by="name" placeholder="사용자 선택" />
              <va-button size="small" color="danger" preset="secondary" icon="close" @click="removeUserFromStep(sIdx, uIdx)"></va-button>
            </div>
            <va-button size="small" preset="secondary" icon="add" @click="addUserToStep(sIdx)">이 단계에 병렬 결재자 추가</va-button>
          </div>
          <va-button size="small" color="primary" icon="add" @click="addStep">다음 단계 추가</va-button>
        </div>

        <div>
          <h4 style="margin-bottom: 0.5rem; border-bottom: 1px solid #ccc; padding-bottom: 0.25rem;">통보자(참조) 설정</h4>
          <va-select v-model="approvalObserverIds" :options="userList" value-by="id" text-by="name" placeholder="통보자 선택" multiple />
        </div>
      </div>
      <div style="display: flex; justify-content: flex-end; gap: 0.5rem; margin-top: 1.5rem;">
        <va-button color="gray" preset="secondary" @click="showApprovalLineModal = false">취소</va-button>
        <va-button @click="submitWithApprovalLine">상신하기</va-button>
      </div>
    </va-modal>
`

if (!content.includes('showApprovalLineModal = false">취소</va-button>')) {
  // Insert before the last closing </va-modal> ? Or just before </template>
  content = content.replace('</template>', modalTemplate + '\n</template>')
}

// 2. Add script variables
const scriptVars = `
const showApprovalLineModal = ref(false)
const approvalLineAction = ref('') // 'CREATE', 'UPDATE', 'DELETE'
const approvalSteps = ref([
  { id: Date.now(), users: [{ assigneeId: '', stepType: 'CONSENSUS' }] }
])
const approvalObserverIds = ref([])

const addStep = () => {
  approvalSteps.value.push({ id: Date.now() + Math.random(), users: [{ assigneeId: '', stepType: 'APPROVAL' }] })
}
const removeStep = (idx) => approvalSteps.value.splice(idx, 1)
const moveStepUp = (idx) => {
  if (idx === 0) return
  const temp = approvalSteps.value[idx]
  approvalSteps.value[idx] = approvalSteps.value[idx - 1]
  approvalSteps.value[idx - 1] = temp
}
const moveStepDown = (idx) => {
  if (idx === approvalSteps.value.length - 1) return
  const temp = approvalSteps.value[idx]
  approvalSteps.value[idx] = approvalSteps.value[idx + 1]
  approvalSteps.value[idx + 1] = temp
}
const addUserToStep = (sIdx) => approvalSteps.value[sIdx].users.push({ assigneeId: '', stepType: 'APPROVAL' })
const removeUserFromStep = (sIdx, uIdx) => {
  approvalSteps.value[sIdx].users.splice(uIdx, 1)
  if (approvalSteps.value[sIdx].users.length === 0) removeStep(sIdx)
}

const buildStepsPayload = () => {
  const steps = []
  approvalSteps.value.forEach((step, sIdx) => {
    step.users.forEach(u => {
      if (u.assigneeId) {
        steps.push({
          stepType: u.stepType,
          assigneeId: u.assigneeId,
          stepOrder: sIdx + 1
        })
      }
    })
  })
  return steps
}

const submitWithApprovalLine = async () => {
  const payloadSteps = buildStepsPayload()
  if (payloadSteps.length === 0) {
    alert('최소 1명의 결재자/합의자를 지정해주세요.')
    return
  }
  
  const payload = {
    requesterId: currentUser.value?.uuid || '123e4567-e89b-12d3-a456-426614174000',
    steps: payloadSteps,
    observerIds: approvalObserverIds.value
  }

  try {
    if (approvalLineAction.value === 'CREATE') {
      const dataToSave = { ...recordFormData.value }
      nodeFields.value?.forEach(field => {
        if (field.type === 'IMAGE' && newImages.value[field.key]) {
          dataToSave[field.key] = newImages.value[field.key].url
        }
      })
      payload.data = JSON.stringify(formatDataForSave(dataToSave))
      await $fetch(\`/api/nodes/\${selectedNode.value.id}/records\`, {
        method: 'POST',
        headers: { Authorization: \`Bearer \${token.value}\` },
        body: payload
      })
      alert('신규 기안이 성공적으로 상신되었습니다.')
      showCreateModal.value = false
    } else if (approvalLineAction.value === 'UPDATE') {
      payload.data = JSON.stringify(formatDataForSave(selectedRecordData.value))
      await $fetch(\`/api/records/\${selectedRecordId.value}/update-request\`, {
        method: 'POST',
        headers: { Authorization: \`Bearer \${token.value}\` },
        body: payload
      })
      alert('수정 기안이 성공적으로 상신되었습니다.')
      isEditingRecord.value = false
      showDetailModal.value = false
    } else if (approvalLineAction.value === 'DELETE') {
      payload.data = "{}"
      await $fetch(\`/api/records/\${selectedRecordId.value}/delete-request\`, {
        method: 'POST',
        headers: { Authorization: \`Bearer \${token.value}\` },
        body: payload
      })
      alert('삭제 기안이 성공적으로 상신되었습니다.')
      showDetailModal.value = false
    }
    
    showApprovalLineModal.value = false
    await fetchRecords()
  } catch (e) {
    alert('결재 상신 실패: ' + (e.response?._data?.message || e.message || String(e)))
  }
}
`

if (!content.includes('const showApprovalLineModal = ref(false)')) {
  content = content.replace('const isFetching = ref(false)', 'const isFetching = ref(false)\n' + scriptVars)
}

// 3. Modify saveNewRecord, saveEditedRecord, requestDeleteRecord to open modal instead if workflow enabled
content = content.replace(
  /const saveNewRecord = async \(\) => {[\s\S]*?showCreateModal\.value = false\s+await fetchRecords\(\)\s+} catch \(error\) {/,
  `const saveNewRecord = async () => {
  if (targetDomain.value?.workflowEnabled) {
    approvalLineAction.value = 'CREATE'
    showApprovalLineModal.value = true
    return
  }
  // Original create logic
  try {
    let reqId = currentUser.value?.uuid || '123e4567-e89b-12d3-a456-426614174000'
    const dataToSave = { ...recordFormData.value }
    nodeFields.value?.forEach(field => {
      if (field.type === 'IMAGE' && newImages.value[field.key]) {
        dataToSave[field.key] = newImages.value[field.key].url
      }
    })
    const payload = { data: JSON.stringify(formatDataForSave(dataToSave)), requesterId: reqId }
    await $fetch(\`/api/nodes/\${selectedNode.value.id}/records\`, {
      method: 'POST',
      headers: { Authorization: \`Bearer \${token.value}\` },
      body: payload
    })
    showCreateModal.value = false
    await fetchRecords()
  } catch (error) {`
)

content = content.replace(
  /const saveEditedRecord = async \(\) => {[\s\S]*?isEditingRecord\.value = false\s+showDetailModal\.value = false\s+alert\('Record update request submitted successfully for approval\.'\)\s+await fetchRecords\(\)\s+} catch \(e\) {/,
  `const saveEditedRecord = async () => {
  if (targetDomain.value?.workflowEnabled) {
    approvalLineAction.value = 'UPDATE'
    showApprovalLineModal.value = true
    return
  }
  // Original update logic
  try {
    let reqId = currentUser.value?.uuid || '123e4567-e89b-12d3-a456-426614174000'
    const payload = { requesterId: reqId, data: JSON.stringify(formatDataForSave(selectedRecordData.value)) }
    await $fetch(\`/api/records/\${selectedRecordId.value}/update-request\`, {
      method: 'POST',
      headers: { Authorization: \`Bearer \${token.value}\` },
      body: payload
    })
    isEditingRecord.value = false
    showDetailModal.value = false
    alert('Record update request submitted successfully for approval.')
    await fetchRecords()
  } catch (e) {`
)

content = content.replace(
  /const requestDeleteRecord = async \(\) => {[\s\S]*?showDetailModal\.value = false\s+alert\('Record deletion request submitted successfully for approval\.'\)\s+await fetchRecords\(\)\s+} catch \(e\) {/,
  `const requestDeleteRecord = async () => {
  if (!confirm('Are you sure you want to request deletion for this record?')) return
  if (targetDomain.value?.workflowEnabled) {
    approvalLineAction.value = 'DELETE'
    showApprovalLineModal.value = true
    return
  }
  // Original delete logic
  try {
    let reqId = currentUser.value?.uuid || '123e4567-e89b-12d3-a456-426614174000'
    const payload = { requesterId: reqId, data: "{}" }
    await $fetch(\`/api/records/\${selectedRecordId.value}/delete-request\`, {
      method: 'POST',
      headers: { Authorization: \`Bearer \${token.value}\` },
      body: payload
    })
    showDetailModal.value = false
    alert('Record deletion request submitted successfully for approval.')
    await fetchRecords()
  } catch (e) {`
)


fs.writeFileSync(file, content)
console.log('Successfully injected Approval Line Config Modal to records.vue')
