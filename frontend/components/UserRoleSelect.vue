<template>
  <div class="user-role-select-wrapper" style="display: flex; flex-direction: column; gap: 0.4rem; width: 100%;">
    <va-select
      :model-value="modelValue"
      :options="formattedOptions"
      :multiple="multiple"
      :label="label"
      :placeholder="placeholder"
      :clearable="clearable"
      :disabled="disabled"
      :size="size"
      value-by="value"
      text-by="text"
      @update:modelValue="onUpdate"
      class="slim-role-select"
    />

    <!-- Selected Roles Chip List -->
    <div v-if="multiple && selectedRoleList.length > 0" class="selected-role-chips" style="display: flex; flex-wrap: wrap; gap: 0.35rem; margin-top: 0.25rem;">
      <va-chip
        v-for="roleCode in selectedRoleList"
        :key="roleCode"
        size="small"
        :color="getRoleColor(roleCode)"
        closeable
        @remove="removeRole(roleCode)"
        style="font-weight: 600; font-size: 0.78rem; padding: 2px 8px;"
      >
        {{ formatRoleText(roleCode) }}
      </va-chip>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRoles } from '~/composables/useRoles'

const props = defineProps({
  modelValue: {
    type: [String, Array],
    default: null
  },
  multiple: {
    type: Boolean,
    default: false
  },
  orgId: {
    type: String,
    default: null
  },
  label: {
    type: String,
    default: ''
  },
  placeholder: {
    type: String,
    default: ''
  },
  clearable: {
    type: Boolean,
    default: true
  },
  disabled: {
    type: Boolean,
    default: false
  },
  size: {
    type: String,
    default: 'medium'
  },
  includeRolePrefix: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue', 'change'])

const { fetchRolesForOrg, formatRoleText, getUserOrgId } = useRoles()

const localRoleList = ref([])

const loadRoles = async () => {
  const targetOrgId = props.orgId || getUserOrgId()
  localRoleList.value = await fetchRolesForOrg(targetOrgId)
}

onMounted(() => {
  loadRoles()
})

watch(() => props.orgId, async (newOrgId) => {
  localRoleList.value = await fetchRolesForOrg(newOrgId || getUserOrgId(), true)
})

const formattedOptions = computed(() => {
  if (!localRoleList.value || localRoleList.value.length === 0) return []

  const result = []
  const seenCodes = new Set()
  const seenTexts = new Set()

  localRoleList.value.forEach(role => {
    if (!role || !role.name) return
    const code = role.name
    const normCode = code.startsWith('ROLE_') ? code.replace('ROLE_', '') : code
    const textStr = formatRoleText(code)

    if (!seenCodes.has(code) && !seenCodes.has(normCode) && !seenTexts.has(textStr)) {
      seenCodes.add(code)
      seenCodes.add(normCode)
      seenTexts.add(textStr)
      result.push({
        value: code,
        text: textStr
      })
    }
  })

  return result
})

const selectedRoleList = computed(() => {
  if (!props.modelValue) return []
  if (Array.isArray(props.modelValue)) {
    return props.modelValue.filter(Boolean)
  }
  if (typeof props.modelValue === 'string') {
    return props.modelValue.split(',').map(r => r.trim()).filter(Boolean)
  }
  return []
})

const removeRole = (roleCodeToRemove) => {
  const normTarget = roleCodeToRemove.replace('ROLE_', '')
  const currentList = Array.isArray(props.modelValue)
    ? [...props.modelValue]
    : (typeof props.modelValue === 'string' ? props.modelValue.split(',').map(r => r.trim()) : [])

  const newList = currentList.filter(r => r !== roleCodeToRemove && r.replace('ROLE_', '') !== normTarget)

  const finalVal = Array.isArray(props.modelValue) ? newList : newList.join(', ')
  emit('update:modelValue', finalVal)
  emit('change', finalVal)
}

const getRoleColor = (code) => {
  const norm = code ? code.replace('ROLE_', '') : ''
  switch (norm) {
    case 'ADMIN': return 'danger'
    case 'ORG_ADMIN': return 'warning'
    case 'DATA_STEWARD': return 'primary'
    case 'DOMAIN_EDITOR': return 'info'
    case 'DQ_MANAGER': return 'success'
    case 'VIEWER': return 'secondary'
    default: return 'primary'
  }
}

const onUpdate = (val) => {
  emit('update:modelValue', val)
  emit('change', val)
}
</script>

<style scoped>
:deep(.slim-role-select .va-input-wrapper__field) {
  max-height: 42px !important;
  overflow: hidden !important;
}
</style>
