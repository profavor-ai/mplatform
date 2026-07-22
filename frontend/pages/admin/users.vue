<template>
  <div style="display: flex; flex-direction: column; padding-bottom: 2rem;">
    <h1 style="font-size: 2rem; font-weight: bold; margin-bottom: 1.5rem; color: var(--va-text-primary);">
      {{ $t('user_management') }}
    </h1>

    <div style="display: flex; gap: 1.5rem; align-items: flex-start; flex-wrap: wrap;">
      <!-- User List -->
      <va-card style="flex: 1; min-width: 320px; display: flex; flex-direction: column;">
        <va-card-title style="display: flex; justify-content: space-between; align-items: center;">
          <span>{{ $t('user_management') }}</span>
          <va-input v-model="searchQuery" :placeholder="$t('search') || 'Search'" @keyup.enter="fetchUsers" clearable @clear="fetchUsers" style="max-width: 150px;" />
        </va-card-title>
        <va-card-content style="flex: 1; display: flex; flex-direction: column;">
          <va-list style="flex: 1;">
            <va-list-item
              v-for="user in users"
              :key="user.id"
              @click="selectUser(user)"
              style="cursor: pointer; padding: 0.75rem 0.5rem; border-radius: 6px; margin-bottom: 0.25rem;"
              :style="{
                backgroundColor: selectedUser?.id === user.id ? 'var(--va-background-element)' : 'transparent',
                border: selectedUser?.id === user.id ? '1px solid var(--va-primary)' : '1px solid transparent'
              }"
            >
              <va-list-item-section avatar>
                <va-icon name="account_circle" size="large" />
              </va-list-item-section>
              <va-list-item-section>
                <va-list-item-title style="font-weight: bold; font-size: 1rem;">{{ user.username }}</va-list-item-title>
                <div style="display: flex; gap: 0.35rem; align-items: center; margin-top: 0.2rem; flex-wrap: wrap;">
                  <va-badge :text="user.role || 'USER'" color="primary" size="small" />
                  <va-badge :text="getOrgName(user.organizationId)" color="info" outline size="small" />
                </div>
              </va-list-item-section>
            </va-list-item>
          </va-list>
          <div style="margin-top: 1rem; display: flex; justify-content: center;" v-if="totalPages > 1">
            <va-pagination v-model="currentPage" :pages="totalPages" @update:modelValue="fetchUsers" :visible-pages="5" size="small" />
          </div>
        </va-card-content>
      </va-card>

      <!-- Permissions & Org Assignment -->
      <va-card v-if="selectedUser" style="flex: 2; min-width: 420px;">
        <va-card-title style="display: flex; justify-content: space-between; align-items: center;">
          <span>{{ $t('permissions') }}: {{ selectedUser.username }}</span>
          <va-badge :text="selectedUser.username" color="success" />
        </va-card-title>
        <va-card-content>
          
          <!-- Organization & Role Setting -->
          <div style="background: var(--va-background-secondary); border: 1px solid var(--va-background-border); border-radius: 8px; padding: 1.25rem; margin-bottom: 1.5rem;">
            <h3 style="font-weight: 700; margin-bottom: 1rem; color: var(--va-text-primary); font-size: 1.05rem; display: flex; align-items: center; gap: 0.5rem;">
              <va-icon name="apartment" color="primary" />
              {{ $t('belongs_to_org') }} & {{ $t('user_role') }}
            </h3>

            <div style="display: flex; flex-direction: column; gap: 1rem;">
              <va-select
                v-model="selectedUserOrgId"
                :options="organizations"
                value-by="id"
                text-by="displayName"
                :label="$t('belongs_to_org')"
                outline
              />
              <va-select
                v-model="selectedUserRole"
                :options="['ORG_ADMIN', 'DATA_STEWARD', 'DOMAIN_EDITOR', 'DQ_MANAGER', 'VIEWER', 'ADMIN', 'USER']"
                :label="$t('user_role')"
                outline
              />
              <div style="display: flex; justify-content: flex-end;">
                <va-button color="primary" icon="save" @click="updateUserTenantInfo">
                  {{ $t('save_changes') }}
                </va-button>
              </div>
            </div>
          </div>

          <va-divider style="margin: 1.5rem 0;" />

          <!-- Domain Permissions -->
          <div style="margin-bottom: 1rem;">
            <h3 style="font-weight: bold; margin-bottom: 0.5rem; color: var(--va-text-primary);">{{ $t('granted_domains') }}</h3>
            <div v-if="userPermissions.length === 0" style="color: var(--va-text-secondary); font-size: 0.85rem;">{{ $t('no_specific_domain_permissions') }}</div>
            <div v-else style="display: flex; gap: 0.5rem; flex-wrap: wrap;">
              <va-chip v-for="perm in userPermissions" :key="perm.id" color="success" style="margin-bottom: 0.5rem;">
                {{ getDomainName(perm.domain.name) }}
                <va-icon name="close" size="small" style="margin-left: 0.5rem; cursor: pointer;" @click="revokePermission(perm.domain.id)" />
              </va-chip>
            </div>
          </div>
          
          <va-divider style="margin: 1.5rem 0;" />
          
          <div>
            <h3 style="font-weight: bold; margin-bottom: 0.5rem; color: var(--va-text-primary);">{{ $t('grant_new_permission') }}</h3>
            <div style="display: flex; gap: 0.5rem;">
              <va-select v-model="selectedDomainToGrant" :options="availableDomains" value-by="id" text-by="label" :placeholder="$t('select_a_domain')" style="flex: 1;" />
              <va-button @click="grantPermission" :disabled="!selectedDomainToGrant">{{ $t('grant') }}</va-button>
            </div>
          </div>
        </va-card-content>
      </va-card>
    </div>

    <!-- Requests -->
    <va-card style="margin-top: 1.5rem;">
      <va-card-title>{{ $t('pending_domain_access_requests') }}</va-card-title>
      <va-card-content>
        <div v-if="pendingRequests.length === 0" style="color: var(--va-text-secondary); font-size: 0.85rem;">{{ $t('no_pending_requests') }}</div>
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
                  <div style="font-size: 0.8rem; color: var(--va-text-secondary); margin-top: 0.2rem;">
                    {{ group.formattedDate }}
                  </div>
                </div>
              </div>

              <div style="flex: 1; margin-bottom: 1.25rem;">
                <div style="font-size: 0.85rem; font-weight: 600; color: var(--va-text-secondary); margin-bottom: 0.5rem;">
                  {{ $t('requested_domains') }} ({{ group.domains.length }})
                </div>
                <div style="display: flex; flex-wrap: wrap; gap: 0.4rem; max-height: 100px; overflow-y: auto; padding: 0.2rem;">
                  <va-chip v-for="(dom, idx) in group.domains" :key="idx" size="small" color="primary" outline>
                    {{ dom }}
                  </va-chip>
                </div>
              </div>

              <div style="display: flex; gap: 0.75rem; justify-content: flex-end; border-top: 1px solid var(--va-background-element); padding-top: 1rem;">
                <va-button color="danger" preset="secondary" size="small" @click="handleBatchReject(group.reqIds)">
                  {{ $t('reject') }}
                </va-button>
                <va-button color="success" size="small" @click="handleBatchApprove(group.reqIds)">
                  {{ $t('approve') }}
                </va-button>
              </div>
            </va-card-content>
          </va-card>
        </div>
      </va-card-content>
    </va-card>

    <!-- System Notification Modal -->
    <va-modal
      v-model="showErrorAlertModal"
      :title="errorAlertTitle || $t('system_notification')"
      hide-default-actions
      size="small"
      :prevent-click-outside="true"
      :no-outside-dismiss="true"
    >
      <div style="padding: 1.25rem 0; text-align: center;">
        <div
          v-if="errorAlertType === 'success'"
          style="width: 60px; height: 60px; border-radius: 50%; background: rgba(30, 203, 114, 0.12); color: #15803d; display: flex; align-items: center; justify-content: center; margin: 0 auto 1.25rem auto;"
        >
          <va-icon name="check_circle" size="2.5rem" color="success" />
        </div>
        <div
          v-else-if="errorAlertType === 'warning'"
          style="width: 60px; height: 60px; border-radius: 50%; background: rgba(232, 139, 36, 0.12); color: #c2410c; display: flex; align-items: center; justify-content: center; margin: 0 auto 1.25rem auto;"
        >
          <va-icon name="warning" size="2.5rem" color="warning" />
        </div>
        <div
          v-else
          style="width: 60px; height: 60px; border-radius: 50%; background: rgba(229, 57, 53, 0.12); color: #b91c1c; display: flex; align-items: center; justify-content: center; margin: 0 auto 1.25rem auto;"
        >
          <va-icon name="error" size="2.5rem" color="danger" />
        </div>

        <h3
          style="margin: 0 0 0.75rem 0; font-weight: 700; font-size: 1.25rem;"
          :style="{
            color: errorAlertType === 'success' ? '#15803d' : (errorAlertType === 'warning' ? '#c2410c' : '#b91c1c')
          }"
        >
          {{ errorAlertHeader || $t('system_notification') }}
        </h3>

        <div style="background: var(--va-background-secondary); border: 1px solid var(--va-background-border); border-radius: 8px; padding: 1rem 1.25rem; text-align: left; font-size: 0.92rem; color: var(--va-text-primary); max-height: 200px; overflow-y: auto; margin-bottom: 1.5rem; word-break: break-word; white-space: pre-wrap;">
          {{ errorAlertMessage }}
        </div>

        <div style="display: flex; justify-content: center;">
          <va-button
            :color="errorAlertType === 'success' ? 'success' : (errorAlertType === 'warning' ? 'warning' : 'primary')"
            preset="solid"
            style="min-width: 120px;"
            @click="showErrorAlertModal = false"
          >
            {{ $t('close') || '확인' }}
          </va-button>
        </div>
      </div>
    </va-modal>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useCookie } from '#app'

