const fs = require('fs')
const path = require('path')

const file = path.join('C:\\dev\\ai\\frontend\\pages\\approvals.vue')
let content = fs.readFileSync(file, 'utf-8')

// Replace the old step rendering block
const oldStepBlock = `<div v-if="selectedRequest.steps && selectedRequest.steps.length > 0" style="margin-top: 1rem; padding-top: 1rem; border-top: 1px solid #eee;">
          <div style="font-weight: 600; font-size: 0.95rem; margin-bottom: 0.5rem; color: #333;">결재 이력</div>
          <div v-for="step in selectedRequest.steps" :key="step.id" style="margin-bottom: 0.5rem; background: #f8f9fa; padding: 0.5rem; border-radius: 4px; font-size: 0.85rem;">
            <div style="display: flex; justify-content: space-between; margin-bottom: 4px;">
              <span style="font-weight: bold; color: #154ec1;">{{ step.stepType === 'CONSENSUS' ? t('consensus') : t('finalApproval') }} - {{ getUserName(step.approverId) }}</span>
              <va-badge :text="step.status" :color="step.status === 'APPROVED' ? 'success' : (step.status === 'REJECTED' ? 'danger' : 'warning')" size="small" />
            </div>
            <div v-if="step.comment" style="color: #555; background: #fff; padding: 4px 8px; border-radius: 4px; border-left: 3px solid #ccc; font-style: italic;">
              "{{ step.comment }}"
            </div>
            <div v-else style="color: #999; font-style: italic;">
              의견 없음
            </div>
          </div>
        </div>`

const newStepBlock = `<div v-if="selectedRequest.steps && selectedRequest.steps.length > 0" style="margin-top: 1rem; padding-top: 1rem; border-top: 1px solid #eee;">
          <div style="font-weight: 600; font-size: 0.95rem; margin-bottom: 0.5rem; color: #333;">결재선 현황</div>
          <div v-for="group in getGroupedSteps(selectedRequest.steps)" :key="group.order" style="margin-bottom: 1rem;">
            <div style="font-weight: bold; margin-bottom: 0.25rem;">{{ group.order }}단계</div>
            <div style="display: flex; gap: 0.5rem; flex-wrap: wrap;">
              <div v-for="step in group.steps" :key="step.id" style="flex: 1; min-width: 200px; background: #f8f9fa; padding: 0.5rem; border-radius: 4px; font-size: 0.85rem; border: 1px solid #eaeaea;">
                <div style="display: flex; justify-content: space-between; margin-bottom: 4px;">
                  <span style="font-weight: bold; color: #154ec1;">{{ step.stepType === 'CONSENSUS' ? '합의' : '결재' }} - {{ getUserName(step.assigneeId) }}</span>
                  <va-badge :text="step.status" :color="step.status === 'APPROVED' ? 'success' : (step.status === 'REJECTED' ? 'danger' : 'warning')" size="small" />
                </div>
                <div v-if="step.comment" style="color: #555; background: #fff; padding: 4px 8px; border-radius: 4px; border-left: 3px solid #ccc; font-style: italic;">
                  "{{ step.comment }}"
                </div>
                <div v-else style="color: #999; font-style: italic;">
                  의견 없음
                </div>
              </div>
            </div>
          </div>
          
          <div v-if="getObserversList(selectedRequest.observerIds).length > 0" style="margin-top: 1rem; padding-top: 1rem; border-top: 1px dashed #ccc;">
            <div style="font-weight: 600; font-size: 0.9rem; margin-bottom: 0.5rem; color: #555;">통보자(참조)</div>
            <div style="display: flex; gap: 0.5rem; flex-wrap: wrap;">
              <va-badge v-for="obsId in getObserversList(selectedRequest.observerIds)" :key="obsId" color="info" preset="secondary">{{ getUserName(obsId) }}</va-badge>
            </div>
          </div>
        </div>`

if (content.includes(oldStepBlock)) {
  content = content.replace(oldStepBlock, newStepBlock)
}

// Add the helper functions
const helpers = `
const getGroupedSteps = (steps) => {
  if (!steps) return []
  const grouped = {}
  steps.forEach(s => {
    if (!grouped[s.stepOrder]) grouped[s.stepOrder] = []
    grouped[s.stepOrder].push(s)
  })
  return Object.keys(grouped).sort((a,b) => Number(a) - Number(b)).map(k => ({
    order: k,
    steps: grouped[k]
  }))
}

const getObserversList = (observersJson) => {
  if (!observersJson) return []
  try {
    const list = typeof observersJson === 'string' ? JSON.parse(observersJson) : observersJson
    if (Array.isArray(list)) return list
  } catch (e) {}
  return []
}
`

if (!content.includes('const getGroupedSteps =')) {
  content = content.replace('const t = (key) => key', 'const t = (key) => key\n' + helpers)
}

fs.writeFileSync(file, content)
console.log('Successfully updated approvals.vue')
