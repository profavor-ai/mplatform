<template>
  <div class="menu-logs-container">
    <!-- Tab Navigation -->
    <va-tabs v-model="activeTab" class="mb-4" style="border-bottom: 1px solid var(--va-background-border);">
      <template #tabs>
        <va-tab name="access">Menu Access Logs</va-tab>
        <va-tab name="login">Login Logs</va-tab>
        <va-tab name="error">Error Logs</va-tab>
        <va-tab name="integration">Integration Logs</va-tab>
      </template>
    </va-tabs>

    <!-- 1. Menu Access Logs Tab -->
    <div v-if="activeTab === 'access'">
      <va-card class="mb-4">
        <va-card-title>
          <div class="flex justify-between items-center w-full">
            <h2 style="text-transform: none; font-size: 1.2rem; margin: 0; color: var(--va-dark);">{{ $t('menu_access_statistics') || 'Menu Access Statistics' }}</h2>
            <va-button-toggle
              v-model="accessChartPeriod"
              preset="secondary"
              border-color="primary"
              :options="[
                { label: 'Daily', value: 'daily' },
                { label: 'Monthly', value: 'monthly' },
                { label: 'Yearly', value: 'yearly' }
              ]"
              @update:model-value="updateChart"
            />
          </div>
        </va-card-title>
        <va-card-content>
          <div style="height: 300px; width: 100%;">
            <client-only>
              <v-chart v-if="isMounted" :option="chartOption" autoresize />
            </client-only>
          </div>
        </va-card-content>
      </va-card>

      <va-card>
        <va-card-title>
          <div class="flex justify-between items-center w-full">
            <h2 style="text-transform: none; font-size: 1.2rem; margin: 0; color: var(--va-dark);">{{ $t('menu_access_logs') || 'Menu Access Logs' }}</h2>
            <va-button icon="refresh" preset="secondary" @click="refreshGrid">Refresh</va-button>
          </div>
        </va-card-title>
        <va-card-content>
          <div style="height: 500px; width: 100%;">
            <client-only>
              <ag-grid-vue
                v-if="isMounted"
                style="width: 100%; height: 100%;"
                :theme="gridTheme"
                :columnDefs="columnDefs"
                :rowModelType="'serverSide'"
                :serverSideDatasource="datasource"
                :pagination="true"
                :paginationPageSize="20"
                :cacheBlockSize="20"
                @grid-ready="onGridReady"
              >
              </ag-grid-vue>
            </client-only>
          </div>
        </va-card-content>
      </va-card>
    </div>

    <!-- 2. Login Logs Tab -->
    <div v-if="activeTab === 'login'">
      <va-card class="mb-4">
        <va-card-title>
          <div class="flex justify-between items-center w-full">
            <h2 style="text-transform: none; font-size: 1.2rem; margin: 0; color: var(--va-dark);">Login Frequency Statistics</h2>
            <va-button-toggle
              v-model="loginChartPeriod"
              preset="secondary"
              border-color="primary"
              :options="[
                { label: 'Daily', value: 'daily' },
                { label: 'Monthly', value: 'monthly' },
                { label: 'Yearly', value: 'yearly' }
              ]"
              @update:model-value="updateLoginChart"
            />
          </div>
        </va-card-title>
        <va-card-content>
          <div style="height: 300px; width: 100%;">
            <client-only>
              <v-chart v-if="isMounted" :option="loginChartOption" autoresize />
            </client-only>
          </div>
        </va-card-content>
      </va-card>

      <va-card>
        <va-card-title>
          <div class="flex justify-between items-center w-full">
            <h2 style="text-transform: none; font-size: 1.2rem; margin: 0; color: var(--va-dark);">{{ $t('user_login_logs') || 'Login History Logs' }}</h2>
            <va-button icon="refresh" preset="secondary" @click="refreshLoginGrid">Refresh</va-button>
          </div>
        </va-card-title>
        <va-card-content>
          <div style="height: 500px; width: 100%;">
            <client-only>
              <ag-grid-vue
                v-if="isMounted"
                style="width: 100%; height: 100%;"
                :theme="gridTheme"
                :columnDefs="loginColumnDefs"
                :rowModelType="'serverSide'"
                :serverSideDatasource="loginDatasource"
                :pagination="true"
                :paginationPageSize="20"
                :cacheBlockSize="20"
                @grid-ready="onLoginGridReady"
              >
              </ag-grid-vue>
            </client-only>
          </div>
        </va-card-content>
      </va-card>
    </div>

    <!-- 3. Error Logs Tab -->
    <div v-if="activeTab === 'error'">
      <va-card>
        <va-card-title>
          <div class="flex justify-between items-center w-full">
            <h2 style="text-transform: none; font-size: 1.2rem; margin: 0; color: var(--va-dark);">System Error Logs</h2>
            <va-button icon="refresh" preset="secondary" @click="refreshErrorGrid">Refresh</va-button>
          </div>
        </va-card-title>
        <va-card-content>
          <div class="mb-2" style="font-size: 0.85rem; color: var(--va-text-secondary);">
            * Double click on any row to view full stack trace details.
          </div>
          <div style="height: 600px; width: 100%;">
            <client-only>
              <ag-grid-vue
                v-if="isMounted"
                style="width: 100%; height: 100%;"
                :theme="gridTheme"
                :columnDefs="errorColumnDefs"
                :rowModelType="'serverSide'"
                :serverSideDatasource="errorDatasource"
                :pagination="true"
                :paginationPageSize="20"
                :cacheBlockSize="20"
                @grid-ready="onErrorGridReady"
                @row-double-clicked="onRowDoubleClicked"
              >
              </ag-grid-vue>
            </client-only>
          </div>
        </va-card-content>
      </va-card>
    </div>

    <!-- 4. Integration Logs Tab -->
    <div v-if="activeTab === 'integration'">
      <va-card>
        <va-card-title>
          <div class="flex justify-between items-center w-full">
            <h2 style="text-transform: none; font-size: 1.2rem; margin: 0; color: var(--va-dark);">Integration Monitoring Logs</h2>
            <div class="flex items-center" style="gap: 1rem;">
              <va-select
                v-model="selectedChannelId"
                :options="channelOptions"
                value-by="id"
                text-by="name"
                placeholder="모든 채널 (All Channels)"
                clearable
                style="width: 250px"
                @update:modelValue="fetchIntegrationLogs(0)"
              />
              <va-button icon="refresh" preset="secondary" @click="fetchIntegrationLogs(integrationCurrentPage)">Refresh</va-button>
            </div>
          </div>
        </va-card-title>
        <va-card-content>
          <va-data-table
            :items="integrationLogs"
            :columns="integrationColumns"
            :loading="isIntegrationLoading"
            striped
            hoverable
          >
            <template #cell(status)="{ rowData }">
              <va-badge
                :text="rowData.status"
                :color="rowData.status === 'SUCCESS' ? 'success' : 'danger'"
              />
            </template>
            <template #cell(actions)="{ rowData }">
              <va-button preset="plain" icon="visibility" @click="viewIntegrationDetails(rowData)" />
            </template>
          </va-data-table>
          
          <!-- Pagination -->
          <div class="flex justify-center mt-4" v-if="integrationTotalPages > 0">
            <va-pagination
              v-model="integrationCurrentPage"
              :pages="integrationTotalPages"
              :visible-pages="5"
              @update:modelValue="onIntegrationPageChange"
            />
          </div>
        </va-card-content>
      </va-card>

      <!-- Integration Details Modal -->
      <va-modal v-model="showIntegrationDetailsModal" title="Integration Log Detail" size="large" hide-default-actions>
        <div class="p-4" style="min-width: 600px; max-height: 80vh; overflow-y: auto;" v-if="selectedIntegrationLog">
          <div class="mb-4 flex gap-4">
            <div><strong>Status:</strong> <va-badge :text="selectedIntegrationLog.status" :color="selectedIntegrationLog.status === 'SUCCESS' ? 'success' : 'danger'" /></div>
            <div><strong>Event:</strong> {{ selectedIntegrationLog.eventType }}</div>
            <div><strong>Retry Count:</strong> {{ selectedIntegrationLog.retryCount }}</div>
            <div><strong>Logged At:</strong> {{ selectedIntegrationLog.createdAt }}</div>
          </div>

          <div class="mb-4" v-if="selectedIntegrationLog.errorMessage">
            <label class="font-bold text-red-600 block mb-2">Error Message</label>
            <div class="bg-red-50 p-3 rounded border border-red-200 text-red-800" style="white-space: pre-wrap; font-family: monospace; font-size: 0.85em;">
              {{ selectedIntegrationLog.errorMessage }}
            </div>
          </div>

          <div class="mb-4" v-if="selectedIntegrationLog.stackTrace">
            <label class="font-bold text-red-600 block mb-2">Stack Trace</label>
            <div class="bg-red-50 p-3 rounded border border-red-200 text-red-800 stack-trace-view" style="white-space: pre-wrap; font-family: monospace; font-size: 0.8em; max-height: 200px; overflow-y: auto;">
              {{ selectedIntegrationLog.stackTrace }}
            </div>
          </div>

          <div class="mb-4">
            <label class="font-bold block mb-2">Original Payload</label>
            <div class="bg-gray-100 p-3 rounded" style="white-space: pre-wrap; font-family: monospace; font-size: 0.85em;">
              {{ formatJson(selectedIntegrationLog.originalPayload) }}
            </div>
          </div>

          <div class="mb-4">
            <label class="font-bold block mb-2">Mapped Payload</label>
            <div class="bg-gray-100 p-3 rounded" style="white-space: pre-wrap; font-family: monospace; font-size: 0.85em;">
              {{ formatJson(selectedIntegrationLog.mappedPayload) }}
            </div>
          </div>
          
          <div class="flex justify-end mt-4" style="gap: 1rem;">
            <va-button v-if="selectedIntegrationLog.status === 'FAIL' && (hasPermission('integration:write') || hasPermission('integration:*'))" color="warning" @click="retryIntegrationLog(selectedIntegrationLog.id)">재전송 (Retry)</va-button>
            <va-button color="secondary" @click="showIntegrationDetailsModal = false">Close</va-button>
          </div>
        </div>
      </va-modal>
    </div>

    <!-- Stack Trace Detail Modal -->
    <va-modal v-model="showErrorModal" title="Error Stack Trace Detail" size="large" hide-default-actions>
      <div v-if="selectedError" style="padding: 1rem; box-sizing: border-box; overflow: hidden;">
        <div class="mb-4">
          <strong>Request URI: </strong> <code>{{ selectedError.requestUri }}</code>
        </div>
        <div class="mb-4">
          <strong>Error Message: </strong> <span class="text-danger">{{ selectedError.errorMessage }}</span>
        </div>
        <div class="mb-4">
          <strong>Logged At: </strong> {{ new Date(selectedError.loggedAt).toLocaleString(locale === 'ko' ? 'ko-KR' : 'en-US') }} (User: {{ selectedError.userId }})
        </div>
        <div>
          <strong>Stack Trace:</strong>
          <pre class="stack-trace-view">{{ selectedError.stackTrace }}</pre>
        </div>
        <div class="d-flex justify-end mt-4">
          <va-button color="secondary" @click="showErrorModal = false">Close</va-button>
        </div>
      </div>
    </va-modal>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { AgGridVue } from 'ag-grid-vue3'
