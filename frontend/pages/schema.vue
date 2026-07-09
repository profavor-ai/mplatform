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
                분류체계 트리가 없습니다. 하단의 Domain 버튼을 눌러 새 도메인을 생성해주세요.
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
              <va-button style="flex: 1" icon="add" @click="openDomainModal()">Domain</va-button>
              <va-button style="flex: 1" icon="add" @click="openNodeModal()" :disabled="!selectedNode">Node</va-button>
            </div>
            <div style="margin-top: 1rem;">
              <va-button style="width: 100%" color="info" icon="settings" @click="openSectorGroupModal" :disabled="!treeNodes || treeNodes.length === 0" :outline="isDark">Manage Sectors & Groups</va-button>
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
              <div :class="[currentPresetName === 'dark' ? 'ag-theme-alpine-dark' : 'ag-theme-alpine', ' schema-grid-wrapper']">
                <ag-grid-vue
                  style="width: 100%; height: 100%;"
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
                <va-button icon="add" @click="openFieldModal(null)">Add Field</va-button>
              </div>
            </div>

            <!-- Workflows Tab -->
            <div v-show="activeTab === 1" style="flex: 1; display: flex; flex-direction: column; position: relative; min-height: 0;">
              <div style="flex: 1; overflow-y: auto; padding: 1rem; min-height: 0;">
                <div v-for="action in ['CREATE', 'UPDATE', 'DELETE']" :key="action" style="margin-bottom: 1rem; padding: 0.75rem; border: 1px solid #e0e0e0; border-radius: 6px; background: #fff;">
                  <div style="display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #eee; margin-bottom: 0.75rem; padding-bottom: 0.25rem;">
                    <h3 style="font-weight: 600; font-size: 1.1rem; color: #333; margin: 0;">{{ action }} Action</h3>
                  </div>
                  
                  <div style="display: flex; gap: 1rem; align-items: flex-start;">
                    <div style="flex: 2;">
                      <h4 style="font-size: 0.9rem; margin-bottom: 0.25rem; color: #555;">결재선</h4>
                      <div v-if="workflowConfigs[action].steps.length === 0" style="color: #999; font-size: 0.85rem; padding: 0.5rem 0;">결재 단계를 추가해주세요.</div>
                      <div v-for="(step, sIdx) in workflowConfigs[action].steps" :key="step.id" style="border: 1px solid #ececec; padding: 0.5rem; border-radius: 4px; margin-bottom: 0.5rem; background: #fdfdfd; box-shadow: 0 1px 2px rgba(0,0,0,0.02);">
                        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.5rem; border-bottom: 1px dashed #eee; padding-bottom: 0.25rem;">
                          <span style="font-size: 0.85rem; font-weight: bold; color: #666;">Step {{ sIdx + 1 }}</span>
                          <div style="display: flex; gap: 0.25rem;">
                            <va-button size="small" preset="secondary" icon="arrow_upward" @click="moveStepUp(action, sIdx)" :disabled="sIdx === 0" style="padding: 0; min-width: 24px; height: 24px;"></va-button>
                            <va-button size="small" preset="secondary" icon="arrow_downward" @click="moveStepDown(action, sIdx)" :disabled="sIdx === workflowConfigs[action].steps.length - 1" style="padding: 0; min-width: 24px; height: 24px;"></va-button>
                            <va-button size="small" color="danger" preset="secondary" icon="delete" @click="removeStep(action, sIdx)" style="padding: 0; min-width: 24px; height: 24px;"></va-button>
                          </div>
                        </div>
                        <div v-for="(u, uIdx) in step.users" :key="uIdx" style="display: flex; gap: 0.5rem; align-items: center; margin-bottom: 0.25rem;">
                          <va-select style="flex: 1;" v-model="u.stepType" :options="['CONSENSUS', 'APPROVAL']" placeholder="유형" />
                          <va-select style="flex: 2;" v-model="u.assigneeId" :options="userOptions" value-by="value" text-by="text" placeholder="사용자 선택" />
                          <va-button size="small" color="danger" preset="secondary" icon="close" @click="removeUserFromStep(action, sIdx, uIdx)" :disabled="step.users.length === 1" style="padding: 0; min-width: 24px; height: 24px;"></va-button>
                        </div>
                        <div style="text-align: right; margin-top: 0.25rem;">
                          <va-button size="small" preset="secondary" icon="person_add" @click="addUserToStep(action, sIdx)" style="font-size: 0.75rem; padding: 0.25rem 0.5rem; height: 24px;">병렬 추가</va-button>
                        </div>
                      </div>
                      <va-button size="small" preset="secondary" icon="add" @click="addStep(action)">단계 추가</va-button>
                    </div>
                    
                    <div style="flex: 1; border-left: 1px solid #eee; padding-left: 1rem;">
                      <h4 style="font-size: 0.9rem; margin-bottom: 0.25rem; color: #555;">통보자 지정</h4>
                      <va-select
                        v-model="workflowConfigs[action].observerIds"
                        :options="userOptions"
                        value-by="value"
                        text-by="text"
                        multiple
                        placeholder="통보자 다중 선택"
                      />
                    </div>
                  </div>
                </div>
              </div>
              <div style="padding: 1rem; border-top: 1px solid #ddd; background: #fafafa; display: flex; justify-content: flex-end;">
                <va-button color="success" icon="save" @click="saveWorkflowConfigs" :outline="isDark">설정 저장</va-button>
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
          :options="domainFieldOptions.filter(o => o.type === 'MULTILINGUAL')"
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
        <label style="font-weight: bold; margin-bottom: 0.5rem; display: block;">{{ t('options_settings') }}</label>
        
        <div style="margin-bottom: 0.5rem; display: flex; gap: 0.5rem; justify-content: flex-end;">
          <va-button size="small" icon="add" @click="addGridOption">{{ t('add_option') }}</va-button>
          <va-button size="small" icon="remove" color="danger" @click="removeSelectedGridOption" :outline="isDark">{{ t('remove_selected') }}</va-button>
        </div>
        
        <ag-grid-vue
          :class="[currentPresetName === 'dark' ? 'ag-theme-alpine-dark' : 'ag-theme-alpine']"
          style="width: 100%; height: 250px;"
          :columnDefs="optionsColumnDefs"
          :rowData="newFieldOptionsList"
          :defaultColDef="optionsDefaultColDef"
          rowSelection="single"
          @grid-ready="onOptionsGridReady"
        />
      </div>
      
      <div v-else-if="newField.type === 'CALCULATED'" class="mb-4 w-full" style="border: 1px solid #ccc; padding: 1rem; border-radius: 8px;">
        <label style="font-weight: bold; margin-bottom: 0.5rem; display: block;">{{ t('formula_settings') }}</label>
        <va-textarea
          v-model="newField.formula"
          :placeholder="t('e_g_abs_key_a_key_b_2_100')"
          class="w-full mb-2"
          :min-rows="3"
          style="font-family: monospace;"
        />
        <va-alert color="info" dense class="w-full" style="font-size: 0.85rem;">
          <strong>{{ t('formula_guide') }}</strong><br/>
          - <strong>필드 참조</strong>: <code>${필드_KEY}</code> 형식으로 입력하세요. (예: <code>${PRICE}</code>)<br/>
          - <strong>기본 연산</strong>: <code>+</code> (더하기), <code>-</code> (빼기), <code>*</code> (곱하기), <code>/</code> (나누기)<br/>
          - <strong>수학 함수</strong>:<br/>
            &nbsp;&nbsp;• <code>ROUND(값, 자리수)</code> : 반올림(예: <code>ROUND(${PRICE}, 2)</code>)<br/>
            &nbsp;&nbsp;• <code>CEIL(값)</code> : 올림<br/>
            &nbsp;&nbsp;• <code>FLOOR(값)</code> : 내림<br/>
            &nbsp;&nbsp;• <code>ABS(값)</code> : 절대값<br/>
          <span style="color: #d9534f; font-weight: bold;">주의:</span> 참조하는 필드는 반드시 숫자(NUMBER, DECIMAL, FLOAT, INTEGER)이거나 다른 계산 필드(CALCULATED)여야 합니다.
        </va-alert>
      </div>

      <div v-if="['NUMBER', 'DECIMAL', 'FLOAT', 'INTEGER', 'CALCULATED'].includes(newField.type)" class="mb-4 w-full">
        <va-select
          v-model="newField.unit"
          :options="unitOptions"
          label="Unit (단위)"
          class="w-full"
          clearable
          allow-create="unique"
          :no-options-text="'직접 입력 가능'"
        />
      </div>

      <div style="display: flex; gap: 1rem; margin-top: 1rem; margin-bottom: 0.5rem; flex-wrap: wrap;">
        <va-input v-model="newField.gridWidth" type="number" label="Grid Width (px)" class="w-full" style="max-width: 150px;" placeholder="Auto" clearable />
      </div>
      <div style="display: flex; gap: 1rem; margin-top: 1rem; flex-wrap: wrap;">
        <va-checkbox v-model="newField.required" :label="t('required')" />
        <va-checkbox v-model="newField.isMultiValue" :label="t('multi_value')" />
        <va-checkbox v-model="newField.isSearchable" :label="t('searchable_1')" />
        <va-checkbox v-model="newField.isEncrypted" :label="t('encrypted')" />
        <va-checkbox v-model="newField.isReadOnly" :label="t('read_only')" />
        <va-checkbox v-model="newField.isImmutable" :label="t('immutable')" />
        <va-checkbox v-model="newField.isHidden" :label="t('hidden')" />
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
              <va-button size="small" color="danger" @click="deleteSelectedSector" :outline="isDark">Delete Selected</va-button>
            </div>
          </div>
          <div :class="[currentPresetName === 'dark' ? 'ag-theme-alpine-dark' : 'ag-theme-alpine']" style="flex: 1; width: 100%;">
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
              <va-button size="small" color="danger" @click="deleteSelectedGroup" :outline="isDark">Delete Selected</va-button>
            </div>
          </div>
          <div :class="[currentPresetName === 'dark' ? 'ag-theme-alpine-dark' : 'ag-theme-alpine']" style="flex: 1; width: 100%;">
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
const colors = useColors()
const isDark = computed(() => colors.currentPresetName.value === 'dark')
import { useColors } from 'vuestic-ui'
import { useI18n } from 'vue-i18n'
const { t } = useI18n()
const { currentPresetName } = useColors()
import { ref, computed, onMounted, watch } from 'vue'
import { useCookie, useState } from '#app'

