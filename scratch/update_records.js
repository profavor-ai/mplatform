const fs = require('fs')
const path = require('path')

const file = path.join('C:\\dev\\ai\\frontend\\pages\\records.vue')
let content = fs.readFileSync(file, 'utf-8')

// Remove <va-modal v-model="showApprovalLineModal" ...> ... </va-modal>
const modalStart = content.indexOf('<!-- Approval Line Configuration Modal -->')
const modalEnd = content.indexOf('</va-modal>', modalStart) + 11
if (modalStart !== -1 && modalEnd > modalStart) {
    content = content.substring(0, modalStart) + content.substring(modalEnd)
}

// Replace saveNewRecord
const saveNewStart = content.indexOf('const saveNewRecord = async () => {')
const saveNewEnd = content.indexOf('const saveEditedRecord = async () => {')
if (saveNewStart !== -1) {
    const originalSaveNew = `const saveNewRecord = async () => {
  if (!selectedNode.value) return
  if (targetDomain.value?.workflowEnabled) {
    approvalLineAction.value = 'CREATE'
    showApprovalLineModal.value = true
    return
  }
  // Original save logic
  try {
    let reqId = currentUser.value?.uuid || '123e4567-e89b-12d3-a456-426614174000'
    const payload = { requesterId: reqId, data: JSON.stringify(formatDataForSave(newRecordData.value)) }
    await $fetch(\`/api/records/node/\${selectedNode.value.id}/create-request\`, {
      method: 'POST',
      headers: { Authorization: \`Bearer \${token.value}\` },
      body: payload
    })
    showCreateModal.value = false
    alert('Record creation request submitted successfully for approval.')
    await fetchRecords()
  } catch (e) {
    console.error(e)
    alert('Failed to submit creation request')
  }
}`
    const newSaveNew = `const saveNewRecord = async () => {
  if (!selectedNode.value) return
  
  try {
    let reqId = currentUser.value?.uuid || '123e4567-e89b-12d3-a456-426614174000'
    const payload = { requesterId: reqId, data: JSON.stringify(formatDataForSave(newRecordData.value)) }
    
    if (targetDomain.value?.workflowEnabled) {
      await $fetch(\`/api/records/node/\${selectedNode.value.id}/create-request\`, {
        method: 'POST',
        headers: { Authorization: \`Bearer \${token.value}\` },
        body: payload
      })
      alert('Record creation request submitted successfully for approval.')
    } else {
      await $fetch(\`/api/records/node/\${selectedNode.value.id}\`, {
        method: 'POST',
        headers: { Authorization: \`Bearer \${token.value}\` },
        body: payload
      })
      alert('Record created successfully.')
    }
    
    showCreateModal.value = false
    await fetchRecords()
  } catch (e) {
    console.error(e)
    alert('Failed to create record')
  }
}`
    content = content.replace(originalSaveNew, newSaveNew)
}

// Replace saveEditedRecord
const saveEditStart = content.indexOf('const saveEditedRecord = async () => {')
if (saveEditStart !== -1) {
    const originalSaveEdit = `const saveEditedRecord = async () => {
  if (!selectedRecordId.value) return
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
  } catch (e) {
    console.error(e)
    alert('Failed to submit update request')
  }
}`
    const newSaveEdit = `const saveEditedRecord = async () => {
  if (!selectedRecordId.value) return
  
  try {
    let reqId = currentUser.value?.uuid || '123e4567-e89b-12d3-a456-426614174000'
    const payload = { requesterId: reqId, data: JSON.stringify(formatDataForSave(selectedRecordData.value)) }
    
    if (targetDomain.value?.workflowEnabled) {
      await $fetch(\`/api/records/\${selectedRecordId.value}/update-request\`, {
        method: 'POST',
        headers: { Authorization: \`Bearer \${token.value}\` },
        body: payload
      })
      alert('Record update request submitted successfully for approval.')
    } else {
      await $fetch(\`/api/records/\${selectedRecordId.value}\`, {
        method: 'PUT',
        headers: { Authorization: \`Bearer \${token.value}\` },
        body: payload
      })
      alert('Record updated successfully.')
    }
    
    isEditingRecord.value = false
    showDetailModal.value = false
    await fetchRecords()
  } catch (e) {
    console.error(e)
    alert('Failed to update record')
  }
}`
    content = content.replace(originalSaveEdit, newSaveEdit)
}

// Replace requestDeleteRecord
const deleteStart = content.indexOf('const requestDeleteRecord = async () => {')
if (deleteStart !== -1) {
    const originalDelete = `const requestDeleteRecord = async () => {
  if (!selectedRecordId.value) return
  if (!confirm('Are you sure you want to delete this record?')) return
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
  } catch (e) {
    console.error(e)
    alert('Failed to submit deletion request')
  }
}`
    const newDelete = `const requestDeleteRecord = async () => {
  if (!selectedRecordId.value) return
  if (!confirm('Are you sure you want to delete this record?')) return
  
  try {
    let reqId = currentUser.value?.uuid || '123e4567-e89b-12d3-a456-426614174000'
    const payload = { requesterId: reqId, data: "{}" }
    
    if (targetDomain.value?.workflowEnabled) {
      await $fetch(\`/api/records/\${selectedRecordId.value}/delete-request\`, {
        method: 'POST',
        headers: { Authorization: \`Bearer \${token.value}\` },
        body: payload
      })
      alert('Record deletion request submitted successfully for approval.')
    } else {
      await $fetch(\`/api/records/\${selectedRecordId.value}\`, {
        method: 'DELETE',
        headers: { Authorization: \`Bearer \${token.value}\` }
      })
      alert('Record deleted successfully.')
    }
    
    showDetailModal.value = false
    await fetchRecords()
  } catch (e) {
    console.error(e)
    alert('Failed to delete record')
  }
}`
    content = content.replace(originalDelete, newDelete)
}

// Remove submitWithApprovalLine entirely
const submitStart = content.indexOf('const submitWithApprovalLine = async () => {')
const submitEnd = content.indexOf('} catch (e) {', submitStart)
const finalEnd = content.indexOf('}', submitEnd) + 1
if (submitStart !== -1 && finalEnd > submitStart) {
    content = content.substring(0, submitStart) + content.substring(finalEnd)
}

fs.writeFileSync(file, content)
console.log('Successfully updated records.vue')
