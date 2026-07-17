<template>
  <div style="display: flex; flex-direction: column; padding-bottom: 2rem;">
    <h1 style="font-size: 2rem; font-weight: bold; margin-bottom: 1.5rem; color: var(--va-text-primary);">{{ t('user_management') }}</h1>

    <div style="display: flex; gap: 1.5rem; align-items: flex-start; flex-wrap: wrap;">
      <!-- User List -->
      <va-card style="flex: 1; min-width: 300px; display: flex; flex-direction: column;">
        <va-card-title style="display: flex; justify-content: space-between; align-items: center;">
          <span>{{ t('user_management') }}</span>
          <va-input v-model="searchQuery" :placeholder="t('search') || 'Search'" @keyup.enter="fetchUsers" clearable @clear="fetchUsers" style="max-width: 150px;" />
        </va-card-title>
        <va-card-content style="flex: 1; display: flex; flex-direction: column;">
          <va-list style="flex: 1;">
            <va-list-item v-for="user in users" :key="user.id" @click="selectUser(user)" style="cursor: pointer; padding: 0.5rem; border-radius: 4px;" :style="selectedUser?.id === user.id ? 'background-color: var(--va-background-element);' : ''">
              <va-list-item-section avatar>
                <va-icon name="account_circle" />
              </va-list-item-section>
              <va-list-item-section>
                <va-list-item-title style="font-weight: bold;">{{ user.username }}</va-list-item-title>
                <va-list-item-subtitle style="font-size: 0.85rem; color: var(--va-text-secondary);">{{ user.role }}</va-list-item-subtitle>
              </va-list-item-section>
            </va-list-item>
          </va-list>
          <div style="margin-top: 1rem; display: flex; justify-content: center;" v-if="totalPages > 1">
            <va-pagination v-model="currentPage" :pages="totalPages" @update:modelValue="fetchUsers" :visible-pages="5" size="small" />
          </div>
        </va-card-content>
      </va-card>

      <!-- Permissions -->
      <va-card v-if="selectedUser" style="flex: 2; min-width: 400px;">
        <va-card-title>{{ t('permissions') }}: {{ selectedUser.username }}</va-card-title>
        <va-card-content>
          <div style="margin-bottom: 1rem;">
            <h3 style="font-weight: bold; margin-bottom: 0.5rem; color: var(--va-text-primary);">{{ t('user_role') }}</h3>
            <div style="display: flex; gap: 0.5rem;">
              <va-select v-model="selectedUserRole" :options="['USER', 'MANAGER', 'ADMIN']" style="flex: 1;" />
              <va-button @click="updateRole" :disabled="selectedUserRole === selectedUser.role">{{ t('update_role') }}</va-button>
            </div>
          </div>
          <va-divider style="margin: 1.5rem 0;" />

          <div style="margin-bottom: 1rem;">
            <h3 style="font-weight: bold; margin-bottom: 0.5rem; color: var(--va-text-primary);">{{ t('granted_domains') }}</h3>
            <div v-if="userPermissions.length === 0" style="color: var(--va-text-secondary); font-size: 0.85rem;">{{ t('no_specific_domain_permissions') }}</div>
            <div v-else style="display: flex; gap: 0.5rem; flex-wrap: wrap;">
              <va-chip v-for="perm in userPermissions" :key="perm.id" color="success" style="margin-bottom: 0.5rem;">
                {{ getDomainName(perm.domain.name) }}
                <va-icon name="close" size="small" style="margin-left: 0.5rem; cursor: pointer;" @click="revokePermission(perm.domain.id)" />
              </va-chip>
            </div>
          </div>
          
          <va-divider style="margin: 1.5rem 0;" />
          
          <div>
            <h3 style="font-weight: bold; margin-bottom: 0.5rem; color: var(--va-text-primary);">{{ t('grant_new_permission') }}</h3>
            <div style="display: flex; gap: 0.5rem;">
              <va-select v-model="selectedDomainToGrant" :options="availableDomains" value-by="id" text-by="label" :placeholder="t('select_a_domain')" style="flex: 1;" />
              <va-button @click="grantPermission" :disabled="!selectedDomainToGrant">{{ t('grant') }}</va-button>
            </div>
          </div>
        </va-card-content>
      </va-card>
    </div>

    <!-- Requests -->
    <va-card style="margin-top: 1.5rem;">
      <va-card-title>{{ t('pending_domain_access_requests') }}</va-card-title>
      <va-card-content>
        <div v-if="pendingRequests.length === 0" style="color: var(--va-text-secondary); font-size: 0.85rem;">{{ t('no_pending_requests') }}</div>
        <div v-else style="display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 1.5rem;">
          <va-card v-for="group in groupedPendingRequests" :key="group.userId" outlined style="transition: transform 0.2s, box-shadow 0.2s; cursor: default; background-color: var(--va-background-primary);" class="hoverable-card">
            <va-card-content style="display: flex; flex-direction: column; height: 100%; padding: 1.5rem;">
              <div style="display: flex; align-items: center; gap: 1rem; margin-bottom: 1.25rem;">
                <va-avatar size="large" color="primary" style="font-size: 1.25rem; font-weight: bold; width: 48px; height: 48px;">
                  {{ group.username.charAt(0).toUpperCase() }}
                </va-avatar>
                <div style="flex: 1; overflow: hidden;">
                  <div style="font-weight: 700; font-size: 1.15rem; color: var(--va-text-primary); white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">
                    {{ group.username }}
                  </div>
                  <div style="font-size: 0.8rem; color: var(--va-text-secondary); display: flex; align-items: center; gap: 0.25rem; margin-top: 0.1rem;">
                    <va-icon name="schedule" size="14px" />
                    <span>{{ group.formattedDate }}</span>
                  </div>
                </div>
              </div>
              
              <div style="margin-bottom: 1.5rem; flex-grow: 1;">
                <div style="font-size: 0.85rem; color: var(--va-text-secondary); margin-bottom: 0.5rem; font-weight: 600;">
                  {{ t('requested_access_to') }}
                </div>
                <div style="display: flex; flex-wrap: wrap; gap: 0.5rem;">
                  <va-chip v-for="domain in group.domains" :key="domain" color="primary" outline size="small" style="font-weight: 600; background-color: rgba(var(--va-primary-rgb), 0.05);">
                    {{ domain }}
                  </va-chip>
                </div>
              </div>
              
              <div style="display: flex; gap: 0.75rem;">
                <va-button style="flex: 1; font-weight: 600;" color="success" @click="approveRequestGroup(group.reqIds)">
                  {{ t('approve') }}
                </va-button>
                <va-button style="flex: 1; font-weight: 600;" color="danger" outline @click="rejectRequestGroup(group.reqIds)">
                  {{ t('reject') }}
                </va-button>
              </div>
            </va-card-content>
          </va-card>
        </div>
      </va-card-content>
    </va-card>

  </div>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import { ref, onMounted, computed } from 'vue'