import { AgGridVue } from 'ag-grid-vue3'
import 'ag-grid-community/styles/ag-grid.css'
import 'ag-grid-community/styles/ag-theme-alpine.css'

const currentLocale = useCookie('locale', { default: () => 'ko' })
const token = useCookie('auth_token', { default: () => '' })
const getAuthHeaders = () => token.value ? { Authorization: `Bearer ${token.value}` } : {}

const activeTab = ref(0)
const workflowConfigs = ref({
  CREATE: { steps: [], observerIds: [] },
  UPDATE: { steps: [], observerIds: [] },
  DELETE: { steps: [], observerIds: [] }
})
const userOptions = ref([])
const unitOptions = ref([])
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
  unit: '',
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

const fieldTypes = ['TEXT', 'NUMBER', 'DATE', 'BOOLEAN', 'SELECT', 'DECIMAL', 'FLOAT', 'INTEGER', 'DOMAIN_REFERENCE', 'MULTI_SELECT', 'TIME', 'HTML_TEXT', 'CHECKBOX', 'CALCULATED', 'MULTILINGUAL']

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
  { headerName: 'Grid Width', field: 'gridWidth', sortable: true, width: 120 },
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
      if (params.data.domainId && !selectedNode.value.isDomain) {
        return '<span style="color: #666; font-style: italic;">Inherited</span>';
      }
      return '<span style="cursor: pointer; color: #2c82e0; text-decoration: underline;">Edit</span>';
    },
    onCellClicked: (params) => {
      if (params.data.domainId && !selectedNode.value.isDomain) return;
      openFieldModal(params.data);
    }
  }
])