if (process.client) {
  import('ag-grid-enterprise')
}
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart, LineChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, GridComponent } from 'echarts/components'
import { useAgGridTheme } from '~/composables/useAgGridTheme'
import { useI18n } from 'vue-i18n'
import { useToast } from 'vuestic-ui'
import { usePermission } from '~/composables/usePermission'

const { hasPermission } = usePermission()

if (process.client) {
  use([CanvasRenderer, BarChart, LineChart, TitleComponent, TooltipComponent, GridComponent])
}

const { locale } = useI18n()
const { gridTheme, autoSizeStrategy } = useAgGridTheme()
const token = useCookie('auth_token')
const { init } = useToast()
const activeTab = ref('access')
const isMounted = ref(false)

const { fetchMenuTree } = useMenu()
const dbMenuMap = ref(new Map())
let isMapLoaded = false

const loadDbMenuMap = async (force = false) => {
  if (isMapLoaded && !force) return
  try {
    const tree = await fetchMenuTree(force)
    const newMap = new Map()
    const traverse = (nodes) => {
      if (!Array.isArray(nodes)) return
      nodes.forEach(node => {
        if (node && node.path) {
          const clean = node.path.toLowerCase().replace(/\/+$/, '')
          newMap.set(clean, node.name)
        }
        if (node.children && node.children.length > 0) {
          traverse(node.children)
        }
      })
    }
    traverse(tree)
    dbMenuMap.value = newMap
    isMapLoaded = true
  } catch (e) {
    console.error('Failed to load menu tree for chart lookup:', e)
  }
}

