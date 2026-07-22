<template>
  <div class="page-container">
    <div class="page-header">
      <h1 class="page-title">{{ $t('integration.channels.title') }}</h1>
      <va-button color="primary" @click="openCreateModal">
        <va-icon name="add" class="mr-2" /> {{ $t('integration.channels.add') }}
      </va-button>
    </div>

    <!-- Channels Table -->
    <va-card class="mb-4">
      <va-card-content>
        <va-data-table
          :items="channels"
          :columns="columns"
          :loading="isLoading"
          striped
        >
          <template #cell(direction)="{ rowData }">
            <va-badge
              :text="rowData.direction === 'INBOUND' ? $t('integration.channels.inbound') : $t('integration.channels.outbound')"
              :color="rowData.direction === 'INBOUND' ? 'warning' : 'info'"
            />
          </template>
          <template #cell(isActive)="{ rowData }">
            <va-badge
              :text="rowData.isActive ? 'Active' : 'Inactive'"
              :color="rowData.isActive ? 'success' : 'danger'"
            />
          </template>
          <template #cell(createdAt)="{ rowData }">
            {{ formatDate(rowData.createdAt) }}
          </template>
          <template #cell(actions)="{ rowData }">
            <div class="action-buttons">
              <va-button preset="plain" color="primary" @click="openEditModal(rowData)">
                <va-icon name="edit" />
              </va-button>
              <va-button preset="plain" color="danger" @click="confirmDelete(rowData.id)">
                <va-icon name="delete" />
              </va-button>
            </div>
          </template>
        </va-data-table>
      </va-card-content>
    </va-card>

    <!-- Create/Edit Modal -->
    <va-modal v-model="showModal" :title="isEdit ? $t('integration.channels.edit') : $t('integration.channels.add')" size="large" hide-default-actions>
      <div class="p-4" style="min-width: 700px; max-height: 80vh; overflow-y: auto;">
        <va-form ref="form" @submit.prevent="submitForm">
          <div class="grid grid-cols-3 gap-4 mb-4">
            <va-input v-model="formData.name" :label="$t('integration.channels.name')" required />
            <va-select
              v-model="formData.direction"
              :options="directionOptions"
              value-by="value"
              text-by="text"
              :label="$t('integration.channels.direction')"
              @update:modelValue="onDirectionChanged"
              required
            />
            <va-select
              v-model="formData.type"
              :options="['WEB_SERVICE', 'JDBC', 'MESSAGE_QUEUE']"
              :label="$t('integration.channels.type')"
              :disabled="formData.direction === 'INBOUND'"
              required
            />
          </div>

          <!-- Inbound Authentication Config for INBOUND -->
          <va-card v-if="formData.direction === 'INBOUND'" outlined class="mb-4">
            <va-card-title>{{ $t('integration.channels.auth_type') }}</va-card-title>
            <va-card-content>
              <div class="grid grid-cols-2 gap-4">
                <va-select
                  v-model="uiConfig.inboundAuthType"
                  :options="authTypeOptions"
                  value-by="value"
                  text-by="text"
                  :label="$t('integration.channels.auth_type')"
                />
                <div v-if="uiConfig.inboundAuthType !== 'NONE'" class="flex gap-2 items-start">
                  <va-input v-model="uiConfig.inboundSecretToken" :label="$t('integration.channels.secret_token')" placeholder="sec_token_..." class="flex-1" required />
                  <va-button preset="secondary" color="info" icon="autorenew" class="mt-4" @click="generateSecretToken">
                    {{ $t('integration.channels.generate_token') }}
                  </va-button>
                </div>
              </div>
            </va-card-content>
          </va-card>

          <!-- Webhook URL notice for INBOUND WEB_SERVICE -->
          <va-card v-if="formData.direction === 'INBOUND' && formData.type === 'WEB_SERVICE'" outlined class="mb-4" style="background-color: #fffbe6; border-color: #ffe58f;">
            <va-card-title style="color: #d48806; font-weight: bold;">
              <va-icon name="key" class="mr-2" /> {{ $t('integration.channels.webhook_url') }}
            </va-card-title>
            <va-card-content>
              <div class="text-sm text-gray-700 mb-2">
                {{ $t('integration.channels.inbound_notice') }}
              </div>
              <div class="flex gap-2 items-center mb-3">
                <va-input :model-value="getWebhookUrl()" readonly class="flex-1 font-mono text-sm" />
                <va-button size="small" color="warning" icon="content_copy" @click="copyWebhookUrl">
                  {{ $t('integration.channels.webhook_copy') }}
                </va-button>
              </div>
              <div v-if="uiConfig.inboundAuthType !== 'NONE'" class="text-xs text-gray-600 bg-white p-2 rounded border border-amber-200">
                <strong>{{ $t('integration.channels.auth_header_example') }}:</strong>
                <code v-if="uiConfig.inboundAuthType === 'BEARER_TOKEN'" class="ml-2 font-mono text-blue-700">Authorization: Bearer {{ uiConfig.inboundSecretToken || 'secretToken' }}</code>
                <code v-else-if="uiConfig.inboundAuthType === 'API_KEY'" class="ml-2 font-mono text-blue-700">X-API-KEY: {{ uiConfig.inboundSecretToken || 'secretToken' }}</code>
              </div>
            </va-card-content>
          </va-card>

          <!-- Domain & Node Selection -->
          <va-card outlined class="mb-4">
            <va-card-title>{{ $t('integration.channels.select_domain_node') }}</va-card-title>
            <va-card-content>
              <div class="grid grid-cols-2 gap-4">
                <va-select
                  v-model="selectedDomainId"
                  :options="domains"
                  value-by="id"
                  text-by="name"
                  :label="$t('integration.channels.select_domain') + (formData.direction === 'INBOUND' ? ' *' : '')"
                  :rules="[v => formData.direction !== 'INBOUND' || !!v || $t('integration.channels.domain_required_for_inbound')]"
                  @update:modelValue="onDomainSelected"
                  clearable
                />
                <va-select
                  v-model="formData.nodeId"
                  :options="nodes"
                  value-by="id"
                  text-by="name"
                  :label="$t('integration.channels.select_node') + (formData.direction === 'INBOUND' ? ' *' : '')"
                  :rules="[v => formData.direction !== 'INBOUND' || !!v || $t('integration.channels.node_required_for_inbound')]"
                  :disabled="!selectedDomainId"
                  clearable
                />
              </div>
            </va-card-content>
          </va-card>

          <!-- Channel Config UI (OUTBOUND 전용) -->
          <va-card v-if="formData.direction === 'OUTBOUND'" outlined class="mb-4">
            <va-card-title style="display: flex; justify-content: space-between; align-items: center; width: 100%;">
              <span>{{ $t('integration.channels.detail_config') }}</span>
              <va-button size="small" preset="secondary" color="info" icon="cloud_done" @click="testConnection" :loading="isTesting" style="flex: 0 0 auto; width: max-content;">{{ $t('integration.channels.test_connection') }}</va-button>
            </va-card-title>
            <va-card-content>
              <template v-if="formData.type === 'WEB_SERVICE'">
                <va-input v-model="uiConfig.wsUrl" :label="$t('integration.channels.ws_url')" placeholder="http://api.example.com/webhook" required class="mb-3 w-full" />
                <va-select v-model="uiConfig.wsMethod" :options="['POST', 'PUT', 'GET']" :label="$t('integration.channels.ws_method')" class="mb-3 w-full" />
                
                <div class="mt-4">
                  <div class="flex justify-between items-center mb-2">
                    <span class="font-bold">HTTP Headers</span>
                    <va-button size="small" preset="secondary" icon="add" @click="addWsHeader">{{ $t('integration.channels.add_header') }}</va-button>
                  </div>
                  <div v-if="uiConfig.wsHeaders.length === 0" class="text-sm text-gray-500 mb-2">{{ $t('integration.channels.no_headers') }}</div>
                  <div v-for="(header, index) in uiConfig.wsHeaders" :key="index" class="flex gap-2 mb-2 items-start">
                    <va-input v-model="header.key" placeholder="Header Name (ex: Authorization)" class="flex-1" required />
                    <va-input v-model="header.value" placeholder="Header Value (ex: Bearer token...)" class="flex-1" required />
                    <va-button preset="plain" color="danger" icon="remove_circle" @click="removeWsHeader(index)" class="mt-1" />
                  </div>
                </div>
              </template>
              <template v-else-if="formData.type === 'JDBC'">
                <va-input v-model="uiConfig.jdbcUrl" :label="$t('integration.channels.db_url')" placeholder="jdbc:mysql://localhost:3306/db" required class="mb-3 w-full" />
                <div class="grid grid-cols-2 gap-4">
                  <va-input v-model="uiConfig.jdbcUser" :label="$t('integration.channels.db_user')" class="mb-3" />
                  <va-input v-model="uiConfig.jdbcPassword" type="password" :label="$t('integration.channels.db_password')" class="mb-3" />
                </div>
                <va-input v-model="uiConfig.jdbcTable" :label="$t('integration.channels.db_table')" placeholder="integration_data" required class="mb-3 w-full" />
              </template>
              <template v-else-if="formData.type === 'MESSAGE_QUEUE'">
                <va-input v-model="uiConfig.mqBroker" :label="$t('integration.channels.mq_broker')" placeholder="kafka://localhost:9092" required class="mb-3 w-full" />
                <va-input v-model="uiConfig.mqTopic" :label="$t('integration.channels.mq_topic')" placeholder="events.data.changed" required class="mb-3 w-full" />
              </template>
            </va-card-content>
          </va-card>

          <!-- Field Mapping UI -->
          <va-card outlined class="mb-4">
            <va-card-title style="display: flex; justify-content: space-between; align-items: center; width: 100%;">
              <span>{{ $t('integration.channels.field_mapping') }}</span>
              <va-button size="small" preset="secondary" icon="add" @click="addMapping" style="flex: 0 0 auto; width: max-content;">{{ $t('integration.channels.add_field') }}</va-button>
            </va-card-title>
            <va-card-content>
              <div class="text-xs text-gray-500 mb-2">
                * {{ formData.direction === 'INBOUND' ? $t('integration.channels.mapping_desc_inbound') : $t('integration.channels.mapping_desc') }}
              </div>
              <va-input 
                v-if="formData.direction === 'INBOUND'" 
                v-model="uiMappingRootPath" 
                :label="$t('integration.channels.mapping_root_path')" 
                :placeholder="$t('integration.channels.mapping_root_path_placeholder')" 
                class="mb-3 w-full" 
                clearable 
              />
              <div style="height: 300px; width: 100%;">
                <client-only>
                  <ag-grid-vue
                    v-if="showModal"
                    style="width: 100%; height: 100%;"
                    :theme="gridTheme"
                    :columnDefs="mappingColumnDefs"
                    :rowData="uiMappings"
                    @grid-ready="onMappingGridReady"
                    @cell-value-changed="onMappingCellValueChanged"
                  >
                  </ag-grid-vue>
                </client-only>
              </div>
            </va-card-content>
          </va-card>

          <va-checkbox v-model="formData.isActive" :label="$t('integration.channels.is_active')" class="mb-4" />

        </va-form>
      </div>
      <template #footer>
        <div class="flex justify-end gap-3 w-full mt-2 pt-2">
          <va-button preset="secondary" color="secondary" @click="showModal = false">{{ $t('cancel') }}</va-button>
          <va-button color="primary" @click="submitForm">{{ $t('save') }}</va-button>
        </div>
      </template>
    </va-modal>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useCookie } from '#app'
