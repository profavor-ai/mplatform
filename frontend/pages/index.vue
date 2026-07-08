<template>
  <div>
    <h1 style="font-size: 2rem; font-weight: bold; margin-bottom: 1.5rem;">{{ currentLocale === 'ko' ? '대시보드' : 'Dashboard' }}</h1>
    
    <div style="display: grid; grid-template-columns: repeat(3, 1fr); gap: 1rem; margin-bottom: 2rem;">
      <va-card>
        <va-card-title>{{ currentLocale === 'ko' ? '총 도메인 수' : 'Total Domains' }}</va-card-title>
        <va-card-content style="font-size: 2rem; font-weight: bold;">
          {{ stats?.totalDomains || 0 }}
        </va-card-content>
      </va-card>
      <va-card>
        <va-card-title>{{ currentLocale === 'ko' ? '결재 대기 중' : 'Pending Approvals' }}</va-card-title>
        <va-card-content style="font-size: 2rem; font-weight: bold; color: #E42222;">
          {{ stats?.pendingApprovals || 0 }}
        </va-card-content>
      </va-card>
      <va-card>
        <va-card-title>{{ currentLocale === 'ko' ? '활성 레코드' : 'Active Records' }}</va-card-title>
        <va-card-content style="font-size: 2rem; font-weight: bold; color: #3D9209;">
          {{ stats?.activeRecords || 0 }}
        </va-card-content>
      </va-card>
    </div>
    
    <div style="display: grid; grid-template-columns: 2fr 1fr; gap: 1rem; margin-bottom: 2rem;">
      <va-card>
        <va-card-title>{{ currentLocale === 'ko' ? '레코드 생성 추이' : 'Record Creation Trends' }}</va-card-title>
        <va-card-content>
          <ClientOnly>
            <v-chart style="height: 350px; width: 100%;" :option="chartOption" autoresize />
          </ClientOnly>
        </va-card-content>
      </va-card>

      <va-card>
        <va-card-title>{{ currentLocale === 'ko' ? '나의 할 일 목록' : 'My To-Do List' }}</va-card-title>
        <va-card-content>
          <div v-if="!todos || todos.length === 0" style="text-align: center; color: #777; margin-top: 2rem;">
            {{ currentLocale === 'ko' ? '대기 중인 작업이 없습니다.' : 'No pending tasks. You\'re all caught up!' }}
          </div>
          <div v-else style="display: flex; flex-direction: column; gap: 1rem;">
            <div v-for="todo in todos" :key="todo.id" style="border: 1px solid #eee; padding: 1rem; border-radius: 8px; display: flex; align-items: center; justify-content: space-between; gap: 1rem;">
              <div style="flex: 1; display: flex; flex-direction: column;">
                <div style="display: flex; align-items: center; gap: 0.5rem; margin-bottom: 0.5rem;">
                  <va-badge :text="todo.stepType" :color="todo.stepType === 'CONSENSUS' ? 'warning' : 'danger'" />
                  <va-badge :text="getActionTypeLabel(todo.approvalRequest.changes)" color="info" outline />
                </div>
                <div style="font-size: 0.85rem; color: #555; line-height: 1.5;">
                  <div v-if="todo.approvalRequest.classificationNode">
                    <strong>{{ currentLocale === 'ko' ? '도메인' : 'Domain' }}:</strong> {{ todo.approvalRequest.classificationNode.domainName?.[currentLocale] || todo.approvalRequest.classificationNode.domainName?.['en'] || 'Unknown' }}<br/>
                    <strong>{{ currentLocale === 'ko' ? '분류' : 'Classification' }}:</strong> {{ todo.approvalRequest.classificationNode.name?.[currentLocale] || todo.approvalRequest.classificationNode.name?.['en'] || 'Unknown' }}
                  </div>
                  <div style="margin-top: 0.25rem;">
                    <strong>{{ currentLocale === 'ko' ? '기안자' : 'Requester' }}:</strong> {{ getUserName(todo.approvalRequest.requesterId) }}
                  </div>
                  <div><strong>{{ currentLocale === 'ko' ? '기안일' : 'Date' }}:</strong> {{ new Date(todo.approvalRequest.createdAt).toLocaleDateString() }}</div>
                  <div style="font-size: 0.75rem; color: #888; margin-top: 0.25rem;">
                    {{ currentLocale === 'ko' ? '요청 ID' : 'Req ID' }}: {{ todo.approvalRequest.id.substring(0, 8) }}...
                  </div>
                </div>
              </div>
              
              <div style="flex: 1; display: flex; flex-direction: column; justify-content: center; align-items: flex-start; background-color: #f8f9fa; padding: 1rem; border-radius: 6px; border: 1px solid #e0e0e0; min-height: 80px;">
                <div v-if="displayInfo[todo.id]?.displayId || displayInfo[todo.id]?.displayName" style="width: 100%;">
                  <div v-if="displayInfo[todo.id]?.displayId" style="font-size: 0.85rem; font-weight: bold; color: #154ec1; margin-bottom: 0.25rem;">
                    {{ displayInfo[todo.id].idField?.name?.[currentLocale] || displayInfo[todo.id].idField?.name?.ko || displayInfo[todo.id].idField?.name?.en || 'ID' }}: {{ displayInfo[todo.id].displayId }}
                  </div>
                  <div v-if="displayInfo[todo.id]?.displayName" style="font-size: 0.85rem; font-weight: bold; color: #333;">
                    {{ displayInfo[todo.id].nameField?.name?.[currentLocale] || displayInfo[todo.id].nameField?.name?.ko || displayInfo[todo.id].nameField?.name?.en || 'Name' }}: {{ displayInfo[todo.id].displayName }}
                  </div>
                </div>
                <div v-else style="color: #999; font-size: 0.85rem; width: 100%; word-break: break-all;">
                  <em>{{ currentLocale === 'ko' ? '필드 데이터를 기다리는 중...' : 'Waiting for field data...' }}</em><br/>
                  <div style="margin-top: 0.25rem;">
                    <strong>{{ currentLocale === 'ko' ? '원본 데이터' : 'Raw Data' }}:</strong><br/>
                    {{ typeof todo.approvalRequest.changes === 'string' ? todo.approvalRequest.changes : JSON.stringify(todo.approvalRequest.changes) }}
                  </div>
                </div>
              </div>

              <div style="display: flex; align-items: center; justify-content: center; padding-left: 1rem;">
                <va-button size="large" @click="goToApprovals(todo)">{{ currentLocale === 'ko' ? '심사하기' : 'Review' }}</va-button>
              </div>
            </div>
          </div>
        </va-card-content>
      </va-card>
    </div>

    <!-- My Submitted Requests -->
    <h2 style="font-size: 1.5rem; font-weight: bold; margin-bottom: 1rem;">{{ currentLocale === 'ko' ? '내가 상신한 결재 내역' : 'My Submitted Requests' }}</h2>
    <va-card style="margin-bottom: 2rem;">
      <va-card-content>
        <div v-if="!myRequests || myRequests.length === 0" style="text-align: center; color: #777; padding: 1rem;">
          {{ currentLocale === 'ko' ? '상신한 결재가 없습니다.' : 'No requests submitted yet.' }}
        </div>
        <table v-else style="width: 100%; border-collapse: collapse; text-align: left;">
          <thead>
            <tr style="border-bottom: 2px solid #eaeaea;">
              <th style="padding: 10px;">{{ currentLocale === 'ko' ? '요청 ID' : 'Request ID' }}</th>
              <th style="padding: 10px;">{{ currentLocale === 'ko' ? '상태' : 'Status' }}</th>
              <th style="padding: 10px;">{{ currentLocale === 'ko' ? '기안일' : 'Date' }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="req in myRequests" :key="req.id" style="border-bottom: 1px solid #eaeaea;">
              <td style="padding: 10px;">{{ req.id.substring(0, 8) }}...</td>
              <td style="padding: 10px;">
                <va-badge :text="req.status" :color="req.status === 'PENDING' ? 'warning' : (req.status === 'APPROVED' ? 'success' : 'danger')" />
              </td>
              <td style="padding: 10px;">{{ new Date(req.createdAt).toLocaleDateString() }}</td>
            </tr>
          </tbody>
        </table>
      </va-card-content>
    </va-card>

  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const stats = ref(null)
const todos = ref([])
const myRequests = ref([])
const userList = ref([])
const domainList = ref([])
const displayInfo = ref({})

const tokenCookie = useCookie('auth_token')
const userCookie = useCookie('user_data')
const localeCookie = useCookie('locale')
const currentLocale = computed(() => localeCookie.value || 'ko')

const currentUser = computed(() => {
  if (userCookie.value) {
    return typeof userCookie.value === 'string' ? JSON.parse(userCookie.value) : userCookie.value
  }
  return null
})

onMounted(async () => {
  try {
    const headers = { Authorization: `Bearer ${tokenCookie.value}` }
    const myUuid = currentUser.value?.uuid
    
    try {
      const uRes = await fetch('http://localhost:8080/api/auth/users', { headers })
      if (uRes.ok) userList.value = await uRes.json()
    } catch(e) {}

    try {
      domainList.value = await $fetch('/api/domains', { headers })
    } catch(e) {}

    stats.value = await $fetch('/api/dashboard/stats', { headers })
    if (myUuid) {
      todos.value = await $fetch(`/api/approval-requests/todos?assigneeId=${myUuid}`, { headers })
      
      const nodeFieldCache = {}
      const fetchFieldsForNode = async (nodeId) => {
        if (nodeFieldCache[nodeId]) return nodeFieldCache[nodeId]
        try {
          const fields = await $fetch(`/api/nodes/${nodeId}/fields/effective`, { headers })
          nodeFieldCache[nodeId] = fields
          return fields
        } catch(e) {
          return []
        }
      }

      for (const todo of todos.value) {
        if (todo.approvalRequest.targetType === 'RECORD') {
          const domainId = todo.approvalRequest.classificationNode?.domainId
          const domain = domainList.value.find(d => d.id === domainId)
          console.log(`[Dashboard] Todo: ${todo.id}, domainId: ${domainId}, foundDomain:`, domain)
          if (domain) {
            const fields = await fetchFieldsForNode(todo.approvalRequest.classificationNode.id)
            let idField = fields.find(f => f.id === domain.identifierFieldId)
            let nameField = fields.find(f => f.id === domain.displayNameFieldId)
            
            if (!idField && fields.length > 0) idField = fields[0]
            if (!nameField && fields.length > 1) nameField = fields[1]
            
            console.log(`[Dashboard] Fields found for node:`, fields.length, `idField:`, idField, `nameField:`, nameField)

            let payload = {}
            try {
              let parsed = todo.approvalRequest.changes
              if (typeof parsed === 'string') parsed = JSON.parse(parsed)
              if (typeof parsed === 'string') parsed = JSON.parse(parsed) // Handle double-escaped JSON
              
              payload = parsed?.after || parsed || {}
              console.log(`[Dashboard] Payload for todo ${todo.id}:`, payload)
            } catch(e) {
              console.error('[Dashboard] JSON parse error:', e)
            }
            
            displayInfo.value[todo.id] = {
              displayId: idField ? payload[idField.key] : null,
              displayName: nameField ? payload[nameField.key] : null,
              idField: idField ? { ...idField, name: typeof idField.name === 'string' ? JSON.parse(idField.name || '{}') : idField.name } : null,
              nameField: nameField ? { ...nameField, name: typeof nameField.name === 'string' ? JSON.parse(nameField.name || '{}') : nameField.name } : null,
              _debug_idFieldKey: idField?.key || 'None',
              _debug_nameFieldKey: nameField?.key || 'None',
              _debug_payload: payload,
              _debug_fieldsLen: fields.length
            }
            
            console.log(`[Dashboard] displayId: ${todo.displayId}, displayName: ${todo.displayName}`)
          }
        }
      }

      myRequests.value = await $fetch(`/api/approval-requests/my-requests?requesterId=${myUuid}`, { headers })
    }
  } catch (e) {
    console.error('Error fetching dashboard data:', e)
  }
})

const goToApprovals = (todo) => {
  router.push(`/approvals?openModalId=${todo.id}`)
}

const getActionTypeLabel = (changes) => {
  if (!changes) return currentLocale.value === 'ko' ? '생성' : 'CREATE'
  try {
    const deepParse = (val) => {
      try {
        if (typeof val === 'string') return deepParse(JSON.parse(val))
        return val
      } catch(e) { return val }
    }
    const parsed = deepParse(changes)
    if (parsed && typeof parsed === 'object' && ('before' in parsed || 'after' in parsed)) {
      return currentLocale.value === 'ko' ? '변경' : 'UPDATE'
    }
  } catch(e) {}
  return currentLocale.value === 'ko' ? '생성' : 'CREATE'
}

const getUserName = (uuid) => {
  if (!uuid) return ''
  const u = userList.value.find(user => user.uuid === uuid)
  return u ? u.username : uuid
}

const chartOption = ref({
  tooltip: { trigger: 'axis' },
  xAxis: {
    type: 'category',
    data: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']
  },
  yAxis: { type: 'value' },
  series: [
    {
      data: [820, 932, 901, 934, 1290, 1330, 1320],
      type: 'line',
      smooth: true,
      color: '#154EC1',
      areaStyle: {
        color: {
          type: 'linear',
          x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [{
            offset: 0, color: 'rgba(21, 78, 193, 0.5)'
          }, {
            offset: 1, color: 'rgba(21, 78, 193, 0)'
          }]
        }
      }
    }
  ]
})
</script>