// Multilingual Menu Translation Helper (Pure DB dynamic lookup - NO hardcoding)
const getMenuName = (path) => {
  if (!path) return ''
  const cleanPath = path.toLowerCase().replace(/\/+$/, '')
  
  if (dbMenuMap.value.has(cleanPath)) {
    const rawName = dbMenuMap.value.get(cleanPath)
    const text = getMultilingualText(rawName)
    if (text) return text
  }

  // Exact path or prefix match against DB menu map
  for (const [menuPath, rawName] of dbMenuMap.value.entries()) {
    if (menuPath && (cleanPath === menuPath || cleanPath.startsWith(menuPath) || menuPath.startsWith(cleanPath))) {
      const text = getMultilingualText(rawName)
      if (text) return text
    }
  }

  return path
}

// ----------------------------------------------------
// 1. Menu Access Logs Data & Setup
// ----------------------------------------------------
const gridApi = ref(null)
const accessChartPeriod = ref('daily')

const columnDefs = ref([
  { field: 'id', headerName: 'ID', width: 100 },
  { field: 'userId', headerName: 'User ID', flex: 1 },
  { 
    field: 'menuName', 
    headerName: 'Menu Name', 
    flex: 1.5,
    valueGetter: (params) => {
      if (!params.data) return ''
      return getMenuName(params.data.menuPath)
    }
  },
  { field: 'menuPath', headerName: 'Accessed Path', flex: 1.5 },
  { field: 'userAgent', headerName: 'User Agent', flex: 2 },
  { field: 'clientIp', headerName: 'Client IP', flex: 1 },
  { 
    field: 'accessedAt', 
    headerName: 'Accessed At', 
    flex: 1.2,
    valueFormatter: (params) => {
      if (!params.value) return ''
      return new Date(params.value).toLocaleString(locale.value === 'ko' ? 'ko-KR' : 'en-US')
    }
  }
])

