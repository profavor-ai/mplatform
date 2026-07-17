<template>
  <div>
    <h1 style="font-size: 2rem; font-weight: bold; margin-bottom: 1.5rem;">Domain Schema Management</h1>
    
    <div class="schema-layout">
      <!-- Tree Column -->
      <div class="schema-tree-column">
        <va-card>
          <va-card-title>
            Classification Tree
          </va-card-title>
          <va-card-content>
            <div class="schema-tree-wrapper">
              <div v-if="!treeNodes || treeNodes.length === 0" style="padding: 2rem; text-align: center; color: #666;">
                분류체계 트리가 없습니다. 상단의 Domain 버튼을 눌러 새 도메인을 생성해주세요.
              </div>
              <div v-else class="va-tree" style="width: 100%;">
                <SchemaTreeNode 
                  v-for="domain in treeNodes" 
                  :key="domain.id" 
                  :node="domain" 
                  :selectedNode="selectedNode" 
                  @select="onNodeSelected" 
                  @edit="handleNodeEdit" 
                />
              </div>
            </div>
            <div style="display: flex; gap: 0.5rem; margin-top: 1rem;">
              <va-button style="flex: 1" icon="add" @click="openDomainModal(false)">Domain</va-button>
              <va-button style="flex: 1" icon="add" @click="openNodeModal(false)" :disabled="!selectedNode">Node</va-button>
            </div>
            <div style="margin-top: 1rem;">
              <va-button style="width: 100%" color="info" icon="settings" @click="openSectorGroupModal" :disabled="!treeNodes || treeNodes.length === 0">Manage Sectors & Groups</va-button>
            </div>
          </va-card-content>
        </va-card>
      </div>
      
      <!-- Detail Column -->
      <div class="schema-detail-column">
        <va-card v-if="selectedNode" style="flex: 1; display: flex; flex-direction: column; min-height: 0;">
          <va-card-title>
            <va-tabs v-model="activeTab" style="width: 100%;">
              <template #tabs>
                <va-tab>Fields</va-tab>
                <va-tab>Workflows</va-tab>
              </template>
            </va-tabs>
          </va-card-title>
          <va-card-content style="flex: 1; display: flex; flex-direction: column; min-height: 0; padding: 0;">
            <!-- Fields Tab -->
            <div v-show="activeTab === 0" style="flex: 1; display: flex; flex-direction: column; min-height: 0; padding: 1rem;">
              <div class="schema-grid-wrapper">
                <ag-grid-vue
                  style="width: 100%; height: 100%;"
                  :theme="theme"
                  :columnDefs="columnDefs"
                  :rowData="fields"
                  rowSelection="single"
                  :pagination="true"
                  :paginationPageSize="20"
                  :paginationPageSizeSelector="[10, 20, 50]"
                  @grid-ready="onGridReady"
                  @rowDoubleClicked="onRowDoubleClicked"
                />
              </div>
              
              <div style="display: flex; justify-content: flex-end; margin-top: 1rem;">
                <va-button icon="add" @click="openFieldModal(false)">Add Field</va-button>
              </div>
            </div>

            <!-- Workflows Tab -->
            <div v-show="activeTab === 1" style="flex: 1; display: flex; flex-direction: column; padding: 1.5rem; overflow-y: auto;">
              <div v-for="action in ['CREATE', 'UPDATE', 'DELETE']" :key="action" style="margin-bottom: 2rem; padding: 1rem; border: 1px solid #eee; border-radius: 8px;">
                <h3 style="font-weight: bold; margin-bottom: 1rem; color: #333;">{{ action }} Action Workflow</h3>
                <div style="display: flex; gap: 1rem;">
                  <va-select
                    v-model="workflowConfigs[action].consensusUserId"
                    :options="userOptions"
                    value-by="value"
                    text-by="text"
                    label="Consensus User (Optional)"
                    clearable
                    class="w-full"
                  />
                  <va-select
                    v-model="workflowConfigs[action].approverUserId"
                    :options="userOptions"
                    value-by="value"
                    text-by="text"
                    label="Final Approver (Optional)"
                    clearable
                    class="w-full"
                  />
                </div>
              </div>
              <div style="display: flex; justify-content: flex-end;">
                <va-button color="success" icon="save" @click="saveWorkflowConfigs">Save Workflows</va-button>
              </div>
            </div>
          </va-card-content>
        </va-card>
        <va-card v-else>
          <va-card-content style="text-align: center; padding: 3rem; color: #666;">
            Select a Classification Node from the tree to view or add fields.
          </va-card-content>
        </va-card>
      </div>
    </div>

    <!-- Domain Modal -->
    <va-modal v-model="showDomainModal" :title="isEditMode ? 'Edit Domain' : 'Create New Domain'" :ok-text="isEditMode ? 'Save' : 'Create'" cancel-text="Cancel" @ok="saveDomain" :prevent-click-outside="true">
      <div style="display: flex; gap: 1rem;">
        <va-input v-model="newDomain.name.ko" label="Domain Name (KO)" class="mb-4 w-full" />
        <va-input v-model="newDomain.name.en" label="Domain Name (EN)" class="mb-4 w-full" />
      </div>
      <div style="display: flex; gap: 1rem;">
        <va-input v-model="newDomain.description.ko" label="Description (KO)" class="mb-4 w-full" />
        <va-input v-model="newDomain.description.en" label="Description (EN)" class="mb-4 w-full" />
      </div>
      
      <!-- Field Mappings (Only show in Edit mode because we need fields) -->
      <div v-if="isEditMode" style="margin-top: 1rem; border-top: 1px solid #eee; padding-top: 1rem;">
        <div style="margin-bottom: 0.5rem; font-weight: bold; font-size: 0.9rem; color: #666;">Domain Field Mappings (Required)</div>
        <va-select
          v-model="newDomain.identifierFieldId"
          :options="domainFieldOptions"
          value-by="value"
          text-by="text"
          label="Identifier Field (ID)*"
          class="mb-4 w-full"
          :error="mappingError.id"
          :error-messages="['Required']"
        />
        <va-select
          v-model="newDomain.displayNameFieldId"
          :options="domainFieldOptions"
          value-by="value"
          text-by="text"
          label="Display Name Field*"
          class="mb-4 w-full"
          :error="mappingError.name"
          :error-messages="['Required']"
        />
        <va-select
          v-model="newDomain.descriptionFieldId"
          :options="domainFieldOptions"
          value-by="value"
          text-by="text"
          label="Description Field (Optional)"
          class="mb-4 w-full"
          clearable
        />
      </div>
    </va-modal>

    <!-- Node Modal -->
    <va-modal v-model="showNodeModal" :title="isEditMode ? `Edit Node` : `Add Node to ${selectedNode?.label}`" :ok-text="isEditMode ? 'Save' : 'Create'" cancel-text="Cancel" @ok="saveNode">
      <div style="display: flex; gap: 1rem;">
        <va-input v-model="newNode.name.ko" label="Node Name (KO)" class="mb-4 w-full" />
        <va-input v-model="newNode.name.en" label="Node Name (EN)" class="mb-4 w-full" />
      </div>
      <va-input v-model="newNode.order" type="number" label="Order" class="mb-4 w-full" />
    </va-modal>

    <!-- Field Modal -->
    <va-modal v-model="showFieldModal" :title="isEditMode ? `Edit Field` : `Add Field to ${selectedNode?.label}`" hide-default-actions size="large">
      <div style="display: flex; gap: 1rem;">
        <va-input v-model="newField.name.ko" label="Field Name (KO)" class="mb-4 w-full" />
        <va-input v-model="newField.name.en" label="Field Name (EN)" class="mb-4 w-full" />
      </div>
      <va-select 
        v-model="newField.fieldGroupId" 
        :options="groupOptions" 
        value-by="value"
        label="Group (Sector is mapped automatically)" 
        class="mb-4 w-full" 
      />
      <div style="display: flex; gap: 1rem;">
        <va-input v-model="newField.key" label="Field Key" class="mb-4 w-full" />
        <va-input v-model="newField.order" type="number" label="Sort Order" class="mb-4 w-full" />
      </div>
      
      <va-select v-model="newField.type" :options="fieldTypes" label="Field Type" class="mb-4 w-full" />
        
        <va-select 
          v-if="newField.type === 'DOMAIN_REFERENCE'" 
          v-model="newField.targetDomainId" 
          :options="domainOptions" 
          value-by="value"
          label="Target Domain" 
          class="mb-4 w-full" 
        />
      
      <div v-if="['SELECT', 'MULTI_SELECT'].includes(newField.type)" class="mb-4 w-full" style="border: 1px solid #ccc; padding: 1rem; border-radius: 8px;">
        <label style="font-weight: bold; margin-bottom: 0.5rem; display: block;">{{ currentLocale === 'ko' ? '옵션 설정' : 'Options Settings' }}</label>
        
        <div style="margin-bottom: 0.5rem; display: flex; gap: 0.5rem; justify-content: flex-end;">
          <va-button size="small" icon="add" @click="addGridOption">{{ currentLocale === 'ko' ? '옵션 추가' : 'Add Option' }}</va-button>
          <va-button size="small" icon="remove" color="danger" @click="removeSelectedGridOption">{{ currentLocale === 'ko' ? '선택 삭제' : 'Remove Selected' }}</va-button>
        </div>
        
        <ag-grid-vue
          class="ag-theme-alpine"
          style="width: 100%; height: 250px;"
          :columnDefs="optionsColumnDefs"
          :rowData="newFieldOptionsList"
          :defaultColDef="optionsDefaultColDef"
          rowSelection="single"
          @grid-ready="onOptionsGridReady"
        />
      </div>
      
      <div v-else-if="newField.type === 'CALCULATED'" class="mb-4 w-full" style="border: 1px solid #ccc; padding: 1rem; border-radius: 8px;">
        <label style="font-weight: bold; margin-bottom: 0.5rem; display: block;">{{ currentLocale === 'ko' ? '수식 설정 (Formula)' : 'Formula Settings' }}</label>
        <va-textarea
          v-model="newField.formula"
          :placeholder="currentLocale === 'ko' ? '예: ABS(${KEY_A} + ${KEY_B} / 2) * 100' : 'e.g. ABS(${KEY_A} + ${KEY_B} / 2) * 100'"
          class="w-full mb-2"
          :min-rows="3"
          style="font-family: monospace;"
        />
        <va-alert color="info" dense class="w-full" style="font-size: 0.85rem;">
          <strong>{{ currentLocale === 'ko' ? '수식 작성 가이드' : 'Formula Guide' }}</strong><br/>
          - <strong>필드 참조</strong>: <code>${필드_KEY}</code> 형식으로 입력하세요. (예: <code>${PRICE}</code>)<br/>
          - <strong>기본 연산자</strong>: <code>+</code> (더하기), <code>-</code> (빼기), <code>*</code> (곱하기), <code>/</code> (나누기)<br/>
          - <strong>수학 함수</strong>:<br/>
            &nbsp;&nbsp;• <code>ROUND(값, 자리수)</code> : 반올림 (예: <code>ROUND(${PRICE}, 2)</code>)<br/>
            &nbsp;&nbsp;• <code>CEIL(값)</code> : 올림<br/>
            &nbsp;&nbsp;• <code>FLOOR(값)</code> : 내림<br/>
            &nbsp;&nbsp;• <code>ABS(값)</code> : 절대값<br/>
          <span style="color: #d9534f; font-weight: bold;">주의:</span> 참조하는 필드는 반드시 숫자형(NUMBER, DECIMAL, FLOAT, INTEGER)이거나 다른 계산 필드(CALCULATED)여야 합니다.
        </va-alert>
      </div>

      <div style="display: flex; gap: 1rem; margin-top: 1rem; flex-wrap: wrap;">
        <va-checkbox v-model="newField.required" :label="currentLocale === 'ko' ? '필수' : 'Required'" />
        <va-checkbox v-model="newField.isMultiValue" :label="currentLocale === 'ko' ? '다중 값' : 'Multi-Value'" />
        <va-checkbox v-model="newField.isSearchable" :label="currentLocale === 'ko' ? '검색 가능' : 'Searchable'" />
        <va-checkbox v-model="newField.isEncrypted" :label="currentLocale === 'ko' ? '암호화' : 'Encrypted'" />
        <va-checkbox v-model="newField.isReadOnly" :label="currentLocale === 'ko' ? '읽기 전용' : 'Read-Only'" />
        <va-checkbox v-model="newField.isImmutable" :label="currentLocale === 'ko' ? '수정 금지' : 'Immutable'" />
        <va-checkbox v-model="newField.isHidden" :label="currentLocale === 'ko' ? '숨김' : 'Hidden'" />
      </div>

      <div style="display: flex; justify-content: flex-end; gap: 1rem; margin-top: 1.5rem;">
        <va-button preset="secondary" @click="showFieldModal = false">Cancel</va-button>
        <va-button @click="saveField">{{ isEditMode ? 'Save' : 'Create' }}</va-button>
      </div>
    </va-modal>

    <!-- Sector & Group Manager Modal -->
    <va-modal v-model="showSectorGroupModal" title="Manage Sectors & Groups" hide-default-actions size="large">
      <div style="display: flex; gap: 2rem; height: 500px; padding-bottom: 1rem;">
        <div style="flex: 1; border-right: 1px solid #eee; padding-right: 1rem; display: flex; flex-direction: column;">
          <div style="display:flex; justify-content:space-between; margin-bottom: 1rem; align-items: center;">
            <h3 style="font-weight:bold; margin: 0;">Sectors</h3>
            <div style="display:flex; gap: 0.5rem;">
              <va-button size="small" @click="addSectorRow">Add Row</va-button>
              <va-button size="small" color="danger" @click="deleteSelectedSector">Delete Selected</va-button>
            </div>
          </div>
          <div class="ag-theme-alpine" style="flex: 1; width: 100%;">
            <AgGridVue
              style="width: 100%; height: 100%;"
              :columnDefs="sectorColumnDefs"
              :rowData="domainSectors"
              :defaultColDef="sgDefaultColDef"
              @grid-ready="onSectorGridReady"
              @rowValueChanged="onSectorRowSaved"
              editType="fullRow"
              rowSelection="single"
            />
          </div>
        </div>

        <div style="flex: 1; display: flex; flex-direction: column;">
          <div style="display:flex; justify-content:space-between; margin-bottom: 1rem; align-items: center;">
            <h3 style="font-weight:bold; margin: 0;">Groups</h3>
            <div style="display:flex; gap: 0.5rem;">
              <va-button size="small" @click="addGroupRow">Add Row</va-button>
              <va-button size="small" color="danger" @click="deleteSelectedGroup">Delete Selected</va-button>
            </div>
          </div>
          <div class="ag-theme-alpine" style="flex: 1; width: 100%;">
            <AgGridVue
              style="width: 100%; height: 100%;"
              :columnDefs="groupColumnDefs"
              :rowData="domainGroups"
              :defaultColDef="sgDefaultColDef"
              @grid-ready="onGroupGridReady"
              @rowValueChanged="onGroupRowSaved"
              editType="fullRow"
              rowSelection="single"
            />
          </div>
        </div>
      </div>
      <div style="display: flex; justify-content: space-between; margin-top: 1rem;">
        <span style="font-size: 0.85em; color: #888;">* Double-click a row to edit. Press Enter to save.</span>
        <va-button @click="showSectorGroupModal = false">Close</va-button>
      </div>
    </va-modal>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useCookie, useState } from '#app'

