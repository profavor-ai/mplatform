const fs = require('fs');
let lines = fs.readFileSync('C:/dev/ai/frontend/pages/schema.vue', 'utf8').split('\n');

const patchCode = `    } // end for wf
  } catch(e) {
    console.error('Failed to load node data', e)
  }
  
  try {
    fields.value = await $fetch(\`http://localhost:8080/api/nodes/\${node.id}/fields\`, { headers: getAuthHeaders() })
  } catch(e) {
    console.error('Failed to load fields', e)
    fields.value = []
  }
}

const handleNodeEdit = () => {
  if (!selectedNode.value) return
  isEditMode.value = true
  if (selectedNode.value.isDomain) {
    newDomain.value = { ...selectedNode.value }
    showDomainModal.value = true
  } else {
    newNode.value = { ...selectedNode.value }
    showNodeModal.value = true
  }
}

const openDomainModal = () => {
  isEditMode.value = false
  newDomain.value = { name: {ko:'', en:''}, description: {ko:'', en:''}, identifierFieldId: null, displayNameFieldId: null, descriptionFieldId: null }
  showDomainModal.value = true
}

const openNodeModal = () => {
  if (!selectedNode.value || !selectedNode.value.isDomain) return
  isEditMode.value = false
  newNode.value = { name: {ko:'', en:''}, order: 0 }
  showNodeModal.value = true
}

const openFieldModal = (rowData = null) => {
  if (rowData) {
    isEditMode.value = true
    editingId.value = rowData.id
    newField.value = { ...rowData, name: { ...rowData.name }, formula: rowData.formula || '', unit: rowData.unit || '' }
    if (['SELECT', 'MULTI_SELECT'].includes(rowData.type)) {
      try {
        newFieldOptionsList.value = JSON.parse(rowData.options || '[]')
      } catch (e) { newFieldOptionsList.value = [] }
    } else {
      newFieldOptionsList.value = []
    }
  } else {
    isEditMode.value = false
    editingId.value = null
    newField.value = { name: {ko:'', en:''}, key: '', type: 'STRING', required: false, order: 0, fieldGroupId: null, targetDomainId: null, isMultiValue: false, isSearchable: true, isEncrypted: false, isReadOnly: false, isImmutable: false, isHidden: false, formula: '', unit: '' }
    newFieldOptionsList.value = []
  }
  showFieldModal.value = true
}

const saveNode = async () => {
  if (!selectedNode.value || !selectedNode.value.isDomain) return
  try {
    const url = isEditMode.value ? \`http://localhost:8080/api/nodes/\${selectedNode.value.id}\` : \`http://localhost:8080/api/domains/\${selectedNode.value.id}/nodes\`
    await $fetch(url, {
      method: isEditMode.value ? 'PUT' : 'POST',
      headers: getAuthHeaders(),
      body: {
        name: newNode.value.name,
        order: newNode.value.order,
        parentId: isEditMode.value ? undefined : (selectedNode.value.isDomain ? null : selectedNode.value.id)
      }
    })
    showNodeModal.value = false
    await loadTree()
  } catch (e) {
    alert('Error saving node')
  }
}

const saveField = async () => {`;

lines.splice(1010, 6, ...patchCode.split('\n'));
fs.writeFileSync('C:/dev/ai/frontend/pages/schema.vue', lines.join('\n'));