const chartOption = ref({
  tooltip: {
    trigger: 'axis',
    axisPointer: { type: 'shadow' }
  },
  grid: {
    left: '3%',
    right: '4%',
    bottom: '3%',
    containLabel: true
  },
  xAxis: [
    {
      type: 'category',
      data: [],
      axisTick: { alignWithLabel: true },
      axisLabel: {
        interval: 0,
        rotate: 0
      }
    }
  ],
  yAxis: [
    { type: 'value' }
  ],
  series: [
    {
      name: 'Access Count',
      type: 'bar',
      barWidth: '60%',
      data: []
    }
  ]
})

const onGridReady = (params) => {
  gridApi.value = params.api
}

const datasource = {
  getRows: async (params) => {
    try {
      const page = Math.floor(params.request.startRow / 20)
      
      const response = await $fetch('/api/menus/logs', {
        headers: token.value ? { Authorization: `Bearer ${token.value}` } : {},
        params: {
          page: page,
          size: 20,
          sort: 'accessedAt,desc'
        }
      })
      
      let lastRow = -1
      if (response.content.length < 20) {
        lastRow = params.request.startRow + response.content.length
      } else if (response.totalElements) {
        lastRow = response.totalElements
      }
      
      params.success({ rowData: response.content, rowCount: lastRow })
      updateChart()
      
    } catch (error) {
      console.error('Failed to fetch menu logs:', error)
      params.fail()
    }
  }
}

