<template>
  <div style="padding: 1.5rem;">
    <h1 style="font-size: 1.5rem; font-weight: 700; margin-bottom: 1rem;">
      <va-icon name="analytics" /> Data Quality Dashboard
    </h1>

    <!-- Domain Selector & Actions -->
    <va-card style="margin-bottom: 1.5rem;">
      <va-card-content style="display: flex; align-items: flex-end; justify-content: space-between; gap: 1rem;">
        <va-select
          v-model="selectedDomainId"
          :options="domains"
          label="Select Domain"
          text-by="label"
          value-by="value"
          style="max-width: 400px; flex: 1;"
        />
        <va-button
          v-if="selectedDomainId"
          icon="refresh"
          color="primary"
          :loading="scanning"
          @click="triggerScan"
        >
          Run DQ Scan
        </va-button>
      </va-card-content>
    </va-card>

    <!-- Loading -->
    <div v-if="loading" style="display: flex; justify-content: center; padding: 3rem;">
      <va-progress-circle indeterminate size="3rem" />
    </div>

    <!-- Score & Stats -->
    <template v-else-if="scoreData">
      <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 1rem; margin-bottom: 1.5rem;">
        <!-- DQ Score -->
        <va-card>
          <va-card-content style="text-align: center;">
            <div style="font-size: 0.85rem; color: var(--va-text-secondary); margin-bottom: 0.5rem;">
              DQ Score
            </div>
            <va-progress-circle
              :model-value="scoreData.score"
              :color="getScoreColor(scoreData.score)"
              size="5rem"
              :thickness="0.15"
            >
              <span style="font-size: 1.3rem; font-weight: 700;">
                {{ scoreData.score }}%
              </span>
            </va-progress-circle>
          </va-card-content>
        </va-card>

        <!-- Total Records -->
        <va-card>
          <va-card-content style="text-align: center;">
            <div style="font-size: 0.85rem; color: var(--va-text-secondary); margin-bottom: 0.5rem;">
              Total Records
            </div>
            <div style="font-size: 2rem; font-weight: 700; color: var(--va-primary);">
              {{ scoreData.totalRecords?.toLocaleString() }}
            </div>
          </va-card-content>
        </va-card>

        <!-- Total Violations -->
        <va-card>
          <va-card-content style="text-align: center;">
            <div style="font-size: 0.85rem; color: var(--va-text-secondary); margin-bottom: 0.5rem;">
              Total Violations
            </div>
            <div style="font-size: 2rem; font-weight: 700; color: var(--va-danger);">
              {{ scoreData.totalViolations?.toLocaleString() }}
            </div>
          </va-card-content>
        </va-card>

        <!-- Active Rules -->
        <va-card>
          <va-card-content style="text-align: center;">
            <div style="font-size: 0.85rem; color: var(--va-text-secondary); margin-bottom: 0.5rem;">
              Active DQ Rules
            </div>
            <div style="font-size: 2rem; font-weight: 700; color: var(--va-warning);">
              {{ ruleCount }}
            </div>
          </va-card-content>
        </va-card>
      </div>

      <!-- Violations by Severity -->
      <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 1rem;">
        <va-card>
          <va-card-title>Violations by Severity</va-card-title>
          <va-card-content>
            <div v-if="Object.keys(scoreData.violationsBySeverity || {}).length === 0"
                 style="text-align: center; color: var(--va-text-secondary); padding: 2rem;">
              No violations found! 🎉
            </div>
            <div v-else style="display: flex; flex-direction: column; gap: 0.75rem;">
              <div v-for="(count, severity) in scoreData.violationsBySeverity" :key="severity"
                   style="display: flex; align-items: center; gap: 0.75rem;">
                <va-badge :text="severity" :color="severity === 'ERROR' ? 'danger' : 'warning'" style="min-width: 80px;" />
                <va-progress-bar
                  :model-value="scoreData.totalViolations > 0 ? (count / scoreData.totalViolations * 100) : 0"
                  :color="severity === 'ERROR' ? 'danger' : 'warning'"
                  style="flex: 1;"
                />
                <span style="min-width: 50px; text-align: right; font-weight: 600;">{{ count }}</span>
              </div>
            </div>
          </va-card-content>
        </va-card>

        <va-card>
          <va-card-title>Violations by Field</va-card-title>
          <va-card-content>
            <div v-if="Object.keys(scoreData.violationsByField || {}).length === 0"
                 style="text-align: center; color: var(--va-text-secondary); padding: 2rem;">
              No violations found! 🎉
            </div>
            <div v-else style="display: flex; flex-direction: column; gap: 0.5rem; max-height: 300px; overflow-y: auto;">
              <div v-for="(count, fieldKey) in sortedFieldViolations" :key="fieldKey"
                   style="display: flex; align-items: center; gap: 0.75rem; padding: 0.5rem; background: var(--va-background-element); border-radius: 6px;">
                <span style="flex: 1; font-weight: 500; font-size: 0.85rem;">{{ getFieldDisplayName(fieldKey) }}</span>
                <va-progress-bar
                  :model-value="maxFieldViolation > 0 ? (count / maxFieldViolation * 100) : 0"
                  color="danger"
                  style="flex: 2;"
                />
                <span style="min-width: 40px; text-align: right; font-weight: 600; color: var(--va-danger);">
                  {{ count }}
                </span>
              </div>
            </div>
          </va-card-content>
        </va-card>
      </div>
    </template>

    <div v-else-if="selectedDomainId" style="text-align: center; padding: 3rem; color: var(--va-text-secondary);">
      Select a domain to view DQ metrics
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'

