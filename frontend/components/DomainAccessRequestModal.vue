<template>
  <va-modal :model-value="modelValue" @update:model-value="$emit('update:modelValue', $event)" @cancel="$emit('update:modelValue', false)" @click-outside="$emit('update:modelValue', false)" :title="$t('request_domain_access')" size="small" hide-default-actions>
    <h3 class="font-bold mb-2 text-sm text-gray-700">{{ $t('request_new_domain') }}</h3>
    <div v-if="availableDomains.length === 0" class="text-gray-500 mb-4 text-sm">{{ $t('no_new_domains_available') }}</div>
    <div v-else class="mb-4">
      <va-select 
        v-model="selectedDomainToRequest" 
        multiple 
        :options="availableDomains" 
        value-by="id" 
        text-by="label" 
        :placeholder="$t('select_a_domain')" 
        class="w-full" 
      />
    </div>
    
    <div style="display: flex; justify-content: flex-end; gap: 0.5rem; margin-top: 1rem; margin-bottom: 1.5rem;">
      <va-button preset="secondary" @click="isOpen = false">{{ $t('cancel') }}</va-button>
      <va-button @click="submitAccessRequest" :disabled="!selectedDomainToRequest || selectedDomainToRequest.length === 0">{{ $t('submit_request') }}</va-button>
    </div>

    <va-divider class="my-4" />

    <div class="mb-4">
      <h3 class="font-bold mb-2 text-sm text-gray-700">{{ $t('my_granted_domains') }}</h3>
      <div v-if="domainList.length === 0" class="text-gray-500 text-sm italic">{{ $t('no_granted_domains') }}</div>
      <div v-else style="display: flex; gap: 0.5rem; flex-wrap: wrap;">
        <va-chip v-for="d in domainList" :key="d.id" color="info" size="small" outline>
          {{ getDomainName(d.name) }}
        </va-chip>
      </div>
    </div>
  </va-modal>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useCookie } from '#app'
import { useI18n } from 'vue-i18n'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue'])

const { t } = useI18n()
const tokenCookie = useCookie('auth_token')
const currentLocale = useCookie('locale', { default: () => 'ko' })

const isOpen = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const availableDomains = ref([])
const domainList = ref([])
const selectedDomainToRequest = ref([])

const getDomainName = (nameObj) => {
  if (!nameObj) return 'Unknown'
  if (typeof nameObj === 'string') {
    try {
      const parsed = JSON.parse(nameObj)
      return parsed[currentLocale.value] || parsed.ko || parsed.en || 'Unknown'
    } catch {
      return nameObj
    }
  }
  return nameObj[currentLocale.value] || nameObj.ko || nameObj.en || 'Unknown'
}

const loadDomains = async () => {
  try {
    const headers = { Authorization: `Bearer ${tokenCookie.value}` }
    const [availResult, domainResult] = await Promise.allSettled([
      $fetch('/api/permissions/domains/available', { headers }),
      $fetch('/api/domains', { headers })
    ])

    if (availResult.status === 'fulfilled' && Array.isArray(availResult.value)) {
      availableDomains.value = availResult.value.map(d => ({ id: d.id, label: getDomainName(d.name) }))
    } else {
      availableDomains.value = []
    }

    if (domainResult.status === 'fulfilled' && Array.isArray(domainResult.value)) {
      domainList.value = domainResult.value
    } else {
      domainList.value = []
    }
  } catch (e) {
    console.error('Error fetching domains for request:', e)
  }
}

watch(() => props.modelValue, (newVal) => {
  if (newVal) {
    selectedDomainToRequest.value = []
    loadDomains()
  }
})

const submitAccessRequest = async () => {
  if (!selectedDomainToRequest.value || selectedDomainToRequest.value.length === 0) return
  try {
    const headers = { Authorization: `Bearer ${tokenCookie.value}` }
    await Promise.all(selectedDomainToRequest.value.map(domainId => 
      $fetch('/api/permissions/requests', {
        method: 'POST',
        headers,
        body: { domainId }
      })
    ))
    isOpen.value = false
    selectedDomainToRequest.value = []
  } catch (e) {
    console.error('Error submitting requests:', e)
  }
}
</script>