const onRowDoubleClicked = (params) => {
  if (params.data.domainId && !selectedNode.value.isDomain) return;
  openFieldModal(params.data)
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
          originalNameMap: pName,
          originalData: n
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
        originalNameMap: dName,
        originalData: d
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
    const users = await $fetch('/api/auth/users', { headers: getAuthHeaders() }).catch(() => [])
    userOptions.value = users.map(u => ({ text: u.username, value: u.uuid }))
    
    unitOptions.value = [
      'kg', 'g', 'mg', 't', 'lb', 'oz', 
      'm', 'cm', 'mm', 'km', 'in', 'ft', 'yd', 'mi', 
      'm²', 'cm²', 'km²', 'ha', 
      'L', 'mL', 'm³', 'cm³', 
      's', 'min', 'h', 'd', 
      '℃', '℉', 'K', 
      'Pa', 'kPa', 'MPa', 'bar', 'atm', 'psi', 
      'N', 'kN', 'J', 'kJ', 'cal', 'kcal', 
      'W', 'kW', 'MW', 
      'V', 'A', 'Ω', 'Hz', 'kHz', 'MHz', 'GHz', 
      'B', 'KB', 'MB', 'GB', 'TB', 
      '%', '‰', 'ppm', 
      '원', '$', '€', '¥', '£', 
      'EA', 'SET', 'BOX', 'ROLL', 'SHEET', 'PCS'
    ]
  } catch (e) {
    console.error('Failed to load metadata:', e)
  }
})