const domains = ref([])
const selectedDomainId = ref(null)
const scoreData = ref(null)
const ruleCount = ref(0)
const loading = ref(false)
const scanning = ref(false)

const fieldMap = ref({})

const { locale } = useI18n()
const token = useCookie('auth_token')

const getHeaders = () => {
  return token.value ? { Authorization: `Bearer ${token.value}` } : {}
}

async function fetchDomainFields(domainId) {
  try {
    const headers = getHeaders()
    const fields = await $fetch(`/api/domains/${domainId}/fields`, { headers })
    const map = {}
    for (const f of (fields || [])) {
      let nameObj = f.name
      if (typeof nameObj === 'string') {
        try { nameObj = JSON.parse(nameObj) } catch (e) {}
      }
      map[f.key] = nameObj
    }
    fieldMap.value = map
  } catch (e) {
    console.error('Failed to fetch domain fields:', e)
    fieldMap.value = {}
  }
}

function getFieldDisplayName(fieldKey) {
  const nameObj = fieldMap.value[fieldKey]
  if (!nameObj) return fieldKey

  const koName = typeof nameObj === 'object' ? nameObj.ko : null
  const enName = typeof nameObj === 'object' ? nameObj.en : null

  const primaryName = locale.value === 'ko'
    ? (koName || enName)
    : (enName || koName)

  if (primaryName && primaryName !== fieldKey) {
    return `${primaryName}(${fieldKey})`
  }
  return fieldKey
}

async function triggerScan() {
  const val = selectedDomainId.value
  const domainId = typeof val === 'object' && val !== null ? (val.value || val.id) : val
  if (!domainId) return

  scanning.value = true
  try {
    const headers = getHeaders()
    const score = await $fetch(`/api/domains/${domainId}/dq-scan`, { method: 'POST', headers })
    scoreData.value = score
  } catch (e) {
    console.error('DQ scan error:', e)
  } finally {
    scanning.value = false
  }
}

// Fetch domains on mount
onMounted(async () => {
  try {
    const res = await $fetch('/api/domains', { headers: getHeaders() })
    domains.value = (res || []).map(d => {
      let nameObj = d.name
      if (typeof nameObj === 'string') {
        try { nameObj = JSON.parse(nameObj) } catch (e) {}
      }
      const labelText = typeof nameObj === 'object' && nameObj !== null
        ? (nameObj[locale.value] || nameObj.ko || nameObj.en || d.code || 'Unknown')
        : (nameObj || d.code || 'Unknown')

      return {
        label: labelText,
        value: d.id
      }
    })
  } catch (e) {
    console.error('Failed to fetch domains:', e)
  }
})

watch(selectedDomainId, async (val) => {
  if (!val) {
    scoreData.value = null
    fieldMap.value = {}
    return
  }
  const domainId = typeof val === 'object' && val !== null ? (val.value || val.id) : val
  if (!domainId) return

  loading.value = true
  try {
    const headers = getHeaders()
    await fetchDomainFields(domainId)
    const [score, rules] = await Promise.all([
      $fetch(`/api/domains/${domainId}/dq-score`, { headers }),
      $fetch(`/api/domains/${domainId}/dq-rules-count`, { headers }).catch((e) => {
        console.error('Failed to fetch dq-rules-count:', e)
        return { count: 0 }
      })
    ])
    scoreData.value = score
    ruleCount.value = rules?.count ?? 0
  } catch (e) {
    console.error('Failed to fetch DQ score:', e)
    scoreData.value = null
  } finally {
    loading.value = false
  }
})

const sortedFieldViolations = computed(() => {
  if (!scoreData.value?.violationsByField) return {}
  const entries = Object.entries(scoreData.value.violationsByField)
  entries.sort((a, b) => b[1] - a[1])
  return Object.fromEntries(entries)
})

const maxFieldViolation = computed(() => {
  if (!scoreData.value?.violationsByField) return 0
  return Math.max(...Object.values(scoreData.value.violationsByField), 1)
})

function getScoreColor(score) {
  if (score >= 90) return 'success'
  if (score >= 70) return 'warning'
  return 'danger'
}
</script>