import { useCookie } from '#app'

const { t } = useI18n()
const token = useCookie('auth_token')
const currentLocale = useCookie('locale', { default: () => 'ko' })

const users = ref([])
const allDomains = ref([])
const selectedUser = ref(null)
const userPermissions = ref([])
const pendingRequests = ref([])
const selectedDomainToGrant = ref(null)
const selectedUserRole = ref(null)

const searchQuery = ref('')
const currentPage = ref(1)
const totalPages = ref(1)

const parseDate = (dateString) => {
  if (!dateString) return null
  let str = String(dateString).trim()
  if (/^\d+$/.test(str)) {
    return new Date(parseInt(str, 10))
  }
  if (!str.endsWith('Z') && !str.includes('+') && !/[-+]\d{2}:\d{2}$/.test(str)) {
    if (str.includes(' ') && !str.includes('T')) {
      str = str.replace(' ', 'T')
    }
    const serverOffset = useCookie('server_offset', { default: () => '+09:00' }).value
    str += serverOffset
  }
  const d = new Date(str)
  return isNaN(d.getTime()) ? new Date(dateString) : d
}

const formatDate = (dateString) => {
  if (!dateString) return ''
  const date = parseDate(dateString)
  if (!date) return ''
  const tz = useCookie('timezone', { default: () => 'Asia/Seoul' }).value
  const formatted = date.toLocaleString(undefined, { timeZone: tz })
  return formatted.replace(/\s*(GMT|UTC|KST|PST|EST|CET)[-+0-9:]*/gi, '').trim()
}

const groupedPendingRequests = computed(() => {
  const groups = {}
  pendingRequests.value.forEach(req => {
    const key = req.user.id
    if (!groups[key]) {
      groups[key] = {
        userId: req.user.id,
        username: req.user.username,
        domains: [],
        reqIds: [],
        createdAt: req.createdAt
      }
    }
    groups[key].domains.push(getDomainName(req.domain.name))
    groups[key].reqIds.push(req.id)
    if (new Date(req.createdAt) < new Date(groups[key].createdAt)) {
      groups[key].createdAt = req.createdAt
    }
  })
  return Object.values(groups).map(g => ({
    ...g,
    formattedDate: formatDate(g.createdAt)
  }))
})