import { useToast } from 'vuestic-ui'
import { AgGridVue } from 'ag-grid-vue3'
import { useAgGridTheme } from '~/composables/useAgGridTheme'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
const { gridTheme } = useAgGridTheme()
const { init } = useToast()
const token = useCookie('auth_token')
const channels = ref([])
const domains = ref([])
const nodes = ref([])
const domainFields = ref([])
const selectedDomainId = ref(null)

const isLoading = ref(false)
const isTesting = ref(false)
const showModal = ref(false)
const isEdit = ref(false)
const form = ref(null)

const directionOptions = computed(() => [
  { value: 'OUTBOUND', text: t('integration.channels.outbound') },
  { value: 'INBOUND', text: t('integration.channels.inbound') }
])

const authTypeOptions = computed(() => [
  { value: 'BEARER_TOKEN', text: t('integration.channels.auth_bearer') },
  { value: 'API_KEY', text: t('integration.channels.auth_api_key') },
  { value: 'NONE', text: t('integration.channels.auth_none') }
])

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

const columns = computed(() => [
  { key: 'name', label: t('integration.channels.name'), sortable: true },
  { key: 'direction', label: t('integration.channels.direction'), sortable: true },
  { key: 'type', label: t('integration.channels.type'), sortable: true },
  { key: 'isActive', label: t('integration.channels.status'), sortable: true },
  { key: 'createdAt', label: t('integration.channels.created_at'), sortable: true },
  { key: 'actions', label: t('integration.channels.management'), width: '100px' }
])