const updateChart = async () => {
  try {
     await loadDbMenuMap()
     const response = await $fetch('/api/menus/logs', {
        headers: token.value ? { Authorization: `Bearer ${token.value}` } : {},
        params: { page: 0, size: 200, sort: 'accessedAt,desc' }
     })
     
     if (response.content.length === 0) return
     
     // Get latest log's date components for filtering reference
     const latestLogDate = response.content[0].accessedAt
     const targetDay = latestLogDate.substring(0, 10)
     const targetMonth = latestLogDate.substring(0, 7)
     const targetYear = latestLogDate.substring(0, 4)
     
     const pathCounts = {}
     response.content.forEach(log => {
       if (log.accessedAt && log.menuPath) {
         const logDay = log.accessedAt.substring(0, 10)
         const logMonth = log.accessedAt.substring(0, 7)
         const logYear = log.accessedAt.substring(0, 4)
         
         // Filter based on selected period mode
         if (accessChartPeriod.value === 'daily' && logDay !== targetDay) return
         if (accessChartPeriod.value === 'monthly' && logMonth !== targetMonth) return
         if (accessChartPeriod.value === 'yearly' && logYear !== targetYear) return
         
         const translatedName = getMenuName(log.menuPath)
         pathCounts[translatedName] = (pathCounts[translatedName] || 0) + 1
       }
     })
     
     const sortedPaths = Object.keys(pathCounts).sort((a,b) => pathCounts[b] - pathCounts[a]).slice(0, 10)
     const counts = sortedPaths.map(p => pathCounts[p])
     
     chartOption.value.xAxis[0].data = sortedPaths
     chartOption.value.series[0].data = counts
  } catch (error) {
     console.error('Failed to update chart:', error)
  }
}

const refreshGrid = () => {
  if (gridApi.value) {
    gridApi.value.refreshServerSide()
  }
  updateChart()
}

// ----------------------------------------------------
// 2. User Login Logs Data & Setup
// ----------------------------------------------------
const loginGridApi = ref(null)
const loginChartPeriod = ref('daily')

const loginColumnDefs = ref([
  { field: 'id', headerName: 'ID', width: 100 },
  { field: 'userId', headerName: 'User UUID', flex: 1.5 },
  { field: 'username', headerName: 'Username', flex: 1 },
  { field: 'userAgent', headerName: 'User Agent', flex: 2 },
  { field: 'clientIp', headerName: 'Client IP', flex: 1 },
  { 
    field: 'loginAt', 
    headerName: 'Login At', 
    flex: 1,
    valueFormatter: (params) => {
      if (!params.value) return ''
      return new Date(params.value).toLocaleString(locale.value === 'ko' ? 'ko-KR' : 'en-US')
    }
  }
])

const loginChartOption = ref({
  tooltip: {
    trigger: 'axis'
  },
  grid: {
    left: '3%',
    right: '4%',
    bottom: '3%',
    containLabel: true
  },
  xAxis: [
    {
      type: 'category',
      data: [],
      axisTick: { alignWithLabel: true }
    }
  ],
  yAxis: [
    { type: 'value' }
  ],
  series: [
    {
      name: 'Login Count',
      type: 'bar',
      barWidth: '60%',
      data: []
    }
  ]
})