import { AgGridVue } from 'ag-grid-vue3'
import { themeBalham } from 'ag-grid-enterprise';
import 'ag-grid-enterprise/styles/ag-grid.css'
import 'ag-grid-enterprise/styles/ag-theme-alpine.css'

const theme = themeBalham;

const currentLocale = useState('locale', () => 'ko')
const token = useCookie('auth_token', { default: () => '' })
const getAuthHeaders = () => token.value ? { Authorization: `Bearer ${token.value}` } : {}

const activeTab = ref(0)
const workflowConfigs = ref({
  CREATE: { consensusUserId: null, approverUserId: null },
  UPDATE: { consensusUserId: null, approverUserId: null },
  DELETE: { consensusUserId: null, approverUserId: null }
})
const userOptions = ref([])
const treeNodes = ref([])
const selectedNode = ref(null)
const fields = ref([])
const gridApi = ref(null)

const showDomainModal = ref(false)
const showNodeModal = ref(false)
const showFieldModal = ref(false)
const showSectorGroupModal = ref(false)

const isEditMode = ref(false)
const editingId = ref(null)

const newDomain = ref({ name: { ko: '', en: '' }, description: { ko: '', en: '' }, identifierFieldId: null, displayNameFieldId: null, descriptionFieldId: null })
const domainFieldOptions = ref([])
const mappingError = ref({ id: false, name: false })