// Base Entity Model
const initialForm = {
  name: '',
  direction: 'OUTBOUND',
  type: 'WEB_SERVICE',
  nodeId: null,
  configJson: '{}',
  mappingConfigJson: '{}',
  isActive: true
}

const getWebhookUrl = () => {
  const config = useRuntimeConfig()
  const apiBaseUrl = config.public.apiBaseUrl || 'http://localhost:8080'
  const channelIdStr = editingId || '{channelId}'
  return `${apiBaseUrl}/api/integration/inbound/${channelIdStr}`
}

const copyWebhookUrl = () => {
  const url = getWebhookUrl()
  if (navigator.clipboard) {
    navigator.clipboard.writeText(url)
    init({ message: t('integration.channels.copied'), color: 'success' })
  }
}

const formData = ref({ ...initialForm })
let editingId = null

// UI Bindings
const uiConfig = ref({
  wsUrl: '', wsMethod: 'POST', wsHeaders: [],
  jdbcUrl: '', jdbcUser: '', jdbcPassword: '', jdbcTable: '',
  mqBroker: '', mqTopic: '',
  inboundAuthType: 'BEARER_TOKEN', inboundSecretToken: ''
})
const uiMappings = ref([])
const uiMappingRootPath = ref('')

const generateSecretToken = () => {
  const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789'
  let tokenStr = 'sec_'
  for (let i = 0; i < 24; i++) {
    tokenStr += chars.charAt(Math.floor(Math.random() * chars.length))
  }
  uiConfig.value.inboundSecretToken = tokenStr
}

