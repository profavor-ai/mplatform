<template>
  <span class="role-badge-wrapper" style="display: inline-flex; gap: 0.35rem; flex-wrap: wrap;">
    <va-badge
      v-for="role in roleListInput"
      :key="role"
      :color="getRoleColor(role)"
      class="mr-1 mb-1"
      style="padding: 4px 8px; font-weight: 600;"
    >
      {{ formatRoleText(role) }}
    </va-badge>
  </span>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRoles } from '~/composables/useRoles'

const props = defineProps({
  value: {
    type: [String, Array],
    default: () => []
  },
  orgId: {
    type: String,
    default: null
  }
})

const { fetchRoles, formatRoleText } = useRoles()

onMounted(() => {
  fetchRoles(props.orgId || undefined)
})

const roleListInput = computed(() => {
  if (!props.value) return []
  if (Array.isArray(props.value)) {
    return props.value.filter(Boolean)
  }
  if (typeof props.value === 'string') {
    return props.value.split(',').map(r => r.trim()).filter(Boolean)
  }
  return []
})

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
</script>
