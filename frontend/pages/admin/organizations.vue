<template>
  <div style="display: flex; flex-direction: column; padding-bottom: 2rem; width: 100%;">
    <!-- Header -->
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.5rem;">
      <div>
        <h1 style="font-size: 1.8rem; font-weight: 800; color: var(--va-text-primary); margin: 0; display: flex; align-items: center; gap: 0.5rem;">
          <va-icon name="corporate_fare" color="primary" size="2rem" />
          {{ t('org_tenant_management') }}
        </h1>
        <p style="font-size: 0.88rem; color: var(--va-text-secondary); margin: 0.25rem 0 0 0;">
          {{ t('org_management_desc') }}
        </p>
      </div>
      <va-button color="primary" icon="add" @click="openCreateOrgModal">
        {{ t('create_organization') }}
      </va-button>
    </div>

    <!-- Main Layout -->
    <div style="display: flex; gap: 1.5rem; align-items: flex-start; flex-wrap: wrap;">
      
      <!-- Organization List Card -->
      <va-card style="flex: 1; min-width: 320px;">
        <va-card-title style="display: flex; justify-content: space-between; align-items: center;">
          <span>{{ t('org_list') }}</span>
          <va-badge color="info" :text="String(organizations.length)" />
        </va-card-title>
        <va-card-content>
          <div v-if="loadingOrgs" style="text-align: center; padding: 2rem;">
            <va-progress-circle indeterminate color="primary" />
          </div>
          <div v-else-if="organizations.length === 0" style="text-align: center; padding: 2rem; color: #777;">
            {{ t('no_orgs_registered') }}
          </div>
          <div v-else style="display: flex; flex-direction: column; gap: 0.75rem;">
            <div
              v-for="org in organizations"
              :key="org.id"
              @click="selectOrganization(org)"
              style="padding: 1rem; border-radius: 8px; border: 1px solid var(--va-background-border); cursor: pointer; transition: all 0.2s ease; background: var(--va-background-element);"
              :style="{
                borderColor: selectedOrg?.id === org.id ? 'var(--va-primary)' : 'var(--va-background-border)',
                boxShadow: selectedOrg?.id === org.id ? '0 4px 12px rgba(21, 101, 192, 0.15)' : 'none'
              }"
            >
              <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.35rem;">
                <span style="font-weight: 700; font-size: 1.05rem; color: var(--va-text-primary);">
                  {{ org.displayName || org.name }}
                </span>
                <va-badge :color="org.isActive ? 'success' : 'danger'" :text="org.isActive ? t('active_status') : t('inactive_status')" size="small" />
              </div>
              <div style="font-size: 0.8rem; color: var(--va-text-secondary); font-family: monospace;">
                ID: {{ org.id }}
              </div>
              <div v-if="org.description" style="font-size: 0.85rem; color: var(--va-text-secondary); margin-top: 0.35rem;">
                {{ org.description }}
              </div>
            </div>
          </div>
        </va-card-content>
      </va-card>

      <!-- Selected Organization Detail Card -->
      <va-card v-if="selectedOrg" style="flex: 2; min-width: 480px;">
        <va-card-title style="display: flex; align-items: center; justify-content: space-between;">
          <div style="display: flex; align-items: center; gap: 0.5rem;">
            <va-icon name="apartment" color="primary" />
            <span>{{ selectedOrg.displayName || selectedOrg.name }}</span>
          </div>
          <span style="font-size: 0.8rem; color: var(--va-text-secondary); font-weight: normal;">
            {{ t('system_org_info') }}
          </span>
        </va-card-title>
        
        <va-card-content>
          <va-tabs v-model="activeTab" style="margin-bottom: 1.25rem;">
            <template #tabs>
              <va-tab name="info">{{ t('basic_info') }}</va-tab>
              <va-tab name="depts">{{ t('dept_team_management') }}</va-tab>
              <va-tab name="roles">{{ t('rbac_role_management') }}</va-tab>
            </template>
          </va-tabs>

          <!-- Tab 1: Basic Info -->
          <div v-if="activeTab === 'info'" style="display: flex; flex-direction: column; gap: 1.25rem;">
            <div class="row" style="display: flex; flex-wrap: wrap; gap: 1rem;">
              <va-input
                v-model="editOrgForm.displayName"
                :label="t('org_display_name')"
                style="flex: 1; min-width: 220px;"
              />
              <va-input
                v-model="editOrgForm.name"
                :label="t('org_sys_code')"
                readonly
                style="flex: 1; min-width: 220px; background-color: #f8f9fa;"
              />
            </div>
            <va-input
              v-model="editOrgForm.description"
              type="textarea"
              :label="t('org_description')"
              style="width: 100%;"
            />
            <div style="display: flex; justify-content: flex-end;">
              <va-button color="success" icon="save" @click="saveOrgInfo">
                {{ t('save_changes') }}
              </va-button>
            </div>
          </div>

          <!-- Tab 2: Departments & Teams Hierarchy Tree -->
          <div v-else-if="activeTab === 'depts'" style="display: flex; flex-direction: column; gap: 1.25rem;">
            <div style="display: flex; justify-content: space-between; align-items: center;">
              <div>
                <h4 style="margin: 0; font-weight: 700; color: var(--va-text-primary);">
                  {{ t('dept_structure') || '소속 부서 및 팀 계층 구조 (Tree View)' }}
                </h4>
                <p style="margin: 0.25rem 0 0 0; font-size: 0.82rem; color: var(--va-text-secondary);">
                  조직 - 상위 부서 - 하위 부서 - 팀 N단계 계층 구조
                </p>
              </div>
              <div style="display: flex; gap: 0.5rem;">
                <va-button size="small" preset="secondary" icon="add" @click="openCreateDeptModal(null)">
                  + {{ t('add_department') || '부서 추가' }}
                </va-button>
                <va-button size="small" preset="secondary" icon="add" @click="openCreateTeamModal(null)">
                  + {{ t('add_team') || '팀 추가' }}
                </va-button>
              </div>
            </div>

            <div v-if="departments.length === 0" style="padding: 2.5rem; text-align: center; color: #777; background: var(--va-background-secondary); border-radius: 8px; border: 1px solid var(--va-background-border);">
              <va-icon name="account_tree" size="large" color="secondary" style="margin-bottom: 0.5rem;" />
              <div>{{ t('no_depts_added') || '등록된 부서가 없습니다.' }}</div>
            </div>

            <!-- Root Tree View Container -->
            <div v-else style="border: 1px solid var(--va-background-border); border-radius: 10px; padding: 1.25rem; background: var(--va-background-element);">
              <!-- Organization Root Node -->
              <div style="display: flex; align-items: center; justify-content: space-between; padding: 0.75rem 1.25rem; border-radius: 8px; background: var(--va-background-secondary); border: 1.5px solid var(--va-primary); box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);">
                <div style="display: flex; align-items: center; gap: 0.65rem;">
                  <va-icon name="corporate_fare" color="primary" size="medium" />
                  <span style="font-weight: 800; font-size: 1.05rem; color: var(--va-text-primary);">
                    {{ selectedOrg.displayName || selectedOrg.name }}
                  </span>
                  <va-chip size="small" color="primary">{{ getLabel('organization', '조직') }}</va-chip>
                </div>
                <va-button size="small" color="primary" preset="secondary" icon="add" @click="openCreateDeptModal(null)">
                  + {{ getLabel('add_root_dept', '최상위 부서 추가') }}
                </va-button>
              </div>

              <!-- N-Level Recursive Sub-Department & Team Tree Branches -->
              <div style="margin-top: 0.5rem;">
                <OrgTreeItem
                  v-for="dept in rootDepartments"
                  :key="dept.id"
                  :node="dept"
                  @add-subdept="openCreateDeptModal"
                  @add-team="openCreateTeamModal"
                  @edit-dept="openEditDeptModal"
                  @delete-dept="openDeleteDeptConfirmModal"
                />
              </div>
            </div>
          </div>

          <!-- Tab 3: RBAC Roles -->
          <div v-else-if="activeTab === 'roles'" style="display: flex; flex-direction: column; gap: 1.25rem;">
            <div style="display: flex; justify-content: space-between; align-items: center;">
              <h4 style="margin: 0; font-weight: 700; color: var(--va-text-primary);">
                {{ t('system_custom_roles') }}
              </h4>
              <va-button size="small" preset="secondary" icon="add" @click="openCreateRoleModal">
                + {{ t('add_role') }}
              </va-button>
            </div>

            <div style="display: flex; flex-direction: column; gap: 0.85rem;">
              <div v-for="role in roles" :key="role.id" style="border: 1px solid var(--va-background-border); border-radius: 8px; padding: 1rem; background: var(--va-background-secondary);">
                <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.5rem;">
                  <div style="display: flex; align-items: center; gap: 0.5rem;">
                    <va-badge :color="role.isSystemRole ? 'primary' : 'warning'" :text="role.name" />
                    <span style="font-weight: 700; font-size: 0.95rem; color: var(--va-text-primary);">{{ role.displayName || role.name }}</span>
                  </div>
                  <span style="font-size: 0.8rem; color: var(--va-text-secondary);">{{ role.description }}</span>
                </div>
                <div style="display: flex; gap: 0.4rem; flex-wrap: wrap; margin-top: 0.5rem;">
                  <va-chip v-for="perm in (role.permissions || [])" :key="perm" size="small" color="success" outline>
                    {{ perm }}
                  </va-chip>
                </div>
              </div>
            </div>
          </div>

        </va-card-content>
      </va-card>

    </div>

    <!-- Create Organization Modal -->
    <va-modal v-model="showCreateOrgModalFlag" :title="t('create_new_org')" hide-default-actions size="small" :prevent-click-outside="true" :no-outside-dismiss="true">
      <div style="padding: 1rem; display: flex; flex-direction: column; gap: 1rem;">
        <va-input v-model="newOrgForm.name" :label="t('org_code_placeholder')" required />
        <va-input v-model="newOrgForm.displayName" :label="t('org_display_name_placeholder')" required />
        <va-input v-model="newOrgForm.description" type="textarea" :label="t('org_description')" />
        <div style="display: flex; justify-content: flex-end; gap: 0.5rem; margin-top: 1rem;">
          <va-button preset="secondary" @click="showCreateOrgModalFlag = false">{{ t('cancel') }}</va-button>
          <va-button color="primary" @click="saveNewOrg">{{ t('create_organization') }}</va-button>
        </div>
      </div>
    </va-modal>

    <!-- Create Department Modal -->
    <va-modal v-model="showCreateDeptModalFlag" :title="t('add_new_dept')" hide-default-actions size="small" :prevent-click-outside="true" :no-outside-dismiss="true">
      <div style="padding: 1rem; display: flex; flex-direction: column; gap: 1rem;">
        <va-select
          v-model="newDeptForm.parentDepartmentId"
          :options="departments"
          value-by="id"
          text-by="name"
          :label="getLabel('parent_dept', '상위 부서 (미선택 시 최상위 부서)')"
          clearable
        />
        <va-input v-model="newDeptForm.name" :label="t('dept_name')" required />
        <va-input v-model="newDeptForm.description" type="textarea" :label="t('description')" />
        <div style="display: flex; justify-content: flex-end; gap: 0.5rem; margin-top: 1rem;">
          <va-button preset="secondary" @click="showCreateDeptModalFlag = false">{{ t('cancel') }}</va-button>
          <va-button color="primary" @click="saveNewDept">{{ t('add_department') }}</va-button>
        </div>
      </div>
    </va-modal>

    <!-- Edit Department Modal -->
    <va-modal v-model="showEditDeptModalFlag" :title="getLabel('edit_dept', '부서/조직 정보 수정')" hide-default-actions size="small" :prevent-click-outside="true" :no-outside-dismiss="true">
      <div style="padding: 1rem; display: flex; flex-direction: column; gap: 1rem;">
        <va-select
          v-model="editDeptForm.parentDepartmentId"
          :options="availableParentDepts"
          value-by="id"
          text-by="name"
          :label="getLabel('parent_dept', '상위 부서 (미선택 시 최상위 부서)')"
          clearable
        />
        <va-input v-model="editDeptForm.name" :label="getLabel('dept_name', '부서/조직명')" required />
        <va-input v-model="editDeptForm.description" type="textarea" :label="getLabel('description', '설명')" />
        <div style="display: flex; justify-content: flex-end; gap: 0.5rem; margin-top: 1rem;">
          <va-button preset="secondary" @click="showEditDeptModalFlag = false">{{ getLabel('cancel', '취소') }}</va-button>
          <va-button color="primary" @click="saveEditDept">{{ getLabel('save', '저장') }}</va-button>
        </div>
      </div>
    </va-modal>

    <!-- Delete Department Confirm Modal -->
    <va-modal v-model="showDeleteDeptModalFlag" :title="getLabel('delete_dept', '부서/조직 삭제')" hide-default-actions size="small" :prevent-click-outside="true" :no-outside-dismiss="true">
      <div style="padding: 1.25rem 0; text-align: center;">
        <div style="width: 50px; height: 50px; border-radius: 50%; background: rgba(229, 57, 53, 0.12); color: #b91c1c; display: flex; align-items: center; justify-content: center; margin: 0 auto 1rem auto;">
          <va-icon name="warning" size="2rem" color="danger" />
        </div>
        <h4 style="margin: 0 0 0.5rem 0; font-weight: 700;">{{ targetDeletingDept?.name }}</h4>
        <p style="font-size: 0.9rem; color: var(--va-text-secondary); margin-bottom: 1.5rem;">
          정말 이 부서/조직을 삭제하시겠습니까? 삭제 후에는 복구할 수 없습니다.
        </p>
        <div style="display: flex; justify-content: center; gap: 0.75rem;">
          <va-button preset="secondary" @click="showDeleteDeptModalFlag = false">{{ getLabel('cancel', '취소') }}</va-button>
          <va-button color="danger" @click="confirmDeleteDept">{{ getLabel('delete', '삭제') }}</va-button>
        </div>
      </div>
    </va-modal>

    <!-- Create Team Modal -->
    <va-modal v-model="showCreateTeamModalFlag" :title="t('add_new_team')" hide-default-actions size="small" :prevent-click-outside="true" :no-outside-dismiss="true">
      <div style="padding: 1rem; display: flex; flex-direction: column; gap: 1rem;">
        <va-select v-model="newTeamForm.departmentId" :options="departments" value-by="id" text-by="name" :label="t('belongs_to_dept')" required />
        <va-input v-model="newTeamForm.name" :label="t('team_name')" required />
        <va-input v-model="newTeamForm.description" type="textarea" :label="t('description')" />
        <div style="display: flex; justify-content: flex-end; gap: 0.5rem; margin-top: 1rem;">
          <va-button preset="secondary" @click="showCreateTeamModalFlag = false">{{ t('cancel') }}</va-button>
          <va-button color="primary" @click="saveNewTeam">{{ t('add_team') }}</va-button>
        </div>
      </div>
    </va-modal>

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
import OrgTreeItem from '~/components/OrgTreeItem.vue'
import { ref, onMounted, computed } from 'vue'
import { useCookie } from '#app'