const newNode = ref({ name: { ko: '', en: '' }, order: 1 })
const newField = ref({ 
  name: { ko: '', en: '' }, 
  fieldGroupId: null,
  key: '', 
  type: 'TEXT', 
  options: '', 
  formula: '',
  targetDomainId: null,
  required: false, 
  isMultiValue: false, 
  isSearchable: true, 
  isEncrypted: false,
  isReadOnly: false,
  isImmutable: false,
  isHidden: false,
  order: 0
})

watch(() => newField.value.key, (newVal) => {
  if (newVal) {
    const sanitized = newVal.toUpperCase().replace(/[^A-Z_]/g, '');
    if (newVal !== sanitized) {
      newField.value.key = sanitized;
    }
  }
})

const newFieldOptionsList = ref([])
const optionsGridApi = ref(null)

const optionsDefaultColDef = {
  flex: 1,
  editable: true,
  sortable: true,
  resizable: true,
}

const optionsColumnDefs = [
  { 
    headerName: 'Key', 
    field: 'key',
    valueSetter: params => {
      params.data.key = String(params.newValue || '').toUpperCase().replace(/[^A-Z_]/g, '');
      return true;
    },
    cellStyle: params => {
      if (!params.value || String(params.value).trim() === '') {
        return { backgroundColor: '#fff3cd' } // Soft yellow warning
      }
      return null
    }
  },
  { headerName: 'Label (KO)', field: 'label.ko', valueSetter: params => { params.data.label.ko = params.newValue; return true; } },
  { headerName: 'Label (EN)', field: 'label.en', valueSetter: params => { params.data.label.en = params.newValue; return true; } },
  { headerName: 'Order', field: 'order', valueParser: params => Number(params.newValue) || 0 }
]