const addStep = (action) => {
  workflowConfigs.value[action].steps.push({
    id: Date.now(),
    users: [{ stepType: 'APPROVAL', assigneeId: null }]
  })
}

const removeStep = (action, sIdx) => {
  workflowConfigs.value[action].steps.splice(sIdx, 1)
}

const moveStepUp = (action, sIdx) => {
  if (sIdx > 0) {
    const arr = workflowConfigs.value[action].steps
    const temp = arr[sIdx]
    arr[sIdx] = arr[sIdx - 1]
    arr[sIdx - 1] = temp
  }
}

const moveStepDown = (action, sIdx) => {
  const arr = workflowConfigs.value[action].steps
  if (sIdx < arr.length - 1) {
    const temp = arr[sIdx]
    arr[sIdx] = arr[sIdx + 1]
    arr[sIdx + 1] = temp
  }
}

const addUserToStep = (action, sIdx) => {
  workflowConfigs.value[action].steps[sIdx].users.push({ stepType: 'CONSENSUS', assigneeId: null })
}

const removeUserFromStep = (action, sIdx, uIdx) => {
  workflowConfigs.value[action].steps[sIdx].users.splice(uIdx, 1)
}

const saveWorkflowConfigs = async () => {
  if (!selectedNode.value) return
  try {
    const payloads = Object.keys(workflowConfigs.value).map(action => {
      const conf = workflowConfigs.value[action]
      const flatSteps = []
      conf.steps.forEach((step, sIdx) => {
        step.users.forEach(u => {
          if (u.assigneeId && u.stepType) {
            flatSteps.push({
              stepType: u.stepType,
              assigneeId: u.assigneeId,
              stepOrder: sIdx + 1
            })
          }
        })
      })
      
      const stepsConfig = JSON.stringify({
          steps: flatSteps,
          observerIds: conf.observerIds || []
      })
      
      return {
        actionType: action,
        stepsConfig: stepsConfig
      }
    })
    
    const url = selectedNode.value.isDomain 
      ? `/api/workflow-configs/domain/${selectedNode.value.id}`
      : `/api/workflow-configs/node/${selectedNode.value.id}`
      
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
    const wfUrl = node.isDomain ? `/api/workflow-configs/domain/${node.id}` : `/api/workflow-configs/node/${node.id}`
    const [sData, gData, wfData] = await Promise.all([
      $fetch(`/api/domains/${dId}/sectors`, { headers: getAuthHeaders() }),
      $fetch(`/api/domains/${dId}/groups`, { headers: getAuthHeaders() }),
      $fetch(wfUrl, { headers: getAuthHeaders() }).catch(() => [])
    ])
    domainSectors.value = sData
    domainGroups.value = gData
    
    workflowConfigs.value = {
      CREATE: { steps: [], observerIds: [] },
      UPDATE: { steps: [], observerIds: [] },
      DELETE: { steps: [], observerIds: [] }
    }
    for (const wf of wfData) {
      if (workflowConfigs.value[wf.actionType]) {
         try {
             const parsed = wf.stepsConfig ? JSON.parse(wf.stepsConfig) : { steps: [], observerIds: [] }
             
             // Convert flat steps back into grouped UI steps
             const flatSteps = parsed.steps || []
             const grouped = []
             let currentOrder = -1
             let currentStep = null
             
             for (const fs of flatSteps) {
                 if (fs.stepOrder !== currentOrder) {
                     currentOrder = fs.stepOrder
                     currentStep = { id: Date.now() + Math.random(), users: [] }
                     grouped.push(currentStep)
                 }
                 currentStep.users.push({
                     stepType: fs.stepType,
                     assigneeId: fs.assigneeId
                 })
             }
             
             workflowConfigs.value[wf.actionType].steps = grouped
             workflowConfigs.value[wf.actionType].observerIds = parsed.observerIds || []
         } catch(e) {
             console.error('Failed to parse stepsConfig', e)
         }
      }
    } // end for wf
  } catch(e) {
    console.error('Failed to load node data', e)
  }
  
  try {
    if (node.isDomain) {
      fields.value = await $fetch(`/api/domains/${node.id}/fields`, { headers: getAuthHeaders() })
    } else {
      fields.value = await $fetch(`/api/nodes/${node.id}/fields/effective`, { headers: getAuthHeaders() })
    }
  } catch(e) {
    console.error('Failed to load fields', e)
    fields.value = []
  }
}