const addWsHeader = () => {
  uiConfig.value.wsHeaders.push({ key: '', value: '' })
}
const removeWsHeader = (index) => {
  uiConfig.value.wsHeaders.splice(index, 1)
}

const onDirectionChanged = (newDir) => {
  if (newDir === 'INBOUND') {
    formData.value.type = 'WEB_SERVICE'
  }
}

const testConnection = async () => {
  if (formData.value.direction === 'INBOUND') return

  // Config Validation before testing
  if (formData.value.type === 'WEB_SERVICE' && !uiConfig.value.wsUrl) return init({ message: t('integration.channels.err_ws_url_required'), color: 'warning' })
  if (formData.value.type === 'JDBC' && !uiConfig.value.jdbcUrl) return init({ message: t('integration.channels.err_db_url_required'), color: 'warning' })
  if (formData.value.type === 'MESSAGE_QUEUE' && !uiConfig.value.mqBroker) return init({ message: t('integration.channels.err_mq_broker_required'), color: 'warning' })

  // Temporarily serialize ui config to test payload
  let config = {}
  if (formData.value.type === 'WEB_SERVICE') {
    config = { url: uiConfig.value.wsUrl, method: uiConfig.value.wsMethod, headers: uiConfig.value.wsHeaders.filter(h => h.key && h.value) }
  } else if (formData.value.type === 'JDBC') {
    config = { url: uiConfig.value.jdbcUrl, user: uiConfig.value.jdbcUser, password: uiConfig.value.jdbcPassword, table: uiConfig.value.jdbcTable }
  } else if (formData.value.type === 'MESSAGE_QUEUE') {
    config = { broker: uiConfig.value.mqBroker, topic: uiConfig.value.mqTopic }
  }

  isTesting.value = true
  try {
    const res = await $fetch('/api/admin/integration/channels/test-connection', {
      method: 'POST',
      headers: { Authorization: `Bearer ${token.value}` },
      body: {
        type: formData.value.type,
        configJson: JSON.stringify(config)
      }
    })
    
    if (res.success) {
      init({ message: res.message, color: 'success' })
    } else {
      init({ message: res.message, color: 'danger', duration: 5000 })
    }
  } catch (e) {
    console.error('Test connection error:', e)
    init({ message: t('integration.channels.err_test_connection'), color: 'danger' })
  } finally {
    isTesting.value = false
  }
}