const sgDefaultColDef = { flex: 1, sortable: true, resizable: true }

const sectorColumnDefs = [
  { field: 'name.ko', headerName: 'Name (KO)', editable: true },
  { field: 'name.en', headerName: 'Name (EN)', editable: true },
  { field: 'sortOrder', headerName: 'Order', width: 100, flex: 0, editable: true, valueParser: p => Number(p.newValue) || 0 }
]

const groupColumnDefs = [
  { 
    headerName: 'Sector',
    editable: true,
    cellEditor: 'agSelectCellEditor',
    cellEditorParams: params => ({ values: domainSectors.value.map(s => s.id) }),
    valueGetter: p => p.data.sector?.id,
    valueSetter: p => {
      if (!p.data.sector) p.data.sector = {}
      p.data.sector.id = p.newValue
      return true
    },
    valueFormatter: params => {
      const s = domainSectors.value.find(sec => sec.id === params.value)
      return s ? (s.name?.ko || s.name?.en) : ''
    }
  },
  { field: 'name.ko', headerName: 'Name (KO)', editable: true },
  { field: 'name.en', headerName: 'Name (EN)', editable: true },
  { field: 'sortOrder', headerName: 'Order', width: 80, flex: 0, editable: true, valueParser: p => Number(p.newValue) || 0 },
  { 
    field: 'isDefaultOpen', 
    headerName: 'Default Open', 
    editable: true,
    cellEditor: 'agSelectCellEditor',
    cellEditorParams: { values: [true, false] },
    valueFormatter: p => p.value !== false ? 'O' : 'X',
    width: 120,
    flex: 0 
  }
]

const onOptionsGridReady = (params) => {
  optionsGridApi.value = params.api
}

const addGridOption = () => {
  newFieldOptionsList.value.push({ key: '', label: { ko: '', en: '' }, order: 0 })
  if (optionsGridApi.value) {
    optionsGridApi.value.setRowData(newFieldOptionsList.value)
  }
}

const removeSelectedGridOption = () => {
  if (!optionsGridApi.value) return
  const selectedNodes = optionsGridApi.value.getSelectedNodes()
  if (selectedNodes.length > 0) {
    const selectedData = selectedNodes.map(node => node.data)
    newFieldOptionsList.value = newFieldOptionsList.value.filter(opt => !selectedData.includes(opt))
    optionsGridApi.value.setRowData(newFieldOptionsList.value)
  }
}

const domainSectors = ref([])
const domainGroups = ref([])

const sectorGridApi = ref(null)
const groupGridApi = ref(null)

const onSectorGridReady = (params) => sectorGridApi.value = params.api
const onGroupGridReady = (params) => groupGridApi.value = params.api

const sectorOptions = computed(() => {
  return domainSectors.value.map(s => ({
    text: s.name?.[currentLocale.value] || s.name?.ko || s.name?.en || 'Unknown',
    value: s.id
  }))
})

const groupOptions = computed(() => {
  return domainGroups.value.map(g => {
    const sName = g.sector?.name?.[currentLocale.value] || g.sector?.name?.ko || g.sector?.name?.en || 'Unknown Sector';
    const gName = g.name?.[currentLocale.value] || g.name?.ko || g.name?.en || 'Unknown Group';
    return {
      text: `[${sName}] ${gName}`,
      value: g.id
    }
  })
})

const fieldTypes = ['TEXT', 'NUMBER', 'DATE', 'BOOLEAN', 'SELECT', 'DECIMAL', 'FLOAT', 'INTEGER', 'DOMAIN_REFERENCE', 'MULTI_SELECT', 'TIME', 'HTML_TEXT', 'CHECKBOX', 'CALCULATED']

const domainOptions = computed(() => {
  return treeNodes.value.filter(n => n.isDomain).map(d => ({
    value: d.id,
    text: d.label || d.id
  }))
})