const { t, locale } = useI18n()

const getDomainName = (nameObj) => {
  if (!nameObj) return 'Unknown'
  const lang = (locale && locale.value) ? locale.value : 'ko'
  if (typeof nameObj === 'string') {
    try {
      const parsed = JSON.parse(nameObj)
      return parsed[lang] || parsed.ko || parsed.en || 'Unknown'
    } catch {
      return nameObj
    }
  }
  return nameObj[lang] || nameObj.ko || nameObj.en || 'Unknown'
}

const token = useCookie('auth_token')
const showErrorAlertModal = ref(false)
const errorAlertTitle = ref('')
const errorAlertHeader = ref('')
const errorAlertMessage = ref('')
const errorAlertType = ref('success')

const showCustomAlert = (msg, header = '', title = '', type = 'success') => {
  errorAlertMessage.value = msg
  errorAlertHeader.value = header
  errorAlertTitle.value = title
  errorAlertType.value = type
  showErrorAlertModal.value = true
}
const users = ref([])
const currentPage = ref(1)
const totalPages = ref(1)
const searchQuery = ref('')
const selectedUser = ref(null)
const selectedUserRole = ref('USER')
const selectedUserOrgId = ref(null)
const organizations = ref([])

const userPermissions = ref([])
const allDomains = ref([])
const selectedDomainToGrant = ref(null)
const pendingRequests = ref([])