const onLoginGridReady = (params) => {
  loginGridApi.value = params.api
}

const loginDatasource = {
  getRows: async (params) => {
    try {
      const page = Math.floor(params.request.startRow / 20)
      
      const response = await $fetch('/api/auth/login-logs', {
        headers: token.value ? { Authorization: `Bearer ${token.value}` } : {},
        params: {
          page: page,
          size: 20
        }
      })
      
      let lastRow = -1
      if (response.content.length < 20) {
        lastRow = params.request.startRow + response.content.length
      } else if (response.totalElements) {
        lastRow = response.totalElements
      }
      
      params.success({ rowData: response.content, rowCount: lastRow })
      updateLoginChart()
    } catch (error) {
      console.error('Failed to fetch login logs:', error)
      params.fail()
    }
  }
}

const updateLoginChart = async () => {
  try {
     const response = await $fetch('/api/auth/login-logs', {
        headers: token.value ? { Authorization: `Bearer ${token.value}` } : {},
        params: { page: 0, size: 200 }
     })
     
     const dateCounts = {}
     response.content.forEach(log => {
       if (log.loginAt) {
         let key = log.loginAt.substring(0, 10) // default daily
         if (loginChartPeriod.value === 'monthly') {
           key = log.loginAt.substring(0, 7)
         } else if (loginChartPeriod.value === 'yearly') {
           key = log.loginAt.substring(0, 4)
         }
         dateCounts[key] = (dateCounts[key] || 0) + 1
       }
     })
     
     const sortedKeys = Object.keys(dateCounts).sort()
     const counts = sortedKeys.map(k => dateCounts[k])
     
     loginChartOption.value.xAxis[0].data = sortedKeys
     loginChartOption.value.series[0].data = counts
  } catch (error) {
     console.error('Failed to update login chart:', error)
  }
}

const refreshLoginGrid = () => {
  if (loginGridApi.value) {
    loginGridApi.value.refreshServerSide()
  }
  updateLoginChart()
}

// ----------------------------------------------------
// 3. System Error Logs Data & Setup
// ----------------------------------------------------
const errorGridApi = ref(null)
const showErrorModal = ref(false)
const selectedError = ref(null)

const errorColumnDefs = ref([
  { field: 'id', headerName: 'ID', width: 100 },
  { field: 'userId', headerName: 'User ID', flex: 1 },
  { field: 'requestUri', headerName: 'Request URI', flex: 1.5 },
  { field: 'errorMessage', headerName: 'Error Message', flex: 3 },
  { 
    field: 'loggedAt', 
    headerName: 'Logged At', 
    flex: 1.2,
    valueFormatter: (params) => {
      if (!params.value) return ''
      return new Date(params.value).toLocaleString(locale.value === 'ko' ? 'ko-KR' : 'en-US')
    }
  }
])

const onErrorGridReady = (params) => {
  errorGridApi.value = params.api
}

const errorDatasource = {
  getRows: async (params) => {
    try {
      const page = Math.floor(params.request.startRow / 20)
      
      const response = await $fetch('/api/admin/error-logs', {
        headers: token.value ? { Authorization: `Bearer ${token.value}` } : {},
        params: {
          page: page,
          size: 20
        }
      })
      
      let lastRow = -1
      if (response.content.length < 20) {
        lastRow = params.request.startRow + response.content.length
      } else if (response.totalElements) {
        lastRow = response.totalElements
      }
      
      params.success({ rowData: response.content, rowCount: lastRow })
    } catch (error) {
      console.error('Failed to fetch error logs:', error)
      params.fail()
    }
  }
}

const refreshErrorGrid = () => {
  if (errorGridApi.value) {
    errorGridApi.value.refreshServerSide()
  }
}

const onRowDoubleClicked = (event) => {
  if (event.data) {
    selectedError.value = event.data
    showErrorModal.value = true
  }
}