const columnDefs = computed(() => [
  { 
    headerName: 'Name', 
    field: 'name', 
    sortable: true,
    flex: 1,
    valueGetter: (params) => {
      if (!params.data) return '';
      const pName = typeof params.data.name === 'string' ? JSON.parse(params.data.name || '{}') : params.data.name;
      return pName?.[currentLocale.value] || pName?.ko || pName?.en || 'Unknown';
    }
  },
  { 
    headerName: 'Sector', 
    field: 'fieldGroup', 
    sortable: true,
    width: 150,
    valueGetter: (params) => {
      if (!params.data || !params.data.fieldGroup || !params.data.fieldGroup.sector) return '';
      const sName = params.data.fieldGroup.sector.name;
      return sName?.[currentLocale.value] || sName?.ko || sName?.en || '';
    }
  },
  { 
    headerName: 'Group', 
    field: 'fieldGroup', 
    sortable: true,
    width: 150,
    valueGetter: (params) => {
      if (!params.data || !params.data.fieldGroup) return '';
      const gName = params.data.fieldGroup.name;
      return gName?.[currentLocale.value] || gName?.ko || gName?.en || '';
    }
  },
  { headerName: 'Key', field: 'key', sortable: true, flex: 1 },
  { headerName: 'Order', field: 'order', sortable: true, width: 90 },
  { headerName: 'Type', field: 'type', sortable: true, width: 150 },
  { 
    headerName: 'Required', 
    field: 'required', 
    sortable: true,
    width: 120,
    cellRenderer: (params) => {
      return params.value 
        ? '<span style="padding: 2px 6px; border-radius: 4px; background: #e42222; color: white; font-size: 12px; font-weight: bold;">Yes</span>' 
        : '<span style="padding: 2px 6px; border-radius: 4px; background: #2c82e0; color: white; font-size: 12px; font-weight: bold;">No</span>';
    }
  },
  { 
    headerName: 'Actions', 
    field: 'id',
    width: 100,
    cellRenderer: (params) => {
      return '<span style="cursor: pointer; color: #2c82e0; text-decoration: underline;">Edit</span>';
    },
    onCellClicked: (params) => {
      openFieldModal(true, params.data);
    }
  }
])

const onRowDoubleClicked = (params) => {
  openFieldModal(true, params.data)
}

const onGridReady = (params) => {
  gridApi.value = params.api
  setTimeout(() => {
    params.api.sizeColumnsToFit()
  }, 100)
}

const loadTree = async () => {
  try {
    const domains = await $fetch('/api/domains', {
      headers: { Authorization: `Bearer ${token.value}` }
    })
    
    // For each domain, fetch its nodes tree
    const builtTree = []
    for (const d of domains) {
      const nodes = await $fetch(`/api/domains/${d.id}/nodes/tree`, {
        headers: { Authorization: `Bearer ${token.value}` }
      })
      
      const parseName = (nameObj) => {
        if (!nameObj) return null;
        if (typeof nameObj === 'string') {
          try {
            return JSON.parse(nameObj);
          } catch (e) {
            return null;
          }
        }
        return nameObj;
      };

      const formatNode = (n) => {
        const pName = parseName(n.name);
        return {
          id: n.id,
          label: pName?.[currentLocale.value] || pName?.ko || pName?.en || 'Unknown',
          domainId: d.id,
          isDomain: false,
          children: n.children ? n.children.map(formatNode) : [],
          originalNameMap: pName
        };
      };
      
      const dName = parseName(d.name);
      builtTree.push({
        id: d.id,
        label: (dName?.[currentLocale.value] || dName?.ko || dName?.en || 'Unknown') + ' (Domain)',
        domainId: d.id,
        isDomain: true,
        expanded: true,
        children: nodes.map(formatNode),
        originalNameMap: dName
      })
    }
    console.log('Loaded tree:', builtTree)
    treeNodes.value = builtTree
  } catch (error) {
    console.error('Failed to load tree:', error.message || error)
  }
}

onMounted(async () => {
  loadTree()
  try {
    const users = await $fetch('http://localhost:8080/api/auth/users', { headers: getAuthHeaders() })
    userOptions.value = users.map(u => ({ text: u.username, value: u.uuid }))
  } catch (e) {
    console.error('Failed to load users:', e)
  }
})

const saveWorkflowConfigs = async () => {
  if (!selectedNode.value) return
  try {
    const payloads = Object.keys(workflowConfigs.value).map(action => ({
      actionType: action,
      consensusUserId: workflowConfigs.value[action].consensusUserId || null,
      approverUserId: workflowConfigs.value[action].approverUserId || null
    }))
    
    const url = selectedNode.value.isDomain 
      ? `http://localhost:8080/api/workflow-configs/domain/${selectedNode.value.id}`
      : `http://localhost:8080/api/workflow-configs/node/${selectedNode.value.id}`
      
    await $fetch(url, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: payloads
    })
    alert('Workflow configurations saved successfully.')
  } catch (e) {
    console.error('Failed to save workflows', e)
    alert('Failed to save workflows.')
  }
}

watch(currentLocale, () => {
  const updateLabel = (nodes) => {
    nodes.forEach(n => {
      if (n.originalNameMap) {
        n.label = n.originalNameMap[currentLocale.value] || n.originalNameMap.ko || n.originalNameMap.en || 'Unknown';
        if (n.isDomain) n.label += ' (Domain)';
      }
      if (n.children && n.children.length > 0) {
        updateLabel(n.children);
      }
    })
  }
  updateLabel(treeNodes.value)
  
  if (gridApi.value) {
    gridApi.value.refreshCells({ force: true })
  }
})

