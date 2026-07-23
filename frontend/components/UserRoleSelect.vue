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

const { fetchRoles, formatRoleText } = useRoles()

const defaultSystemRoles = ['ADMIN', 'ORG_ADMIN', 'DATA_STEWARD', 'DOMAIN_EDITOR', 'DQ_MANAGER', 'VIEWER', 'USER']
const dbRoles = ref([])

const loadRolesFromDb = async () => {
  await fetchRoles(props.orgId || undefined)
}

onMounted(() => {
  loadRolesFromDb()
})

watch(() => props.orgId, () => {
  loadRolesFromDb()
})

const allRoleCodes = computed(() => {
  const list = [...defaultSystemRoles]
  if (props.includeRolePrefix) {
    list.push('ROLE_ADMIN', 'ROLE_USER')
  }
  return Array.from(new Set(list))
})

const formattedOptions = computed(() => {
  return allRoleCodes.value.map(code => ({
    value: code,
    text: formatRoleText(code)
  }))
})

const onUpdate = (val) => {
  emit('update:modelValue', val)
  emit('change', val)
}
</script>