const parseI18nName = (nameObj) => {
  if (!nameObj) return ''
  if (typeof nameObj === 'object') {
    return nameObj.ko || nameObj.en || JSON.stringify(nameObj)
  }
  if (typeof nameObj === 'string' && nameObj.startsWith('{')) {
    try {
      const parsed = JSON.parse(nameObj)
      return parsed.ko || parsed.en || nameObj
    } catch (e) {
      return nameObj
    }
  }
  return nameObj
}

const fetchDomains = async () => {
  try {
    const data = await $fetch('/api/domains', {
      headers: { Authorization: `Bearer ${token.value}` }
    })
    domains.value = data.map(d => {
      return { ...d, name: parseI18nName(d.name) }
    })
  } catch (e) { console.error('Failed to load domains', e) }
}

const fetchNodesAndFields = async (domainId) => {
  if (!domainId) {
    nodes.value = []
    domainFields.value = []
    return
  }
  try {
    const [nodesRes, fieldsRes] = await Promise.all([
      $fetch(`/api/domains/${domainId}/nodes/tree`, { headers: { Authorization: `Bearer ${token.value}` } }),
      $fetch(`/api/domains/${domainId}/fields`, { headers: { Authorization: `Bearer ${token.value}` } })
    ])
    
    // Flatten node tree for select dropdown
    const flatNodes = []
    const flatten = (items, prefix = '') => {
      items.forEach(item => {
        flatNodes.push({ id: item.id, name: prefix + parseI18nName(item.name) })
        if (item.children && item.children.length > 0) {
          flatten(item.children, prefix + '-- ')
        }
      })
    }
    flatten(nodesRes)
    nodes.value = flatNodes
    
    // Fields
    domainFields.value = fieldsRes.map(f => ({ code: f.key, name: `${parseI18nName(f.name)} (${f.key})` }))
  } catch (e) {
    console.error('Failed to load nodes/fields', e)
  }
}

const onDomainSelected = (domainId) => {
  formData.value.nodeId = null
  fetchNodesAndFields(domainId)
}

// AG-Grid Settings
let mappingGridApi = null
const onMappingGridReady = (params) => {
  mappingGridApi = params.api
  params.api.sizeColumnsToFit()
}

const validateSpelExpression = (expr) => {
  if (!expr) return true
  let openBrackets = 0
  let openParens = 0
  let inSingleQuote = false
  let inDoubleQuote = false
  
  for (let i = 0; i < expr.length; i++) {
    const c = expr[i]
    if (c === "'" && !inDoubleQuote) inSingleQuote = !inSingleQuote
    else if (c === '"' && !inSingleQuote) inDoubleQuote = !inDoubleQuote
    else if (!inSingleQuote && !inDoubleQuote) {
      if (c === '[') openBrackets++
      else if (c === ']') openBrackets--
      else if (c === '(') openParens++
      else if (c === ')') openParens--
      
      if (openBrackets < 0 || openParens < 0) return false
    }
  }
  return openBrackets === 0 && openParens === 0 && !inSingleQuote && !inDoubleQuote
}

const mappingColumnDefs = computed(() => {
  const isInbound = formData.value.direction === 'INBOUND'
  return [
    { 
      field: 'targetField', 
      headerName: isInbound ? t('integration.channels.target_field_inbound') : t('integration.channels.target_field'), 
      flex: 1, 
      minWidth: 220, 
      editable: true 
    },
    { 
      field: 'selectedField', 
      headerName: isInbound ? t('integration.channels.domain_field') : t('integration.channels.domain_field'), 
      flex: 1, 
      minWidth: 160,
      editable: true,
      cellEditor: 'agSelectCellEditor',
      cellEditorParams: () => {
        return {
          values: domainFields.value.map(f => f.code)
        }
      },
      valueFormatter: (params) => {
        if (!params.value) return ''
        const field = domainFields.value.find(f => f.code === params.value)
        return field ? field.name : params.value
      }
    },
    { 
      field: 'sourceExpression', 
    headerName: t('integration.channels.source_expr'), 
    flex: 3, 
    minWidth: 250, 
    editable: true,
    cellStyle: (params) => {
      if (!validateSpelExpression(params.value)) {
        return { backgroundColor: '#ffebee', color: 'red', border: '1px solid red' }
      }
      return { backgroundColor: 'transparent', color: 'inherit', border: '1px solid transparent' }
    }
  },
  {
    headerName: t('integration.channels.management'),
    width: 120,
    suppressSizeToFit: true,
    cellStyle: { textAlign: 'center' },
    cellRenderer: (params) => {
      const eDiv = document.createElement('div')
      eDiv.innerHTML = `<button type="button" style="color: red; cursor: pointer; border: none; background: none; font-weight: bold;">${t('delete')}</button>`
      const btn = eDiv.querySelector('button')
      btn.addEventListener('click', (e) => {
        e.preventDefault()
        e.stopPropagation()
        params.api.applyTransaction({ remove: [params.node.data] })
      })
      return eDiv
    }
  }
  ]
})