const onNodeSelected = async (nodes) => {
  const node = Array.isArray(nodes) ? nodes[0] : nodes
  selectedNode.value = node || null
  
  if (!node) {
    fields.value = []
    domainSectors.value = []
    domainGroups.value = []
    return
  }
  
  const dId = node.domainId
  try {
    const wfUrl = node.isDomain ? `http://localhost:8080/api/workflow-configs/domain/${node.id}` : `http://localhost:8080/api/workflow-configs/node/${node.id}`
    const [sData, gData, wfData] = await Promise.all([
      $fetch(`http://localhost:8080/api/domains/${dId}/sectors`, { headers: getAuthHeaders() }),
      $fetch(`http://localhost:8080/api/domains/${dId}/groups`, { headers: getAuthHeaders() }),
      $fetch(wfUrl, { headers: getAuthHeaders() }).catch(() => [])
    ])
    domainSectors.value = sData
    domainGroups.value = gData
    
    workflowConfigs.value = {
      CREATE: { consensusUserId: null, approverUserId: null },
      UPDATE: { consensusUserId: null, approverUserId: null },
      DELETE: { consensusUserId: null, approverUserId: null }
    }
    for (const wf of wfData) {
      if (workflowConfigs.value[wf.actionType]) {
         workflowConfigs.value[wf.actionType].consensusUserId = wf.consensusUserId
         workflowConfigs.value[wf.actionType].approverUserId = wf.approverUserId
      }
    }
  } catch (e) {
    console.error('Failed to load sectors/groups/workflows', e)
  }

  try {
    let data = []
    if (!node.isDomain) {
      data = await $fetch(`/api/nodes/${node.id}/fields/effective`, {
        headers: { Authorization: `Bearer ${token.value}` }
      })
    } else {
      data = await $fetch(`/api/domains/${node.id}/fields`, {
        headers: { Authorization: `Bearer ${token.value}` }
      })
    }
    fields.value = data
    setTimeout(() => {
      if (gridApi.value) {
        gridApi.value.sizeColumnsToFit()
      }
    }, 100)
  } catch (e) {
    console.error(e.message || e)
  }
}

// handleNodeEdit remains unchanged
const handleNodeEdit = (node) => {
  selectedNode.value = node
  if (node.isDomain) {
    openDomainModal(true)
  } else {
    openNodeModal(true)
  }
}

const openDomainModal = async (edit) => {
  isEditMode.value = edit
  mappingError.value = { id: false, name: false }
  if (edit && selectedNode.value?.isDomain) {
    editingId.value = selectedNode.value.id
    try {
      // Fetch domain details
      const domains = await $fetch('/api/domains', {
        headers: { Authorization: `Bearer ${token.value}` }
      })
      const d = domains.find(x => x.id === editingId.value)
      if (d) {
        newDomain.value = {
          name: { ko: d.name?.ko || '', en: d.name?.en || '' },
          description: { ko: d.description?.ko || '', en: d.description?.en || '' },
          identifierFieldId: d.identifierFieldId || null,
          displayNameFieldId: d.displayNameFieldId || null,
          descriptionFieldId: d.descriptionFieldId || null
        }
      }
      
      // Fetch domain fields
      const fieldsData = await $fetch(`/api/domains/${editingId.value}/fields`, {
        headers: { Authorization: `Bearer ${token.value}` }
      })
      domainFieldOptions.value = fieldsData.map(f => ({
        value: f.id,
        text: `${f.name.ko || f.name.en} (${f.key})`
      }))
    } catch(e){}
  } else {
    editingId.value = null
    newDomain.value = { name: { ko: '', en: '' }, description: { ko: '', en: '' }, identifierFieldId: null, displayNameFieldId: null, descriptionFieldId: null }
    domainFieldOptions.value = []
  }
  showDomainModal.value = true
}

const openNodeModal = (edit) => {
  isEditMode.value = edit
  if (edit && selectedNode.value && !selectedNode.value.isDomain) {
    editingId.value = selectedNode.value.id
    // selectedNode has limited info, ideally we fetch node details, but name is enough
    const originalNameMap = selectedNode.value.originalNameMap || { ko: '', en: '' }
    newNode.value = {
      name: { ko: originalNameMap.ko || '', en: originalNameMap.en || '' },
      order: selectedNode.value.order || 1
    }
  } else {
    editingId.value = null
    newNode.value = { name: { ko: '', en: '' }, order: 1 }
  }
  showNodeModal.value = true
}

const openFieldModal = (edit, rowData) => {
  isEditMode.value = edit
  if (edit && rowData) {
    editingId.value = rowData.id
    newField.value = {
      name: { ko: rowData.name?.ko || '', en: rowData.name?.en || '' },
      fieldGroupId: rowData.fieldGroup?.id || null,
      key: rowData.key,
      type: rowData.type,
      options: rowData.options || '',
      targetDomainId: null,
      required: rowData.required,
      isMultiValue: rowData.isMultiValue,
      isSearchable: rowData.isSearchable,
      isEncrypted: rowData.isEncrypted,
      isReadOnly: rowData.isReadOnly,
      isImmutable: rowData.isImmutable,
      isHidden: rowData.isHidden,
      order: rowData.order || 0
    }
    
    if (rowData.type === 'DOMAIN_REFERENCE') {
      try {
        const parsedOpts = JSON.parse(rowData.options || '{}')
        newField.value.targetDomainId = parsedOpts.targetDomainId || null
      } catch(e) {}
    } else if (rowData.type === 'CALCULATED') {
      try {
        const parsedOpts = JSON.parse(rowData.options || '{}')
        newField.value.formula = parsedOpts.formula || ''
      } catch(e) {}
    } else {
      try {
        const parsedOpts = JSON.parse(rowData.options || '[]')
        if (Array.isArray(parsedOpts)) {
          newFieldOptionsList.value = parsedOpts.map(opt => {
            if (typeof opt === 'string') return { key: opt, label: { ko: opt, en: opt }, order: 0 }
            return { 
              key: opt.key || '', 
              label: { ko: opt.label?.ko || opt.label?.en || opt.key, en: opt.label?.en || opt.label?.ko || opt.key },
              order: opt.order || 0
            }
          })
        }
      } catch (e) {
        newFieldOptionsList.value = [{ key: '', label: { ko: '', en: '' }, order: 0 }]
      }
    }
  } else {
    editingId.value = null
    newField.value = { 
      name: { ko: '', en: '' }, 
      fieldGroupId: null,
      key: '', 
      type: 'TEXT', 
      options: '', 
      targetDomainId: null,
      required: false, 
      isMultiValue: false, 
      isSearchable: true, 
      isEncrypted: false,
      isReadOnly: false,
      isImmutable: false,
      isHidden: false,
      order: fields.value.length + 1
    }
    newFieldOptionsList.value = [{ key: '', label: { ko: '', en: '' }, order: 0 }]
  }
  showFieldModal.value = true
}

