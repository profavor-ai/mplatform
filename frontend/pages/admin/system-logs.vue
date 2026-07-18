<template>
  <div class="menu-logs-container">
    <!-- Tab Navigation -->
    <va-tabs v-model="activeTab" class="mb-4" style="border-bottom: 1px solid var(--va-background-border);">
      <template #tabs>
        <va-tab name="access">Menu Access Logs</va-tab>
        <va-tab name="login">Login Logs</va-tab>
        <va-tab name="error">Error Logs</va-tab>
      </template>
    </va-tabs>

    <!-- 1. Menu Access Logs Tab -->
    <div v-if="activeTab === 'access'">
      <va-card class="mb-4">
        <va-card-title>
          <div class="d-flex justify-space-between align-center w-100">
            <h2>{{ $t('menu_access_statistics') || 'Menu Access Statistics' }}</h2>
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
          <div class="d-flex justify-space-between align-center w-100">
            <h2>{{ $t('menu_access_logs') || 'Menu Access Logs' }}</h2>
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
          <div class="d-flex justify-space-between align-center w-100">
            <h2>Login Frequency Statistics</h2>
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
          <div class="d-flex justify-space-between align-center w-100">
            <h2>{{ $t('user_login_logs') || 'Login History Logs' }}</h2>
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
          <div class="d-flex justify-space-between align-center w-100">
            <h2>System Error Logs</h2>
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
          <strong>Logged At: </strong> {{ new Date(selectedError.loggedAt).toLocaleString() }} (User: {{ selectedError.userId }})
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

if (process.client) {
  use([CanvasRenderer, BarChart, LineChart, TitleComponent, TooltipComponent, GridComponent])
}

const { locale } = useI18n()
const { gridTheme, autoSizeStrategy } = useAgGridTheme()
const token = useCookie('auth_token')
const activeTab = ref('access')
const isMounted = ref(false)

// Multilingual Menu Translation Helper
const getMenuName = (path) => {
  if (!path) return ''
  const isKo = locale.value === 'ko'
  const cleanPath = path.toLowerCase().replace(/\/+$/, '')
  
  if (cleanPath === '' || cleanPath === '/') {
    return isKo ? '대시보드' : 'Dashboard'
  }
  if (cleanPath.includes('/schema')) {
    return isKo ? '스키마' : 'Schema'
  }
  if (cleanPath.includes('/records')) {
    return isKo ? '레코드' : 'Records'
  }
  if (cleanPath.includes('/approvals')) {
    return isKo ? '결재' : 'Approvals'
  }
  if (cleanPath === '/admin') {
    return isKo ? '관리자' : 'Admin'
  }
  if (cleanPath.includes('/admin/users')) {
    return isKo ? '사용자 관리' : 'User Management'
  }
  if (cleanPath.includes('/admin/menus')) {
    return isKo ? '메뉴 관리' : 'Menu Management'
  }
  if (cleanPath.includes('/admin/system-logs') || cleanPath.includes('/admin/menu-logs') || cleanPath.includes('/admin/menulogs')) {
    return isKo ? '시스템 로그' : 'System Logs'
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
      return new Date(params.value).toLocaleString()
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
      return new Date(params.value).toLocaleString()
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
      return new Date(params.value).toLocaleString()
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

// Watch language change to dynamically translate chart labels
watch(locale, () => {
  updateChart()
  if (gridApi.value) {
    gridApi.value.refreshCells({ force: true })
  }
})

onMounted(() => {
  isMounted.value = true
  updateChart()
  updateLoginChart()
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
