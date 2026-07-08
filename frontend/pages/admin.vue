<template>
  <div>
    <h1 style="font-size: 2rem; font-weight: bold; margin-bottom: 1.5rem;">Admin Process Monitor</h1>
    
    <va-card>
      <va-card-title style="display: flex; justify-content: space-between; align-items: center;">
        All Ongoing Workflows
        <va-button preset="secondary" icon="refresh" size="small" @click="loadWorkflows">Refresh</va-button>
      </va-card-title>
      
      <va-card-content>
        <div v-if="loading" style="text-align: center; padding: 2rem;">Loading...</div>
        <div v-else-if="workflows.length === 0" style="text-align: center; padding: 2rem; color: #666;">
          No ongoing workflows at the moment.
        </div>
        
        <div v-else style="display: flex; flex-direction: column; gap: 1rem;">
          <div v-for="flow in workflows" :key="flow.id" style="border: 1px solid #e0e0e0; border-radius: 8px; padding: 1.5rem;">
            
            <div style="display: flex; justify-content: space-between; margin-bottom: 1rem;">
              <div style="font-weight: bold; font-size: 1.1rem;">
                Request: {{ flow.targetType }} Creation
              </div>
              <div>
                <va-badge :text="flow.status" :color="flow.status === 'PENDING' ? 'warning' : (flow.status === 'APPROVED' ? 'success' : 'danger')" />
              </div>
            </div>

            <div style="font-size: 0.9rem; color: #555; margin-bottom: 1.5rem;">
              <strong>Requester:</strong> {{ flow.requesterId }} <br/>
              <strong>Created At:</strong> {{ new Date(flow.createdAt).toLocaleString() }}
            </div>

            <!-- Pipeline Visualizer -->
            <div style="display: flex; align-items: center; justify-content: space-between; background: #f8f9fa; padding: 1rem; border-radius: 8px;">
              <!-- DQ Check (Implied step 0) -->
              <div style="text-align: center; flex: 1;">
                <va-icon name="check_circle" color="success" size="large" />
                <div style="font-size: 0.8rem; margin-top: 0.5rem; font-weight: bold;">DQ Passed</div>
              </div>
              
              <div style="flex: 1; height: 2px; background: #ddd; margin: 0 10px;"></div>
              
              <!-- Steps -->
              <template v-for="(step, idx) in flow.steps" :key="step.id">
                <div style="text-align: center; flex: 1; position: relative;">
                  <va-icon 
                    :name="step.status === 'APPROVED' ? 'check_circle' : (step.status === 'REJECTED' ? 'cancel' : 'radio_button_unchecked')" 
                    :color="step.status === 'APPROVED' ? 'success' : (step.status === 'REJECTED' ? 'danger' : (step.status === 'PENDING' ? 'warning' : 'secondary'))" 
                    size="large" 
                  />
                  <div style="font-size: 0.8rem; margin-top: 0.5rem; font-weight: bold;">
                    {{ step.stepType === 'CONSENSUS' ? 'Consensus' : 'Final Approval' }}
                  </div>
                  <div style="font-size: 0.75rem; color: #777; margin-top: 0.2rem;">
                    {{ step.status }}
                  </div>
                  <div style="font-size: 0.75rem; color: #999; margin-top: 0.2rem;" :title="step.assigneeId">
                    {{ step.assigneeId.substring(0,8) }}...
                  </div>
                </div>
                
                <div v-if="idx < flow.steps.length - 1" style="flex: 1; height: 2px; background: #ddd; margin: 0 10px;"></div>
              </template>
            </div>
            
          </div>
        </div>
      </va-card-content>
    </va-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useCookie } from '#app'

const token = useCookie('auth_token')
const workflows = ref([])
const loading = ref(true)

const loadWorkflows = async () => {
  loading.value = true
  try {
    const headers = { Authorization: `Bearer ${tokenCookie.value}` }
    const res = await $fetch('/api/approval-requests', { headers })
    workflows.value = res
  } catch (e) {
    console.error('Failed to load workflows', e)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadWorkflows()
})
</script>