const saveDomain = async () => {
  if (isEditMode.value) {
    mappingError.value.id = !newDomain.value.identifierFieldId;
    mappingError.value.name = !newDomain.value.displayNameFieldId;
    if (mappingError.value.id || mappingError.value.name) {
      return;
    }
  }

  try {
    const url = isEditMode.value ? `/api/domains/${editingId.value}` : '/api/domains'
    const method = isEditMode.value ? 'PUT' : 'POST'
    await $fetch(url, {
      method,
      headers: { Authorization: `Bearer ${token.value}` },
      body: newDomain.value
    })
    newDomain.value = { name: { ko: '', en: '' }, description: { ko: '', en: '' }, identifierFieldId: null, displayNameFieldId: null, descriptionFieldId: null }
    showDomainModal.value = false
    await loadTree()
  } catch (error) {
    alert('Error saving domain: ' + (error.data?.message || error.message || 'Unknown error'))
  }
}

const saveNode = async () => {
  if (!selectedNode.value) return
  try {
    const url = isEditMode.value 
      ? `/api/domains/${selectedNode.value.domainId}/nodes/${editingId.value}`
      : `/api/domains/${selectedNode.value.domainId}/nodes`
    const method = isEditMode.value ? 'PUT' : 'POST'
    await $fetch(url, {
      method,
      headers: { Authorization: `Bearer ${token.value}` },
      body: {
        name: newNode.value.name,
        order: newNode.value.order,
        parentId: isEditMode.value ? undefined : (selectedNode.value.isDomain ? null : selectedNode.value.id)
      }
    })
    newNode.value = { name: { ko: '', en: '' }, order: 1 }
    showNodeModal.value = false
    await loadTree()
  } catch (error) {
    alert('Error saving node')
  }
}

const saveField = async () => {
  if (!selectedNode.value) return
  
  if (!newField.value.key || String(newField.value.key).trim() === '') {
    alert(currentLocale.value === 'ko' ? 'Field Key를 입력해주세요.' : 'Please enter a Field Key.')
    return
  }

  const duplicate = fields.value.find(f => f.key === newField.value.key && (!isEditMode.value || f.id !== editingId.value))
  if (duplicate) {
    alert(currentLocale.value === 'ko' ? `이미 존재하는 Field Key 입니다: ${newField.value.key}` : `Field Key already exists: ${newField.value.key}`)
    return
  }
  
  if (['SELECT', 'MULTI_SELECT'].includes(newField.value.type)) {
    const hasEmptyKey = newFieldOptionsList.value.some(opt => !opt.key || String(opt.key).trim() === '')
    if (hasEmptyKey) {
      alert(currentLocale.value === 'ko' ? '모든 옵션의 Key를 입력해주세요.' : 'Please enter a Key for all options.')
      return
    }
    newField.value.options = JSON.stringify(newFieldOptionsList.value)
  } else if (newField.value.type === 'DOMAIN_REFERENCE') {
    if (!newField.value.targetDomainId) {
      alert(currentLocale.value === 'ko' ? '대상 도메인을 선택해주세요.' : 'Please select a target domain.')
      return
    }
    newField.value.options = JSON.stringify({ targetDomainId: newField.value.targetDomainId })
  } else if (newField.value.type === 'CALCULATED') {
    if (!newField.value.formula || String(newField.value.formula).trim() === '') {
      alert(currentLocale.value === 'ko' ? '수식을 입력해주세요.' : 'Please enter a formula.')
      return
    }
    try {
      const testFormula = newField.value.formula.replace(/\${[^}]+}/g, '1')
      const ROUND = (val, dec=0) => Number(Math.round(val+'e'+dec)+'e-'+dec);
      const fn = new Function('ROUND', 'ABS', 'CEIL', 'FLOOR', `return ${testFormula};`)
      fn(ROUND, Math.abs, Math.ceil, Math.floor)
    } catch (e) {
      alert(currentLocale.value === 'ko' ? `수식에 문법 오류가 있습니다: ${e.message}` : `Syntax error in formula: ${e.message}`)
      return
    }
    newField.value.options = JSON.stringify({ formula: newField.value.formula.trim() })
  }
  
  try {
    let url = ''
    if (selectedNode.value.isDomain) {
      url = isEditMode.value
        ? `/api/domains/${selectedNode.value.id}/fields/${editingId.value}`
        : `/api/domains/${selectedNode.value.id}/fields`
    } else {
      url = isEditMode.value
        ? `/api/nodes/${selectedNode.value.id}/fields/${editingId.value}`
        : `/api/nodes/${selectedNode.value.id}/fields`
    }
    const method = isEditMode.value ? 'PUT' : 'POST'
    await $fetch(url, {
      method,
      headers: { Authorization: `Bearer ${token.value}` },
      body: newField.value
    })
    newField.value = { 
      name: { ko: '', en: '' }, 
      fieldGroupId: null,
      key: '', type: 'TEXT', options: '', required: false, isMultiValue: false, isSearchable: true, isEncrypted: false, isReadOnly: false, isImmutable: false, isHidden: false, order: 0 
    }
    showFieldModal.value = false
    await onNodeSelected(selectedNode.value)
  } catch (error) {
    alert('Error saving field')
  }
}

