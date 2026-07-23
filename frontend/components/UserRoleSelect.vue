<template>
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
  />
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

  localRoleList.value.forEach(role => {
    if (!role || !role.name) return
    const code = role.name
    if (!seenCodes.has(code)) {
      seenCodes.add(code)
      result.push({
        value: code,
        text: formatRoleText(code)
      })
    }

    if (props.includeRolePrefix && !code.startsWith('ROLE_')) {
      const prefixedCode = `ROLE_${code}`
      if (!seenCodes.has(prefixedCode)) {
        seenCodes.add(prefixedCode)
        result.push({
          value: prefixedCode,
          text: formatRoleText(code)
        })
      }
    }
  })

  return result
})

const onUpdate = (val) => {
  emit('update:modelValue', val)
  emit('change', val)
}
</script>
