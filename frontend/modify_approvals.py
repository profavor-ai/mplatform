import re

with open('C:\\dev\\ai\\frontend\\pages\\approvals.vue', 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Replace the pendingSteps va-card grid with AgGridVue and Modal
grid_modal_html = '''
    <div style="display: flex; gap: 1rem; margin-bottom: 1rem;">
      <va-button preset="primary" icon="check_circle" @click="bulkApprove" :disabled="!pendingSelectedRows.length">
        선택 승인 ({{ pendingSelectedRows.length }})
      </va-button>
      <va-button preset="primary" color="danger" icon="cancel" @click="bulkReject" :disabled="!pendingSelectedRows.length">
        선택 반려 ({{ pendingSelectedRows.length }})
      </va-button>
    </div>

    <div v-if="pendingSteps.length === 0" style="text-align: center; padding: 4rem; color: #666; background: white; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.05);">
      <va-icon name="check_circle_outline" size="large" color="success" style="margin-bottom: 1rem;" />
      <h3>{{ t('allDone') }}</h3>
      <p>{{ t('noRequests') }}</p>
    </div>

    <div v-else class="ag-theme-alpine" style="height: 400px; width: 100%; margin-bottom: 2rem;">
      <AgGridVue
        style="width: 100%; height: 100%;"
        :columnDefs="pendingColumnDefs"
        :rowData="pendingSteps"
        :gridOptions="pendingGridOptions"
      />
    </div>

    <!-- Action Modal for Pending Step -->
    <va-modal v-model="showActionModal" size="large" hide-default-actions>
      <template #header>
        <h3 style="margin: 0; font-size: 1.25rem;">결재 심사</h3>
      </template>
      <div v-if="selectedPendingStep" style="padding: 1rem 0;">
        <div style="display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #eee; padding-bottom: 1rem; margin-bottom: 1rem;">
          <div style="display: flex; align-items: center; gap: 0.5rem;">
            <va-badge :text="selectedPendingStep.stepType === \\'CONSENSUS\\' ? t('consensus') : t('finalApproval')" :color="selectedPendingStep.stepType === \\'CONSENSUS\\' ? \\'warning\\' : \\'primary\\'" />
            <span style="font-size: 1.1rem;">{{ t('step') }} #{{ selectedPendingStep.stepOrder }}</span>
          </div>
          <span style="font-size: 0.8rem; color: #999;">{{ formatDate(selectedPendingStep.createdAt) }}</span>
        </div>
        
        <div style="margin-bottom: 1rem; display: flex; align-items: center; gap: 0.5rem; color: #555;">
          <va-icon name="person" size="small" />
          <span style="font-size: 0.9rem;">{{ t('requestedBy') }} <strong>{{ getUserName(selectedPendingStep.approvalRequest?.requesterId) }}</strong></span>
        </div>
        
        <!-- Data Changes Display -->
        <div style="background-color: #f8f9fa; border-left: 4px solid #154ec1; border-radius: 4px; padding: 1rem; margin-bottom: 1.5rem;">
          <h4 style="margin-top: 0; margin-bottom: 0.8rem; font-size: 0.9rem; color: #333; font-weight: bold;">{{ t('requestedData') }}</h4>
          
          <template v-if="getParsedChanges(selectedPendingStep.approvalRequest?.changes)">
            <div v-for="sector in getGroupedChangesList(selectedPendingStep.approvalRequest.changes, selectedPendingStep.approvalRequest.targetType)" :key="sector.key" style="margin-bottom: 1rem;">
              <div style="font-weight: bold; padding: 0.5rem; background: #eef2f5; border-radius: 4px; font-size: 0.95rem; color: #154ec1; display: flex; align-items: center; gap: 0.5rem;">
                <va-icon name="folder" size="small" /> {{ sector.label }}
              </div>
              
              <va-accordion multiple style="width: 100%; margin-top: 0.5rem;">
                <va-collapse v-for="group in sector.groups" :key="group.key" :header="group.label" solid color="background-element" style="margin-bottom: 0.5rem;">
                  <div style="display: flex; flex-direction: column; gap: 1rem; padding: 0.5rem;">
                    <template v-for="f in group.fields" :key="f.key">
                      <div v-if="selectedPendingStep.approvalRequest.targetType !== \\'RECORD_UPDATE\\' || (f.val.isChanged || (f.val.before !== f.val.after))" style="border: 1px solid #e0e0e0; border-radius: 8px; overflow: hidden; background: #fff; box-shadow: 0 1px 3px rgba(0,0,0,0.05);">
                        <div style="background: #f8f9fa; padding: 0.75rem 1rem; border-bottom: 1px solid #e0e0e0; font-weight: 600; color: #333; display: flex; justify-content: space-between; align-items: center;">
                          {{ f.label }}
                          <va-badge v-if="selectedPendingStep.approvalRequest.targetType === \\'RECORD_UPDATE\\' && f.val.isChanged" text="Modified" color="warning" size="small" />
                        </div>
                        <div style="padding: 0;">
                          <template v-if="selectedPendingStep.approvalRequest.targetType === \\'RECORD_UPDATE\\'">
                            <div v-if="f.val.isChanged" style="display: flex; flex-direction: column;">
                              <div style="background-color: #ffebee; border-bottom: 1px solid #ffcdd2; padding: 0.75rem 1rem; display: flex; align-items: flex-start; gap: 0.5rem;">
                                <va-icon name="remove_circle_outline" color="danger" size="small" style="margin-top: 2px;" />
                                <div style="color: #b71c1c; word-break: break-all; width: 100%;">
                                  <template v-if="getFilesList(f.val.before).length > 0">
                                    <div v-for="(fileUrl, idx) in getFilesList(f.val.before)" :key="idx" style="margin-bottom: 4px;">
                                      <a :href="fileUrl" target="_blank" style="color: #b71c1c; text-decoration: underline; display: inline-flex; align-items: center; gap: 4px;">
                                        <va-icon name="attach_file" size="small" />{{ getFileName(fileUrl) }}
                                      </a>
                                    </div>
                                  </template>
                                  <template v-else>{{ f.val.before }}</template>
                                </div>
                              </div>
                              <div style="background-color: #e8f5e9; padding: 0.75rem 1rem; display: flex; align-items: flex-start; gap: 0.5rem;">
                                <va-icon name="add_circle_outline" color="success" size="small" style="margin-top: 2px;" />
                                <div style="color: #1b5e20; font-weight: 500; word-break: break-all; width: 100%;">
                                  <template v-if="getFilesList(f.val.after).length > 0">
                                    <div v-for="(fileUrl, idx) in getFilesList(f.val.after)" :key="idx" style="margin-bottom: 4px;">
                                      <a :href="fileUrl" target="_blank" style="color: #1b5e20; text-decoration: underline; display: inline-flex; align-items: center; gap: 4px;">
                                        <va-icon name="attach_file" size="small" />{{ getFileName(fileUrl) }}
                                      </a>
                                    </div>
                                  </template>
                                  <template v-else>{{ f.val.after }}</template>
                                </div>
                              </div>
                            </div>
                            <div v-else style="padding: 0.75rem 1rem; color: #555; background: #fafafa;">
                              <template v-if="getFilesList(f.val.before).length > 0">
                                <div v-for="(fileUrl, idx) in getFilesList(f.val.before)" :key="idx" style="margin-bottom: 4px;">
                                  <a :href="fileUrl" target="_blank" style="color: #154ec1; text-decoration: underline; display: inline-flex; align-items: center; gap: 4px;">
                                    <va-icon name="attach_file" size="small" />{{ getFileName(fileUrl) }}
                                  </a>
                                </div>
                              </template>
                              <template v-else>{{ f.val.before }}</template>
                            </div>
                          </template>
                          <template v-else>
                            <div style="padding: 0.75rem 1rem; color: #333;">
                              <template v-if="getFilesList(f.val).length > 0">
                                <div v-for="(fileUrl, idx) in getFilesList(f.val)" :key="idx" style="margin-bottom: 4px;">
                                  <a :href="fileUrl" target="_blank" style="color: #154ec1; text-decoration: underline; display: inline-flex; align-items: center; gap: 4px;">
                                    <va-icon name="attach_file" size="small" />{{ getFileName(fileUrl) }}
                                  </a>
                                </div>
                              </template>
                              <template v-else>{{ f.val }}</template>
                            </div>
                          </template>
                        </div>
                      </div>
                    </template>
                  </div>
                </va-collapse>
              </va-accordion>
            </div>
          </template>
          <div v-else style="color: #888; font-style: italic; font-size: 0.9rem;">
            {{ t('noParsable') }}
          </div>
        </div>

        <!-- Simple Approval Line Summary -->
        <div style="margin-bottom: 1.5rem; padding: 0.75rem; background-color: #f1f8ff; border-radius: 4px; border-left: 4px solid #154ec1;">
          <div style="font-size: 0.85rem; color: #555; margin-bottom: 0.25rem;">결재라인 (요약):</div>
          <div style="font-weight: bold; color: #154ec1;">{{ getApprovalLineString(selectedPendingStep.approvalRequest?.steps) }}</div>
        </div>

        <!-- Approval Line Status -->
        <div v-if="selectedPendingStep.approvalRequest?.steps && selectedPendingStep.approvalRequest.steps.length > 0" style="margin-bottom: 1.5rem; padding-top: 0.5rem; border-top: 1px solid #eee;">
          <div style="font-weight: 600; font-size: 0.95rem; margin-bottom: 0.5rem; color: #333;">결재선 현황</div>
          <div v-for="group in getGroupedSteps(selectedPendingStep.approvalRequest.steps)" :key="group.order" style="margin-bottom: 1rem;">
            <div style="font-weight: bold; margin-bottom: 0.25rem; font-size: 0.9rem;">{{ group.order }}단계</div>
            <div style="display: flex; gap: 0.5rem; flex-wrap: wrap;">
              <div v-for="s in group.steps" :key="s.id" style="flex: 1; min-width: 200px; background: #f8f9fa; padding: 0.5rem; border-radius: 4px; font-size: 0.85rem; border: 1px solid #eaeaea;">
                <div style="display: flex; justify-content: space-between; margin-bottom: 4px;">
                  <span style="font-weight: bold; color: #154ec1;">{{ s.stepType === \\'CONSENSUS\\' ? \\'합의\\' : \\'결재\\' }} - {{ getUserName(s.assigneeId) }}</span>
                  <va-badge :text="s.status" :color="s.status === \\'APPROVED\\' ? \\'success\\' : (s.status === \\'REJECTED\\' ? \\'danger\\' : \\'warning\\')" size="small" />
                </div>
                <div v-if="s.status === \\'APPROVED\\' || s.status === \\'REJECTED\\'" style="font-size: 0.75rem; color: #888; margin-bottom: 4px; text-align: right;">
                  {{ new Date(s.updatedAt).toLocaleString() }} 처리됨
                </div>
                <div v-if="s.comment" style="color: #555; background: #fff; padding: 4px 8px; border-radius: 4px; border-left: 3px solid #ccc; font-style: italic;">
                  "{{ s.comment }}"
                </div>
                <div v-else style="color: #999; font-style: italic;">
                  의견 없음
                </div>
              </div>
            </div>
          </div>
          
          <div v-if="getObserversList(selectedPendingStep.approvalRequest?.observerIds).length > 0" style="margin-top: 1rem; padding-top: 1rem; border-top: 1px dashed #ccc;">
            <div style="font-weight: 600; font-size: 0.9rem; margin-bottom: 0.5rem; color: #555;">통보자(참조)</div>
            <div style="display: flex; gap: 0.5rem; flex-wrap: wrap;">
              <va-badge v-for="obsId in getObserversList(selectedPendingStep.approvalRequest?.observerIds)" :key="obsId" color="info" preset="secondary">{{ getUserName(obsId) }}</va-badge>
            </div>
          </div>
        </div>

        <va-input 
          v-model="commentData[selectedPendingStep.id]" 
          :placeholder="t('addComment')" 
          class="mb-3"
          style="width: 100%; margin-bottom: 1rem;"
        />

        <!-- Actions -->
        <div style="display: flex; gap: 1rem;">
          <va-button color="success" icon="check" style="flex: 1;" @click="handleSingleAction(selectedPendingStep.id, \\'approve\\')">{{ t('approve') }}</va-button>
          <va-button color="danger" icon="close" preset="secondary" style="flex: 1;" @click="handleSingleAction(selectedPendingStep.id, \\'reject\\')">{{ t('reject') }}</va-button>
        </div>
      </div>
    </va-modal>'''

# Apply HTML replacement for pendingSteps grid
pattern_html = re.compile(r'<div v-if="pendingSteps\.length === 0".*?</div>\s*<!-- My Submitted Requests Section -->', re.DOTALL)
content = pattern_html.sub(grid_modal_html + '\n\n    <!-- My Submitted Requests Section -->', content)

# 2. Update myRequests Details Modal to show 'updatedAt' for steps
my_req_steps_replacement = '''<div v-if="s.status === \\'APPROVED\\' || s.status === \\'REJECTED\\'" style="font-size: 0.75rem; color: #888; margin-bottom: 4px; text-align: right;">
                  {{ new Date(s.updatedAt).toLocaleString() }} 처리됨
                </div>
                <div v-if="s.comment" style="color: #555; background: #fff; padding: 4px 8px; border-radius: 4px; border-left: 3px solid #ccc; font-style: italic;">
                  "{{ s.comment }}"
                </div>
                <div v-else style="color: #999; font-style: italic;">
                  의견 없음
                </div>'''

pattern_my_req = re.compile(r'<div v-if="s\.comment".*?의견 없음\s*</div>', re.DOTALL)
content = pattern_my_req.sub(my_req_steps_replacement, content)


# 3. Modify Script section: adding pendingGridOptions, pendingColumnDefs, methods
script_additions = '''
const showActionModal = ref(false)
const selectedPendingStep = ref(null)
const pendingSelectedRows = ref([])

const pendingColumnDefs = computed(() => [
  { headerName: '', field: 'checkbox', checkboxSelection: true, headerCheckboxSelection: true, width: 50 },
  { field: 'approvalRequest.targetType', headerName: 'Target Type', flex: 1 },
  { 
    field: 'approvalRequest.steps', 
    headerName: '결재라인', 
    flex: 2, 
    valueFormatter: params => getApprovalLineString(params.value)
  },
  { field: 'createdAt', headerName: t('created'), flex: 1, valueFormatter: params => params.value ? new Date(params.value).toLocaleString() : '' },
  { field: 'stepType', headerName: 'Step Type', flex: 1 },
  {
    headerName: 'Action',
    field: 'id',
    cellRenderer: (params) => {
      const eDiv = document.createElement('div')
      const eButton = document.createElement('button')
      eButton.innerHTML = '심사하기'
      eButton.style.padding = '4px 12px'
      eButton.style.cursor = 'pointer'
      eButton.style.background = '#154ec1'
      eButton.style.color = 'white'
      eButton.style.border = 'none'
      eButton.style.borderRadius = '4px'
      eButton.onclick = () => {
        selectedPendingStep.value = params.data
        showActionModal.value = true
      }
      eDiv.appendChild(eButton)
      return eDiv
    },
    flex: 1
  }
])

const pendingGridOptions = ref({
  rowSelection: 'multiple',
  onSelectionChanged: (event) => {
    pendingSelectedRows.value = event.api.getSelectedRows()
  }
})

const bulkApprove = async () => {
  if (!pendingSelectedRows.value.length) return
  if (!confirm('선택한 ' + pendingSelectedRows.value.length + '건을 일괄 승인하시겠습니까?')) return
  await Promise.all(pendingSelectedRows.value.map(row => handleAction(row.id, 'approve', true)))
  pendingSelectedRows.value = []
  await loadRequests()
}

const bulkReject = async () => {
  if (!pendingSelectedRows.value.length) return
  if (!confirm('선택한 ' + pendingSelectedRows.value.length + '건을 일괄 반려하시겠습니까?')) return
  await Promise.all(pendingSelectedRows.value.map(row => handleAction(row.id, 'reject', true)))
  pendingSelectedRows.value = []
  await loadRequests()
}

const handleSingleAction = async (stepId, action) => {
  await handleAction(stepId, action)
  showActionModal.value = false
}
'''

content = content.replace('const pendingSteps = ref([])', 'const pendingSteps = ref([])' + script_additions)


# 4. Add "Details" button to myRequestsGrid
my_req_col_addition = '''
  { field: 'status', headerName: 'Status', flex: 1, 
    cellRenderer: (params) => {
      const status = params.value
      let color = '#ccc'
      if (status === 'APPROVED') color = '#4caf50'
      else if (status === 'REJECTED') color = '#e53935'
      else if (status === 'PENDING') color = '#ff9800'
      return <span style="color: ; font-weight: bold;"></span>
    }
  },
  {
    headerName: '결재 내역',
    field: 'id',
    cellRenderer: (params) => {
      const eDiv = document.createElement('div')
      const eButton = document.createElement('button')
      eButton.innerHTML = '결재 내역 보기'
      eButton.style.padding = '4px 12px'
      eButton.style.cursor = 'pointer'
      eButton.style.background = '#eef2f5'
      eButton.style.color = '#154ec1'
      eButton.style.border = '1px solid #154ec1'
      eButton.style.borderRadius = '4px'
      eButton.onclick = () => {
        selectedRequest.value = params.data
        showDetailsModal.value = true
      }
      eDiv.appendChild(eButton)
      return eDiv
    },
    flex: 1
  }
])'''

content = re.sub(r'\{\s*field:\s*\'status\',\s*headerName:\s*\'Status\',\s*flex:\s*1,.*?\]\)', my_req_col_addition, content, flags=re.DOTALL)


# 5. Modify handleAction to accept "isBulk" to skip loadRequests
handle_action_pattern = re.compile(r'const handleAction = async \(stepId, action\) => \{.*?\n\}', re.DOTALL)
handle_action_replacement = '''const handleAction = async (stepId, action, isBulk = false) => {
  if (!isBulk && !confirm(Are you sure you want to  this step?)) return
  
  const comment = commentData.value[stepId] || ''
  
  try {
    await (/api/approval-requests/steps//?approverId=, {
      method: 'POST',
      headers: { Authorization: Bearer  },
      body: { comment }
    })
    init({ message: Successfully d!, color: 'success' })
    if (!isBulk) {
        await loadRequests()
    }
  } catch (error) {
    console.error(Failed to :, error)
    init({ message: Error: , color: 'danger' })
  }
}'''

content = handle_action_pattern.sub(handle_action_replacement, content)

with open('C:\\dev\\ai\\frontend\\pages\\approvals.vue', 'w', encoding='utf-8') as f:
    f.write(content)
print("File successfully modified.")