const parseDate = (dateString) => {
  if (!dateString) return null
  let str = String(dateString).trim()
  if (str.includes(' ') && !str.includes('T')) {
    str = str.replace(' ', 'T')
  }
  if (!str.endsWith('Z') && !str.includes('+') && !str.includes('-')) {
    const tz = useCookie('timezone', { default: () => 'Asia/Seoul' }).value
    let serverOffset = '+09:00'
    if (tz === 'UTC' || tz === 'GMT') serverOffset = 'Z'
    else if (tz === 'America/New_York') serverOffset = '-05:00'
    else if (tz === 'Europe/London') serverOffset = '+00:00'
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

const fetchOrganizations = async () => {
  try {
    const res = await $fetch('/api/organizations', {
      headers: { Authorization: `Bearer ${token.value}` }
    })
    organizations.value = res || []
  } catch (e) {
    console.error('Failed to fetch orgs:', e)
  }
}

const getOrgName = (orgId) => {
  if (!orgId) return 'Default Org'
  const found = organizations.value.find(o => o.id === orgId)
  return found ? (found.displayName || found.name) : 'Default Org'
}

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
    users.value = res.content || []
    totalPages.value = res.totalPages || 1
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
  selectedUserRole.value = user.role || 'USER'
  selectedUserOrgId.value = user.organizationId || (organizations.value[0]?.id || null)
  await loadUserPermissions(user.id)
}

const updateUserTenantInfo = async () => {
  if (!selectedUser.value) return
  try {
    await $fetch(`/api/permissions/users/${selectedUser.value.id}/tenant-info`, {
      method: 'PUT',
      headers: { Authorization: `Bearer ${token.value}` },
      body: {
        role: selectedUserRole.value,
        organizationId: selectedUserOrgId.value
      }
    })
    selectedUser.value.role = selectedUserRole.value
    selectedUser.value.organizationId = selectedUserOrgId.value
    await fetchUsers()
    showCustomAlert(
      t('user_info_updated_msg') || 'User organization and role updated successfully.',
      t('update_success') || 'Update Success',
      t('notification') || 'Notification',
      'success'
    )
  } catch (e) {
    showCustomAlert(
      e.message || String(e),
      t('update_failed') || 'Update Failed',
      t('error') || 'Error',
      'error'
    )
  }
}

const loadUserPermissions = async (userId) => {
  userPermissions.value = await $fetch(`/api/permissions/users/${userId}/domains`, {
    headers: { Authorization: `Bearer ${token.value}` }
  })
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

const handleBatchApprove = async (reqIds) => {
  try {
    await Promise.all(reqIds.map(id => 
      $fetch(`/api/permissions/requests/${id}/approve`, {
        method: 'POST',
        headers: { Authorization: `Bearer ${token.value}` }
      })
    ))
    await fetchPendingRequests()
    if (selectedUser.value) {
      await loadUserPermissions(selectedUser.value.id)
    }
  } catch (e) {
    console.error(e)
  }
}

const handleBatchReject = async (reqIds) => {
  try {
    await Promise.all(reqIds.map(id => 
      $fetch(`/api/permissions/requests/${id}/reject`, {
        method: 'POST',
        headers: { Authorization: `Bearer ${token.value}` },
      })
    ))
    await fetchPendingRequests()
  } catch (e) {
    console.error(e)
  }
}

onMounted(() => {
  fetchOrganizations()
  fetchUsers()
  fetchDomains()
  fetchPendingRequests()
})
</script>
