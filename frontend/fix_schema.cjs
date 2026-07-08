const fs = require('fs');
let c = fs.readFileSync('C:/dev/ai/frontend/pages/schema.vue', 'utf8');

const regex = /      }\n      body: {\n        name: newNode\.value\.name,\n        order: newNode\.value\.order,\n        parentId: isEditMode\.value \? undefined : \(selectedNode\.value\.isDomain \? null : selectedNode\.value\.id\)\n  }\n\n  const duplicate = fields\.value\.find\(f => f\.key === newField\.value\.key && \(\!isEditMode\.value \|\| f\.id !== editingId\.value\)\)/;

const replacement = `      }
    }
  } catch (e) {
    console.error('Failed to load sectors/groups/workflows', e)
  }

  try {
    let data = []
    if (!node.isDomain) {
      data = await $fetch(\`/api/nodes/\${node.id}/fields/effective\`, {
        headers: { Authorization: \`Bearer \${token.value}\` }
      })
    } else {
      data = await $fetch(\`/api/domains/\${node.id}/fields\`, {
        headers: { Authorization: \`Bearer \${token.value}\` }
      })
    }
    fields.value = data
    setTimeout(() => {
      if (gridApi.value) {
        gridApi.value.sizeColumnsToFit()
      }
    }, 100)
  } catch (e) {
    console.error(e.message || e)
  }
}

const handleNodeEdit = (node) => {
  selectedNode.value = node
  if (node.isDomain) {
    openDomainModal(true)
  } else {
    openNodeModal(true)
  }
}

const openDomainModal = async (edit) => {
  isEditMode.value = edit
  mappingError.value = { id: false, name: false }
  if (edit && selectedNode.value?.isDomain) {
    editingId.value = selectedNode.value.id
    try {
      const domains = await $fetch('/api/domains', {
        headers: { Authorization: \`Bearer \${token.value}\` }
      })
      const d = domains.find(x => x.id === editingId.value)
      if (d) {
        newDomain.value = {
          name: { ko: d.name?.ko || '', en: d.name?.en || '' },
          description: { ko: d.description?.ko || '', en: d.description?.en || '' },
          identifierFieldId: d.identifierFieldId || null,
          displayNameFieldId: d.displayNameFieldId || null,
          descriptionFieldId: d.descriptionFieldId || null
        }
      }
      const fieldsData = await $fetch(\`/api/domains/\${editingId.value}/fields\`, {
        headers: { Authorization: \`Bearer \${token.value}\` }
      })
      domainFieldOptions.value = fieldsData.map(f => ({
        value: f.id,
        text: \`\${f.name.ko || f.name.en} (\${f.key})\`
      }))
    } catch(e){}
  } else {
    editingId.value = null
    newDomain.value = { name: { ko: '', en: '' }, description: { ko: '', en: '' }, identifierFieldId: null, displayNameFieldId: null, descriptionFieldId: null }
    domainFieldOptions.value = []
  }
  showDomainModal.value = true
}

const openNodeModal = (edit) => {
  isEditMode.value = edit
  if (edit && selectedNode.value && !selectedNode.value.isDomain) {
    editingId.value = selectedNode.value.id
    const originalNameMap = selectedNode.value.originalNameMap || { ko: '', en: '' }
    newNode.value = {
      name: { ko: originalNameMap.ko || '', en: originalNameMap.en || '' },
      order: selectedNode.value.order || 1
    }
  } else {
    editingId.value = null
    newNode.value = { name: { ko: '', en: '' }, order: 1 }
  }
  showNodeModal.value = true
}

const openFieldModal = (edit, rowData) => {
  isEditMode.value = edit
  if (edit && rowData) {
    editingId.value = rowData.id
    newField.value = {
      name: { ko: rowData.name?.ko || '', en: rowData.name?.en || '' },
      fieldGroupId: rowData.fieldGroup?.id || null,
      key: rowData.key,
      type: rowData.type,
      options: rowData.options || '',
      targetDomainId: null,
      required: rowData.required,
      isMultiValue: rowData.isMultiValue,
      isSearchable: rowData.isSearchable,
      isEncrypted: rowData.isEncrypted,
      isReadOnly: rowData.isReadOnly,
      isImmutable: rowData.isImmutable,
      isHidden: rowData.isHidden,
      unit: rowData.unit || '',
      order: rowData.order || 0
    }
    
    if (rowData.type === 'DOMAIN_REFERENCE') {
      try {
        const parsedOpts = JSON.parse(rowData.options || '{}')
        newField.value.targetDomainId = parsedOpts.targetDomainId || null
      } catch(e) {}
    } else if (rowData.type === 'CALCULATED') {
      try {
        const parsedOpts = JSON.parse(rowData.options || '{}')
        newField.value.formula = parsedOpts.formula || ''
      } catch(e) {}
    } else {
      try {
        const parsedOpts = JSON.parse(rowData.options || '[]')
        if (Array.isArray(parsedOpts)) {
          newFieldOptionsList.value = parsedOpts.map(opt => {
            if (typeof opt === 'string') return { key: opt, label: { ko: opt, en: opt }, order: 0 }
            return { 
              key: opt.key || '', 
              label: { ko: opt.label?.ko || opt.label?.en || opt.key, en: opt.label?.en || opt.label?.ko || opt.key },
              order: opt.order || 0
            }
          })
        }
      } catch (e) {
        newFieldOptionsList.value = [{ key: '', label: { ko: '', en: '' }, order: 0 }]
      }
    }
  } else {
    editingId.value = null
    newField.value = { 
      name: { ko: '', en: '' }, 
      fieldGroupId: null,
      key: '', 
      type: 'TEXT', 
      options: '', 
      targetDomainId: null,
      required: false, 
      isMultiValue: false, 
      isSearchable: true, 
      isEncrypted: false,
      isReadOnly: false,
      isImmutable: false,
      isHidden: false,
      unit: '',
      order: fields.value.length + 1
    }
    newFieldOptionsList.value = [{ key: '', label: { ko: '', en: '' }, order: 0 }]
  }
  showFieldModal.value = true
}

const saveDomain = async () => {
  if (isEditMode.value) {
    mappingError.value.id = !newDomain.value.identifierFieldId;
    mappingError.value.name = !newDomain.value.displayNameFieldId;
    if (mappingError.value.id || mappingError.value.name) {
      return;
    }
  }

  try {
    const url = isEditMode.value ? \`/api/domains/\${editingId.value}\` : '/api/domains'
    const method = isEditMode.value ? 'PUT' : 'POST'
    await $fetch(url, {
      method,
      headers: { Authorization: \`Bearer \${token.value}\` },
      body: newDomain.value
    })
    newDomain.value = { name: { ko: '', en: '' }, description: { ko: '', en: '' }, identifierFieldId: null, displayNameFieldId: null, descriptionFieldId: null }
    showDomainModal.value = false
    await loadTree()
  } catch (error) {
    alert('Error saving domain: ' + (error.data?.message || error.message || 'Unknown error'))
  }
}

const saveNode = async () => {
  if (!selectedNode.value) return
  try {
    const url = isEditMode.value 
      ? \`/api/domains/\${selectedNode.value.domainId}/nodes/\${editingId.value}\`
      : \`/api/domains/\${selectedNode.value.domainId}/nodes\`
    const method = isEditMode.value ? 'PUT' : 'POST'
    await $fetch(url, {
      method,
      headers: { Authorization: \`Bearer \${token.value}\` },
      body: {
        name: newNode.value.name,
        order: newNode.value.order,
        parentId: isEditMode.value ? undefined : (selectedNode.value.isDomain ? null : selectedNode.value.id)
      }
    })
    newNode.value = { name: { ko: '', en: '' }, order: 1 }
    showNodeModal.value = false
    await loadTree()
  } catch (error) {
    alert('Error saving node')
  }
}

const saveField = async () => {
  if (!selectedNode.value) return
  
  if (!newField.value.key || String(newField.value.key).trim() === '') {
    alert(currentLocale.value === 'ko' ? 'Field Key를 입력해주세요.' : 'Please enter a Field Key.')
    return
  }

  const duplicate = fields.value.find(f => f.key === newField.value.key && (!isEditMode.value || f.id !== editingId.value))`;

c = c.replace(regex, replacement);
fs.writeFileSync('C:/dev/ai/frontend/pages/schema.vue', c);