const handleNodeEdit = async (node) => {
  const targetNode = node || selectedNode.value
  if (!targetNode) return
  isEditMode.value = true
  if (targetNode.isDomain) {
    try {
      const dFields = await $fetch(`/api/domains/${targetNode.id}/fields`, { headers: getAuthHeaders() })
      domainFieldOptions.value = dFields.map(f => {
        const pName = typeof f.name === 'string' ? JSON.parse(f.name || '{}') : (f.name || {})
        return {
          value: f.id,
          text: pName[currentLocale.value] || pName.ko || pName.en || f.key || 'Unknown',
          type: f.type
        }
      })
    } catch (e) {
      domainFieldOptions.value = []
    }
    const rawDomain = targetNode.originalData || {}
    const pDesc = typeof rawDomain.description === 'string' ? JSON.parse(rawDomain.description || '{}') : (rawDomain.description || {})
    newDomain.value = { 
      ...targetNode, 
      name: { ...(targetNode.originalNameMap || {ko:'', en:''}) },
      description: { ko: pDesc.ko || '', en: pDesc.en || '' },
      identifierFieldId: rawDomain.identifierFieldId,
      displayNameFieldId: rawDomain.displayNameFieldId,
      descriptionFieldId: rawDomain.descriptionFieldId
    }
    showDomainModal.value = true
  } else {
    const rawNode = targetNode.originalData || {}
    newNode.value = { 
      ...targetNode, 
      name: { ...(targetNode.originalNameMap || {ko:'', en:''}) },
      order: rawNode.order || 0
    }
    showNodeModal.value = true
  }
}

const openDomainModal = () => {
  isEditMode.value = false
  newDomain.value = { name: {ko:'', en:''}, description: {ko:'', en:''}, identifierFieldId: null, displayNameFieldId: null, descriptionFieldId: null }
  showDomainModal.value = true
}

const openNodeModal = () => {
  if (!selectedNode.value || !selectedNode.value.isDomain) return
  isEditMode.value = false
  newNode.value = { name: {ko:'', en:''}, order: 0 }
  showNodeModal.value = true
}

const openFieldModal = (rowData = null) => {
  if (rowData) {
    isEditMode.value = true
    editingId.value = rowData.id
    newField.value = { 
      ...rowData, 
      name: { ...rowData.name }, 
      formula: rowData.formula || '', 
      unit: rowData.unit || '',
      fieldGroupId: rowData.fieldGroup?.id || null,
      gridWidth: rowData.gridWidth || null
    }
    if (['SELECT', 'MULTI_SELECT'].includes(rowData.type)) {
      try {
        newFieldOptionsList.value = JSON.parse(rowData.options || '[]')
      } catch (e) { newFieldOptionsList.value = [] }
    } else {
      newFieldOptionsList.value = []
    }
    // Parse targetDomainId from options for DOMAIN_REFERENCE fields
    if (rowData.type === 'DOMAIN_REFERENCE' && rowData.options) {
      try {
        const opts = JSON.parse(rowData.options)
        if (opts.targetDomainId) {
          newField.value.targetDomainId = opts.targetDomainId
        }
      } catch (e) {}
    }
    // Parse formula from options for CALCULATED fields
    if (rowData.type === 'CALCULATED' && rowData.options) {
      try {
        const opts = JSON.parse(rowData.options)
        if (opts.formula) {
          newField.value.formula = opts.formula
        }
      } catch (e) {}
    }
  } else {
    isEditMode.value = false
    editingId.value = null
    newField.value = { name: {ko:'', en:''}, key: '', type: 'STRING', required: false, order: 0, fieldGroupId: null, targetDomainId: null, isMultiValue: false, isSearchable: true, isEncrypted: false, isReadOnly: false, isImmutable: false, isHidden: false, formula: '', unit: '', gridWidth: null }
    newFieldOptionsList.value = []
  }
  showFieldModal.value = true
}
const saveDomain = async () => {
  try {
    const url = isEditMode.value ? `/api/domains/${newDomain.value.id}` : `/api/domains`
    await $fetch(url, {
      method: isEditMode.value ? 'PUT' : 'POST',
      headers: getAuthHeaders(),
      body: {
        name: newDomain.value.name,
        description: newDomain.value.description,
        identifierFieldId: newDomain.value.identifierFieldId,
        displayNameFieldId: newDomain.value.displayNameFieldId,
        descriptionFieldId: newDomain.value.descriptionFieldId
      }
    })
    showDomainModal.value = false
    await loadTree()
  } catch (e) {
    alert(t('error_saving_domain'))
  }
}