const onMappingCellValueChanged = (event) => {
  if (event.column.colId === 'selectedField' && event.newValue) {
    if (formData.value.direction === 'INBOUND') {
      // Inbound: 외부(소스) -> 내부(타겟). 도메인 필드를 선택하면 타겟 필드(내부 필드)로 자동 지정
      event.node.setDataValue('targetField', event.newValue)
    } else {
      // Outbound: 내부(소스) -> 외부(타겟). 도메인 필드를 선택하면 소스 표현식으로 자동 지정
      event.node.setDataValue('sourceExpression', `payload['${event.newValue}']`)
    }
  }
}

const addMapping = () => {
  if (mappingGridApi) {
    mappingGridApi.applyTransaction({ add: [{ targetField: '', selectedField: null, sourceExpression: '' }] })
  } else {
    uiMappings.value.push({ targetField: '', selectedField: null, sourceExpression: '' })
  }
}

const serializeUiData = () => {
  let config = {}
  if (formData.value.type === 'WEB_SERVICE') {
    config = { 
        url: uiConfig.value.wsUrl, 
        method: uiConfig.value.wsMethod,
        headers: uiConfig.value.wsHeaders.filter(h => h.key && h.value) 
      }
  } else if (formData.value.type === 'JDBC') {
    config = { url: uiConfig.value.jdbcUrl, user: uiConfig.value.jdbcUser, password: uiConfig.value.jdbcPassword, table: uiConfig.value.jdbcTable }
  } else if (formData.value.type === 'MESSAGE_QUEUE') {
    config = { broker: uiConfig.value.mqBroker, topic: uiConfig.value.mqTopic }
  }
  
  if (formData.value.direction === 'INBOUND') {
    config.authType = uiConfig.value.inboundAuthType || 'BEARER_TOKEN'
    config.secretToken = uiConfig.value.inboundSecretToken || ''
  }

  // UI 복원을 위해 domainId 저장
  if (selectedDomainId.value) {
    config.domainId = selectedDomainId.value
  }

  formData.value.configJson = JSON.stringify(config)

  // Extract from AG-Grid
  if (mappingGridApi) {
    const rowData = []
    mappingGridApi.forEachNode(node => rowData.push(node.data))
    uiMappings.value = rowData
  }

  if (uiMappings.value.length > 0) {
    const mappingsToSave = uiMappings.value.map(m => ({
      targetField: m.targetField,
      sourceExpression: m.sourceExpression
    }))
    const mappingObj = { mappings: mappingsToSave }
    if (uiMappingRootPath.value) {
      mappingObj.rootPath = uiMappingRootPath.value
    }
    formData.value.mappingConfigJson = JSON.stringify(mappingObj)
  } else {
    formData.value.mappingConfigJson = JSON.stringify({})
  }
}

