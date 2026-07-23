<template>
  <div class="page-container p-4">
    <div class="page-header flex justify-between items-center mb-4">
      <h1 class="page-title text-2xl font-bold">Steward Review (매칭 후보 검토 큐)</h1>
      <va-button color="primary" icon="refresh" @click="fetchCandidates">새로고침</va-button>
    </div>

    <va-card>
      <va-card-content>
        <va-data-table :items="candidates" :columns="columns" :loading="isLoading" striped>
          <template #cell(score)="{ rowData }">
            <va-badge :text="(rowData.score * 100).toFixed(1) + '%'" :color="rowData.score >= 0.85 ? 'warning' : 'info'" />
          </template>
          <template #cell(createdAt)="{ rowData }">
            {{ formatDate(rowData.createdAt) }}
          </template>
          <template #cell(actions)="{ rowData }">
            <div class="flex gap-2">
              <va-button size="small" color="success" @click="confirmCandidate(rowData)">병합 승인</va-button>
              <va-button size="small" color="danger" @click="rejectCandidate(rowData)">반려(신규생성)</va-button>
            </div>
          </template>
        </va-data-table>
      </va-card-content>
    </va-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useToast } from 'vuestic-ui'

const { init } = useToast()
const token = useCookie('auth_token')
const isLoading = ref(false)
const candidates = ref([])

const columns = [
  { key: 'id', label: 'Candidate ID' },
  { key: 'source', label: 'Source' },
  { key: 'score', label: 'Similarity Score' },
  { key: 'status', label: 'Status' },
  { key: 'createdAt', label: 'Created At' },
  { key: 'actions', label: 'Actions', width: '180px' }
]

const formatDate = (str) => {
  if (!str) return ''
  return new Date(str).toLocaleString()
}

const fetchCandidates = async () => {
  isLoading.value = true
  try {
    const res = await $fetch('/api/domains/default/match-candidates', {
      headers: { Authorization: `Bearer ${token.value}` }
    })
    candidates.value = res.content || []
  } catch (e) {
    init({ message: '후보 목록 조회 실패', color: 'danger' })
  } finally {
    isLoading.value = false
  }
}

const confirmCandidate = async (item) => {
  try {
    await $fetch(`/api/match-candidates/${item.id}/confirm`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${token.value}` }
    })
    init({ message: '매칭 병합 승인 완료', color: 'success' })
    fetchCandidates()
  } catch (e) {
    init({ message: '승인 실패', color: 'danger' })
  }
}

const rejectCandidate = async (item) => {
  try {
    await $fetch(`/api/match-candidates/${item.id}/reject`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${token.value}` }
    })
    init({ message: '반려 및 신규 생성 완료', color: 'info' })
    fetchCandidates()
  } catch (e) {
    init({ message: '반려 실패', color: 'danger' })
  }
}

onMounted(() => {
  fetchCandidates()
})
</script>