// ----------------------------------------------------
// 4. Integration Logs Data & Setup
// ----------------------------------------------------
const integrationLogs = ref([])
const channelOptions = ref([])
const selectedChannelId = ref(null)
const isIntegrationLoading = ref(false)
const integrationCurrentPage = ref(1)
const integrationTotalPages = ref(0)

const showIntegrationDetailsModal = ref(false)
const selectedIntegrationLog = ref(null)

const integrationColumns = [
  { key: 'channelName', label: 'Channel', sortable: false },
  { key: 'eventType', label: 'Event', sortable: false },
  { key: 'status', label: 'Status', sortable: false },
  { key: 'retryCount', label: 'Retry Count', sortable: false },
  { key: 'createdAt', label: 'Logged At', sortable: false },
  { key: 'actions', label: 'Details', width: '100px' }
]

const fetchChannels = async () => {
  try {
    const data = await $fetch('/api/admin/integration/channels', {
      headers: { Authorization: `Bearer ${token.value}` }
    })
    channelOptions.value = data
  } catch (e) {
    console.error('Failed to load channels:', e)
  }
}

const fetchIntegrationLogs = async (pageIndex) => {
  isIntegrationLoading.value = true
  try {
    const query = new URLSearchParams({
      page: pageIndex > 0 ? pageIndex - 1 : 0,
      size: 20
    })
    if (selectedChannelId.value) {
      query.append('channelId', selectedChannelId.value)
    }
    
    const data = await $fetch(`/api/admin/integration/logs?${query.toString()}`, {
      headers: { Authorization: `Bearer ${token.value}` }
    })
    
    integrationLogs.value = data.content.map(log => {
      const channel = channelOptions.value.find(c => c.id === log.channelId)
      return {
        ...log,
        channelName: channel ? channel.name : 'Unknown',
        createdAt: new Date(log.createdAt).toLocaleString(locale.value === 'ko' ? 'ko-KR' : 'en-US')
      }
    })
    integrationTotalPages.value = data.totalPages
    integrationCurrentPage.value = (data.number + 1)
  } catch (e) {
    console.error('Failed to load integration logs:', e)
  } finally {
    isIntegrationLoading.value = false
  }
}

const onIntegrationPageChange = (page) => {
  fetchIntegrationLogs(page)
}

const viewIntegrationDetails = (log) => {
  selectedIntegrationLog.value = log
  showIntegrationDetailsModal.value = true
}

const formatJson = (str) => {
  if (!str) return 'N/A'
  try {
    return JSON.stringify(JSON.parse(str), null, 2)
  } catch {
    return str
  }
}

const retryIntegrationLog = async (logId) => {
  try {
    await $fetch(`/api/admin/integration/logs/${logId}/retry`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${token.value}` }
    })
    init({ message: '재전송 요청이 완료되었습니다.', color: 'success' })
    showIntegrationDetailsModal.value = false
    fetchIntegrationLogs(integrationCurrentPage.value)
  } catch (e) {
    console.error('Failed to retry integration log:', e)
    init({ message: '재전송 요청 중 오류가 발생했습니다.', color: 'danger' })
  }
}

// Watch language change to dynamically translate chart labels
watch(locale, () => {
  updateChart()
  if (gridApi.value) {
    gridApi.value.refreshCells({ force: true })
  }
})

onMounted(async () => {
  isMounted.value = true
  await loadDbMenuMap()
  updateChart()
  updateLoginChart()
  await fetchChannels()
  fetchIntegrationLogs(1)
})
</script>

<style scoped>
.menu-logs-container {
  padding: 20px;
}
.stack-trace-view {
  background-color: #1f2937;
  color: #f3f4f6;
  padding: 1.5rem;
  border-radius: 8px;
  max-height: 400px;
  overflow-y: auto;
  font-family: 'Courier New', Courier, monospace;
  font-size: 0.85rem;
  white-space: pre-wrap;
}
</style>