const deserializeUiData = (row) => {
  uiConfig.value = {
    wsUrl: '', wsMethod: 'POST', wsHeaders: [],
    jdbcUrl: '', jdbcUser: '', jdbcPassword: '', jdbcTable: '',
    mqBroker: '', mqTopic: '',
    inboundAuthType: 'BEARER_TOKEN', inboundSecretToken: ''
  }
  uiMappings.value = []
  uiMappingRootPath.value = ''
  selectedDomainId.value = null
  
  if (!row) return

  try {
    const config = JSON.parse(row.configJson || '{}')
    
    // 복원된 domainId가 있으면 도메인 필드와 노드 목록 불러오기
    if (config.domainId) {
      selectedDomainId.value = config.domainId
      fetchNodesAndFields(config.domainId)
    }

    if (row.direction === 'INBOUND') {
      uiConfig.value.inboundAuthType = config.authType || 'BEARER_TOKEN'
      uiConfig.value.inboundSecretToken = config.secretToken || ''
    }

    if (row.type === 'WEB_SERVICE') {
      uiConfig.value.wsUrl = config.url || ''
      uiConfig.value.wsMethod = config.method || 'POST'
      uiConfig.value.wsHeaders = config.headers || []
    } else if (row.type === 'JDBC') {
      uiConfig.value.jdbcUrl = config.url || ''
      uiConfig.value.jdbcUser = config.user || ''
      uiConfig.value.jdbcPassword = config.password || ''
      uiConfig.value.jdbcTable = config.table || ''
    } else if (row.type === 'MESSAGE_QUEUE') {
      uiConfig.value.mqBroker = config.broker || ''
      uiConfig.value.mqTopic = config.topic || ''
    }
  } catch (e) { console.error('Failed to parse configJson', e) }

  try {
    const mapping = JSON.parse(row.mappingConfigJson || '{}')
    uiMappingRootPath.value = mapping.rootPath || ''
    
    if (mapping && mapping.mappings && Array.isArray(mapping.mappings)) {
      uiMappings.value = mapping.mappings.map(m => {
        // Try to guess the selected field from sourceExpression
        let selectedField = null
        const match = m.sourceExpression?.match(/(?:payload|#this)\['(.+)'\]/)
        if (match) selectedField = match[1]
        
        return {
          targetField: m.targetField,
          sourceExpression: m.sourceExpression,
          selectedField
        }
      })
    }
    // Refresh grid if open
    if (mappingGridApi) {
      mappingGridApi.setGridOption('rowData', uiMappings.value)
    }
  } catch (e) { console.error('Failed to parse mappingConfigJson', e) }
}

const fetchChannels = async () => {
  isLoading.value = true
  try {
    const data = await $fetch('/api/admin/integration/channels', {
      headers: { Authorization: `Bearer ${token.value}` }
    })
    channels.value = data
  } catch (e) {
    console.error('Failed to load channels:', e)
  } finally {
    isLoading.value = false
  }
}

const openCreateModal = () => {
  isEdit.value = false
  editingId = null
  formData.value = { ...initialForm }
  deserializeUiData(formData.value)
  showModal.value = true
}

const openEditModal = (row) => {
  isEdit.value = true
  editingId = row.id
  formData.value = { ...row }
  if (!formData.value.direction) {
    formData.value.direction = 'OUTBOUND'
  }
  deserializeUiData(row)
  // NOTE: In a full implementation we would fetch the domainId by nodeId here 
  // so the domain dropdown and fields are pre-populated.
  showModal.value = true
}

const submitForm = async () => {
  if (!form.value.validate()) return

  if (formData.value.direction === 'INBOUND') {
    if (!selectedDomainId.value) {
      init({ message: t('integration.channels.domain_required_for_inbound'), color: 'danger' })
      return
    }
    if (!formData.value.nodeId) {
      init({ message: t('integration.channels.node_required_for_inbound'), color: 'danger' })
      return
    }
  }

  serializeUiData()

  try {
    if (isEdit.value) {
      await $fetch(`/api/admin/integration/channels/${editingId}`, {
        method: 'PUT',
        headers: { Authorization: `Bearer ${token.value}` },
        body: formData.value
      })
    } else {
      await $fetch('/api/admin/integration/channels', {
        method: 'POST',
        headers: { Authorization: `Bearer ${token.value}` },
        body: formData.value
      })
    }
    showModal.value = false
    fetchChannels()
  } catch (e) {
    console.error('Failed to save channel:', e)
  }
}

const confirmDelete = async (id) => {
  try {
    await $fetch(`/api/admin/integration/channels/${id}`, {
      method: 'DELETE',
      headers: { Authorization: `Bearer ${token.value}` }
    })
    fetchChannels()
  } catch (e) {
    console.error('Failed to delete channel:', e)
  }
}

onMounted(() => {
  fetchChannels()
  fetchDomains()
})
</script>

<style scoped>
.page-container {
  padding: 1rem;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
}
.page-title {
  font-size: 1.5rem;
  font-weight: 700;
  margin: 0;
}
.action-buttons {
  display: flex;
  gap: 0.25rem;
}
</style>
