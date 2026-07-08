const fs = require('fs')
const path = require('path')

const file = path.join('C:\\dev\\ai\\frontend\\pages\\schema.vue')
let content = fs.readFileSync(file, 'utf-8')

// Replace Workflows Tab HTML
const tabStart = content.indexOf('<!-- Workflows Tab -->')
const tabEnd = content.indexOf('</va-card-content>', tabStart)
const oldTab = content.substring(tabStart, tabEnd)

const newTab = `<!-- Workflows Tab -->
            <div v-show="activeTab === 1" style="flex: 1; display: flex; flex-direction: column; padding: 1.5rem; overflow-y: auto;">
              <div v-for="action in ['CREATE', 'UPDATE', 'DELETE']" :key="action" style="margin-bottom: 2rem; padding: 1rem; border: 1px solid #eee; border-radius: 8px;">
                <h3 style="font-weight: bold; margin-bottom: 1rem; color: #333;">{{ action }} Action Workflow</h3>
                
                <h4 style="margin-bottom: 0.5rem; border-bottom: 1px solid #ccc; padding-bottom: 0.25rem;">결재 단계 설정</h4>
                <div v-for="(step, sIdx) in workflowConfigs[action].steps" :key="step.id" style="border: 1px solid #ddd; padding: 1rem; border-radius: 4px; margin-bottom: 1rem; background: #fafafa;">
                  <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.5rem;">
                    <strong>{{ sIdx + 1 }}단계</strong>
                    <div style="display: flex; gap: 0.5rem;">
                      <va-button size="small" color="primary" preset="secondary" icon="arrow_upward" @click="moveStepUp(action, sIdx)" :disabled="sIdx === 0"></va-button>
                      <va-button size="small" color="primary" preset="secondary" icon="arrow_downward" @click="moveStepDown(action, sIdx)" :disabled="sIdx === workflowConfigs[action].steps.length - 1"></va-button>
                      <va-button size="small" color="danger" preset="secondary" icon="delete" @click="removeStep(action, sIdx)"></va-button>
                    </div>
                  </div>
                  <div v-for="(u, uIdx) in step.users" :key="uIdx" style="display: flex; gap: 1rem; align-items: center; margin-bottom: 0.5rem;">
                    <va-select style="flex: 1;" v-model="u.stepType" :options="['CONSENSUS', 'APPROVAL']" placeholder="결재 타입" />
                    <va-select style="flex: 2;" v-model="u.assigneeId" :options="userOptions" value-by="value" text-by="text" placeholder="결재자 선택" />
                    <va-button size="small" color="danger" preset="secondary" icon="remove_circle" @click="removeUserFromStep(action, sIdx, uIdx)" :disabled="step.users.length === 1"></va-button>
                  </div>
                  <va-button size="small" preset="secondary" icon="add" @click="addUserToStep(action, sIdx)">병렬 결재자 추가</va-button>
                </div>
                <va-button preset="secondary" icon="add" @click="addStep(action)">다음 단계 추가</va-button>
                
                <div style="margin-top: 1.5rem;">
                  <h4 style="margin-bottom: 0.5rem; border-bottom: 1px solid #ccc; padding-bottom: 0.25rem;">통보자 지정</h4>
                  <va-select
                    v-model="workflowConfigs[action].observerIds"
                    :options="userOptions"
                    value-by="value"
                    text-by="text"
                    multiple
                    placeholder="통보자를 선택하세요"
                  />
                </div>
              </div>
              <div style="display: flex; justify-content: flex-end;">
                <va-button color="success" icon="save" @click="saveWorkflowConfigs">Save Workflows</va-button>
              </div>
            </div>
            `
content = content.replace(oldTab, newTab)

// Replace workflowConfigs init
content = content.replace(
`const workflowConfigs = ref({
  CREATE: { consensusUserId: null, approverUserId: null },
  UPDATE: { consensusUserId: null, approverUserId: null },
  DELETE: { consensusUserId: null, approverUserId: null }
})`,
`const workflowConfigs = ref({
  CREATE: { steps: [], observerIds: [] },
  UPDATE: { steps: [], observerIds: [] },
  DELETE: { steps: [], observerIds: [] }
})`)

// Replace in load node function
const match = `    workflowConfigs.value = {
      CREATE: { consensusUserId: null, approverUserId: null },
      UPDATE: { consensusUserId: null, approverUserId: null },
      DELETE: { consensusUserId: null, approverUserId: null }
    }
    for (const wf of wfData) {`
const repl = `    workflowConfigs.value = {
      CREATE: { steps: [], observerIds: [] },
      UPDATE: { steps: [], observerIds: [] },
      DELETE: { steps: [], observerIds: [] }
    }
    for (const wf of wfData) {`
content = content.replace(match, repl)

// Replace loading workflow configs logic
const loadWfStart = content.indexOf('for (const wf of wfData) {')
const loadWfEnd = content.indexOf('}', content.indexOf('}', loadWfStart) + 1) + 1
const oldLoadWf = content.substring(loadWfStart, loadWfEnd)
const newLoadWf = `for (const wf of wfData) {
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
    }`
content = content.replace(oldLoadWf, newLoadWf)

// Replace saveWorkflowConfigs
const saveWfStart = content.indexOf('const payloads = Object.keys(workflowConfigs.value).map(action => ({')
const saveWfEnd = content.indexOf('}))', saveWfStart) + 3
const oldSaveWf = content.substring(saveWfStart, saveWfEnd)
const newSaveWf = `const payloads = Object.keys(workflowConfigs.value).map(action => {
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
    })`
content = content.replace(oldSaveWf, newSaveWf)

// Add step methods before saveWorkflowConfigs
const methodStr = `
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

`
const insertIdx = content.indexOf('const saveWorkflowConfigs')
content = content.substring(0, insertIdx) + methodStr + content.substring(insertIdx)

fs.writeFileSync(file, content)
console.log('Successfully updated schema.vue')