const fetchUsers = async () => {
  try {
    const res = await $fetch('/api/permissions/users', {
      headers: { Authorization: `Bearer ${token.value}` },
      query: {
        page: currentPage.value - 1,
        size: 10,
        search: searchQuery.value || ''
      }
    })
    users.value = res.content
    totalPages.value = res.totalPages
  } catch (e) {
    console.error(e)
  }
}

const fetchDomains = async () => {
  allDomains.value = await $fetch('/api/domains', {
    headers: { Authorization: `Bearer ${token.value}` }
  })
}

const fetchPendingRequests = async () => {
  pendingRequests.value = await $fetch('/api/permissions/requests/pending', {
    headers: { Authorization: `Bearer ${token.value}` }
  })
}

const selectUser = async (user) => {
  selectedUser.value = user
  selectedUserRole.value = user.role
  await loadUserPermissions(user.id)
}

const updateRole = async () => {
  if (!selectedUser.value || !selectedUserRole.value) return
  await $fetch(`/api/permissions/users/${selectedUser.value.id}/role`, {
    method: 'PUT',
    headers: { Authorization: `Bearer ${token.value}` },
    body: { role: selectedUserRole.value }
  })
  selectedUser.value.role = selectedUserRole.value
  await fetchUsers()
}

const loadUserPermissions = async (userId) => {
  userPermissions.value = await $fetch(`/api/permissions/users/${userId}/domains`, {
    headers: { Authorization: `Bearer ${token.value}` }
  })
}

const getDomainName = (nameObj) => {
  if (!nameObj) return 'Unknown'
  if (typeof nameObj === 'string') {
    try {
      const parsed = JSON.parse(nameObj)
      return parsed[currentLocale.value] || parsed.ko || parsed.en || 'Unknown'
    } catch {
      return nameObj
    }
  }
  return nameObj[currentLocale.value] || nameObj.ko || nameObj.en || 'Unknown'
}

const availableDomains = computed(() => {
  const grantedIds = userPermissions.value.map(p => p.domain.id)
  return allDomains.value
    .filter(d => !grantedIds.includes(d.id))
    .map(d => ({ id: d.id, label: getDomainName(d.name) }))
})

const grantPermission = async () => {
  if (!selectedUser.value || !selectedDomainToGrant.value) return
  await $fetch(`/api/permissions/users/${selectedUser.value.id}/domains/${selectedDomainToGrant.value}`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${token.value}` }
  })
  selectedDomainToGrant.value = null
  await loadUserPermissions(selectedUser.value.id)
}

const revokePermission = async (domainId) => {
  if (!selectedUser.value) return
  await $fetch(`/api/permissions/users/${selectedUser.value.id}/domains/${domainId}`, {
    method: 'DELETE',
    headers: { Authorization: `Bearer ${token.value}` }
  })
  await loadUserPermissions(selectedUser.value.id)
}

const approveRequest = async (reqId) => {
  await $fetch(`/api/permissions/requests/${reqId}/approve`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${token.value}` }
  })
  await fetchPendingRequests()
  if (selectedUser.value) {
    await loadUserPermissions(selectedUser.value.id)
  }
}

const rejectRequest = async (reqId) => {
  await $fetch(`/api/permissions/requests/${reqId}/reject`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${token.value}` }
  })
  await fetchPendingRequests()
}

const approveRequestGroup = async (reqIds) => {
  await Promise.all(reqIds.map(reqId => 
    $fetch(`/api/permissions/requests/${reqId}/approve`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${token.value}` }
    })
  ))
  await fetchPendingRequests()
  if (selectedUser.value) {
    await loadUserPermissions(selectedUser.value.id)
  }
}

const rejectRequestGroup = async (reqIds) => {
  await Promise.all(reqIds.map(reqId => 
    $fetch(`/api/permissions/requests/${reqId}/reject`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${token.value}` }
    })
  ))
  await fetchPendingRequests()
}

onMounted(() => {
  fetchUsers()
  fetchDomains()
  fetchPendingRequests()
})
</script>

<style scoped>
.bg-primary-light {
  background-color: var(--va-background-element);
}

.hoverable-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08) !important;
}

</style>