const { t } = useI18n()

const getLabel = (key, fallback) => {
  const res = t(key)
  return (!res || res === key) ? fallback : res
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
const organizations = ref([])
const loadingOrgs = ref(false)
const selectedOrg = ref(null)
const activeTab = ref('info')

const departments = ref([])
const teams = ref([])
const roles = ref([])

const editOrgForm = ref({ name: '', displayName: '', description: '' })
const showCreateOrgModalFlag = ref(false)
const newOrgForm = ref({ name: '', displayName: '', description: '' })

const showCreateDeptModalFlag = ref(false)
const newDeptForm = ref({ parentDepartmentId: null, name: '', description: '' })

const showEditDeptModalFlag = ref(false)
const editDeptForm = ref({ id: null, parentDepartmentId: null, name: '', description: '' })

const showDeleteDeptModalFlag = ref(false)
const targetDeletingDept = ref(null)

const availableParentDepts = computed(() => {
  if (!editDeptForm.value.id) return departments.value
  return departments.value.filter(d => d.id !== editDeptForm.value.id)
})

const openEditDeptModal = (dept) => {
  editDeptForm.value = {
    id: dept.id,
    parentDepartmentId: dept.parentDepartmentId || null,
    name: dept.name,
    description: dept.description || ''
  }
  showEditDeptModalFlag.value = true
}

const saveEditDept = async () => {
  if (!selectedOrg.value || !editDeptForm.value.name) return
  try {
    await $fetch(`/api/organizations/${selectedOrg.value.id}/departments/${editDeptForm.value.id}`, {
      method: 'PUT',
      headers: { Authorization: `Bearer ${token.value}` },
      body: editDeptForm.value
    })
    showEditDeptModalFlag.value = false
    await loadOrgDetails(selectedOrg.value.id)
    showCustomAlert(t('dept_updated_success') || '부서 정보가 성공적으로 수정되었습니다.', t('update_success') || '수정 완료', t('notification') || '알림', 'success')
  } catch (e) {
    showCustomAlert('Failed to update department: ' + (e.message || String(e)), t('error') || 'Error', t('notification') || 'Notification', 'error')
  }
}

const openDeleteDeptConfirmModal = (dept) => {
  targetDeletingDept.value = dept
  showDeleteDeptModalFlag.value = true
}

const confirmDeleteDept = async () => {
  if (!selectedOrg.value || !targetDeletingDept.value) return
  try {
    await $fetch(`/api/organizations/${selectedOrg.value.id}/departments/${targetDeletingDept.value.id}`, {
      method: 'DELETE',
      headers: { Authorization: `Bearer ${token.value}` }
    })
    showDeleteDeptModalFlag.value = false
    await loadOrgDetails(selectedOrg.value.id)
    showCustomAlert('부서가 성공적으로 삭제되었습니다.', t('delete_success') || '삭제 완료', t('notification') || '알림', 'success')
  } catch (e) {
    showCustomAlert('Failed to delete department: ' + (e.message || String(e)), t('error') || 'Error', t('notification') || 'Notification', 'error')
  }
}

const showCreateTeamModalFlag = ref(false)
const newTeamForm = ref({ departmentId: null, name: '', description: '' })

const rootDepartments = computed(() => {
  if (!departments.value) return []
  
  const map = {}
  departments.value.forEach(d => {
    map[d.id] = {
      ...d,
      subDepts: [],
      teams: []
    }
  })

  teams.value.forEach(t => {
    if (t.departmentId && map[t.departmentId]) {
      map[t.departmentId].teams.push(t)
    }
  })

  const roots = []
  departments.value.forEach(d => {
    const node = map[d.id]
    if (d.parentDepartmentId && map[d.parentDepartmentId]) {
      map[d.parentDepartmentId].subDepts.push(node)
    } else {
      roots.push(node)
    }
  })

  return roots
})

const openCreateDeptModal = (parentDeptId = null) => {
  newDeptForm.value = { parentDepartmentId: parentDeptId, name: '', description: '' }
  showCreateDeptModalFlag.value = true
}

const openCreateTeamModal = (deptId = null) => {
  newTeamForm.value = { departmentId: deptId || (departments.value[0]?.id || null), name: '', description: '' }
  showCreateTeamModalFlag.value = true
}

const fetchOrganizations = async () => {
  loadingOrgs.value = true
  try {
    const res = await $fetch('/api/organizations', {
      headers: { Authorization: `Bearer ${token.value}` }
    })
    organizations.value = res || []
    if (organizations.value.length > 0 && !selectedOrg.value) {
      selectOrganization(organizations.value[0])
    }
  } catch (e) {
    console.error('Failed to fetch organizations:', e)
  } finally {
    loadingOrgs.value = false
  }
}

const selectOrganization = async (org) => {
  selectedOrg.value = org
  editOrgForm.value = { ...org }
  await loadOrgDetails(org.id)
}

const loadOrgDetails = async (orgId) => {
  try {
    const [deptsRes, teamsRes, rolesRes] = await Promise.all([
      $fetch(`/api/organizations/${orgId}/departments`, { headers: { Authorization: `Bearer ${token.value}` } }),
      $fetch(`/api/organizations/${orgId}/teams`, { headers: { Authorization: `Bearer ${token.value}` } }),
      $fetch(`/api/roles/org/${orgId}`, { headers: { Authorization: `Bearer ${token.value}` } })
    ])
    departments.value = deptsRes || []
    teams.value = teamsRes || []
    roles.value = rolesRes || []
  } catch (e) {
    console.error('Failed to load org details:', e)
  }
}

const getTeamsForDept = (deptId) => {
  return teams.value.filter(t => t.departmentId === deptId)
}

const openCreateOrgModal = () => {
  newOrgForm.value = { name: '', displayName: '', description: '' }
  showCreateOrgModalFlag.value = true
}

const saveNewOrg = async () => {
  if (!newOrgForm.value.name) return
  try {
    const created = await $fetch('/api/organizations', {
      method: 'POST',
      headers: { Authorization: `Bearer ${token.value}` },
      body: newOrgForm.value
    })
    showCreateOrgModalFlag.value = false
    await fetchOrganizations()
    if (created) selectOrganization(created)
    showCustomAlert(t('org_created_success') || 'Organization created successfully.', t('creation_success') || 'Creation Success', t('notification') || 'Notification', 'success')
  } catch (e) {
    showCustomAlert('Failed to create organization: ' + (e.message || String(e)), t('error') || 'Error', t('notification') || 'Notification', 'error')
  }
}

const saveOrgInfo = async () => {
  if (!selectedOrg.value) return
  try {
    showCustomAlert(t('org_updated_success') || 'Organization info updated successfully.', t('update_success') || 'Update Success', t('notification') || 'Notification', 'success')
  } catch (e) {
    console.error(e)
  }
}



const saveNewDept = async () => {
  if (!selectedOrg.value || !newDeptForm.value.name) return
  try {
    await $fetch(`/api/organizations/${selectedOrg.value.id}/departments`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${token.value}` },
      body: newDeptForm.value
    })
    showCreateDeptModalFlag.value = false
    await loadOrgDetails(selectedOrg.value.id)
    showCustomAlert(t('dept_created_success') || 'Department created successfully.', t('creation_success') || 'Creation Success', t('notification') || 'Notification', 'success')
  } catch (e) {
    showCustomAlert('Failed to create department: ' + (e.message || String(e)), t('error') || 'Error', t('notification') || 'Notification', 'error')
  }
}



const saveNewTeam = async () => {
  if (!selectedOrg.value || !newTeamForm.value.name) return
  try {
    await $fetch(`/api/organizations/${selectedOrg.value.id}/teams`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${token.value}` },
      body: newTeamForm.value
    })
    showCreateTeamModalFlag.value = false
    await loadOrgDetails(selectedOrg.value.id)
    showCustomAlert(t('team_created_success') || 'Team created successfully.', t('creation_success') || 'Creation Success', t('notification') || 'Notification', 'success')
  } catch (e) {
    showCustomAlert('Failed to create team: ' + (e.message || String(e)), t('error') || 'Error', t('notification') || 'Notification', 'error')
  }
}

const openCreateRoleModal = () => {
  showCustomAlert(t('role_creation_placeholder') || 'Role creation modal placeholder', t('info') || 'Info', t('notification') || 'Notification', 'warning')
}

onMounted(() => {
  fetchOrganizations()
})
</script>