const openSectorGroupModal = async () => {
  if (!selectedNode.value && treeNodes.value && treeNodes.value.length > 0) {
    await onNodeSelected(treeNodes.value[0])
  }
  showSectorGroupModal.value = true
}

const addSectorRow = () => {
  domainSectors.value = [...domainSectors.value, { name: { ko: '', en: '' }, sortOrder: 0 }]
}

const addGroupRow = () => {
  domainGroups.value = [...domainGroups.value, { sector: { id: null }, name: { ko: '', en: '' }, sortOrder: 0, isDefaultOpen: true }]
}

const onSectorRowSaved = async (event) => {
  const data = event.data
  const dId = selectedNode.value.domainId
  if (!data.name?.ko && !data.name?.en) {
    alert('Sector name is required.')
    domainSectors.value = await $fetch(`/api/domains/${dId}/sectors`, { headers: getAuthHeaders() })
    return
  }
  try {
    if (data.id) {
      await $fetch(`/api/domains/${dId}/sectors/${data.id}`, { method: 'PUT', headers: getAuthHeaders(), body: data })
    } else {
      await $fetch(`/api/domains/${dId}/sectors`, { method: 'POST', headers: getAuthHeaders(), body: data })
    }
    domainSectors.value = await $fetch(`/api/domains/${dId}/sectors`, { headers: getAuthHeaders() })
  } catch(e) { alert('Error saving sector') }
}

const onGroupRowSaved = async (event) => {
  const data = event.data
  const dId = selectedNode.value.domainId
  if (!data.name?.ko && !data.name?.en) {
    alert('Group name is required.')
    domainGroups.value = await $fetch(`/api/domains/${dId}/groups`, { headers: getAuthHeaders() })
    return
  }
  if (!data.sector?.id) {
    alert('Sector is required.')
    domainGroups.value = await $fetch(`/api/domains/${dId}/groups`, { headers: getAuthHeaders() })
    return
  }
  
  const payload = {
    sectorId: data.sector.id,
    name: data.name,
    sortOrder: data.sortOrder,
    isDefaultOpen: data.isDefaultOpen
  }
  
  try {
    if (data.id) {
      await $fetch(`/api/domains/${dId}/groups/${data.id}`, { method: 'PUT', headers: getAuthHeaders(), body: payload })
    } else {
      await $fetch(`/api/domains/${dId}/groups`, { method: 'POST', headers: getAuthHeaders(), body: payload })
    }
    domainGroups.value = await $fetch(`/api/domains/${dId}/groups`, { headers: getAuthHeaders() })
  } catch(e) { alert('Error saving group') }
}

const deleteSelectedSector = async () => {
  if (!sectorGridApi.value) return
  const selected = sectorGridApi.value.getSelectedNodes()
  if (selected.length === 0) return
  const id = selected[0].data.id
  const dId = selectedNode.value.domainId
  if (!id) {
    domainSectors.value = await $fetch(`/api/domains/${dId}/sectors`, { headers: getAuthHeaders() })
    return
  }
  await deleteSector(id)
}

const deleteSelectedGroup = async () => {
  if (!groupGridApi.value) return
  const selected = groupGridApi.value.getSelectedNodes()
  if (selected.length === 0) return
  const id = selected[0].data.id
  const dId = selectedNode.value.domainId
  if (!id) {
    domainGroups.value = await $fetch(`/api/domains/${dId}/groups`, { headers: getAuthHeaders() })
    return
  }
  await deleteGroup(id)
}

const deleteGroup = async (id) => {
  const dId = selectedNode.value.domainId
  try {
    await $fetch(`/api/domains/${dId}/groups/${id}`, {
      method: 'DELETE',
      headers: { Authorization: `Bearer ${token.value}` }
    })
    domainGroups.value = await $fetch(`/api/domains/${dId}/groups`, { headers: { Authorization: `Bearer ${token.value}` } })
    cancelEditGroup()
  } catch (e) { alert('Error deleting group.') }
}
</script>

<style scoped>
.schema-layout {
  display: flex;
  gap: 0.5rem;
  width: 100%;
  height: 100%;
  min-height: 0;
}
.schema-tree-column {
  width: 30%;
  max-width: 30%;
  overflow: hidden;
}
.schema-tree-wrapper {
  max-height: 400px;
  overflow-y: auto;
  overflow-x: hidden;
  margin-bottom: 1rem;
}
.schema-detail-column {
  width: calc(70% - 1rem);
  max-width: calc(70% - 1rem);
  display: flex;
  flex-direction: column;
}
.schema-grid-wrapper {
  flex: 1;
  width: 100%;
  min-height: 0;
}

@media (max-width: 768px) {
  .schema-layout {
    flex-direction: column;
  }
  .schema-tree-column {
    width: 100%;
    max-width: 100%;
  }
  .schema-tree-wrapper {
    max-height: 250px;
  }
  .schema-detail-column {
    width: 100%;
    max-width: 100%;
    padding: 0.25rem 0;
  }
  .schema-grid-wrapper {
    height: 400px;
  }
}
</style>

<style scoped>
.mb-4 { margin-bottom: 1rem; }
.w-full { width: 100%; }

/* Tree Container */
:deep(.va-tree) {
  overflow-x: hidden;
}
</style>