const saveNode = async () => {
  if (!selectedNode.value) return
  if (!isEditMode.value && !selectedNode.value.isDomain) return
  
  try {
    const targetId = isEditMode.value ? newNode.value.id : selectedNode.value.id
    const url = isEditMode.value ? `/api/nodes/${targetId}` : `/api/domains/${targetId}/nodes`
    await $fetch(url, {
      method: isEditMode.value ? 'PUT' : 'POST',
      headers: getAuthHeaders(),
      body: {
        name: newNode.value.name,
        order: newNode.value.order,
        parentId: isEditMode.value ? undefined : null // currently subnodes are not supported in UI
      }
    })
    showNodeModal.value = false
    await loadTree()
  } catch (e) {
    alert('Error saving node')
  }
}

const saveField = async () => {
  const duplicate = fields.value.find(f => f.key === newField.value.key && (!isEditMode.value || f.id !== editingId.value))
  if (duplicate) {
    alert(t('field_key_already_exists_newfield_value_key'))
    return
  }
  
  if (['SELECT', 'MULTI_SELECT'].includes(newField.value.type)) {
    const hasEmptyKey = newFieldOptionsList.value.some(opt => !opt.key || String(opt.key).trim() === '')
    if (hasEmptyKey) {
      alert(t('enter_key_all_options'))
      return
    }
    newField.value.options = JSON.stringify(newFieldOptionsList.value)
  } else if (newField.value.type === 'DOMAIN_REFERENCE') {
    if (!newField.value.targetDomainId) {
      alert(t('please_select_a_target_domain'))
      return
    }
    newField.value.options = JSON.stringify({ targetDomainId: newField.value.targetDomainId })
  } else if (newField.value.type === 'CALCULATED') {
    if (!newField.value.formula || String(newField.value.formula).trim() === '') {
      alert(t('enter_formula'))
      return
    }
    try {
      const testFormula = newField.value.formula.replace(/\${[^}]+}/g, '1')
      const ROUND = (val, dec=0) => Number(Math.round(val+'e'+dec)+'e-'+dec);
      const fn = new Function('ROUND', 'ABS', 'CEIL', 'FLOOR', `return ${testFormula};`)
      fn(ROUND, Math.abs, Math.ceil, Math.floor)
    } catch (e) {
      alert(t('syntax_error_in_formula_e_message'))
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

    const payload = {
      name: newField.value.name,
      fieldGroupId: newField.value.fieldGroupId || newField.value.fieldGroup?.id || null,
      key: newField.value.key,
      type: newField.value.type,
      unit: newField.value.unit,
      options: newField.value.options,
      required: newField.value.required,
      defaultValue: newField.value.defaultValue,
      order: newField.value.order,
      gridWidth: newField.value.gridWidth ? Number(newField.value.gridWidth) : null,
      isMultiValue: newField.value.isMultiValue,
      isTable: newField.value.isTable,
      isEncrypted: newField.value.isEncrypted,
      isSearchable: newField.value.isSearchable,
      isReadOnly: newField.value.isReadOnly,
      isImmutable: newField.value.isImmutable,
      isHidden: newField.value.isHidden
    }

    await $fetch(url, {
      method,
      headers: { Authorization: `Bearer ${token.value}` },
      body: payload
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
  min-height: 0;
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
