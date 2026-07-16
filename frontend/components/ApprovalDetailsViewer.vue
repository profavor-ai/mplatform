<template>
  <div>
    <!-- Data Changes Display -->
    <div style="background-color: var(--va-background-secondary); border-left: 4px solid var(--va-primary); border-radius: 4px; padding: 1rem 0 1rem 1rem; margin-bottom: 1.5rem;">
      <h4 style="margin-top: 0; margin-bottom: 0.8rem; font-size: 0.9rem; color: var(--va-text-primary); font-weight: bold;">{{ t('requestedData') }}</h4>
      
      <template v-if="getParsedChanges(request?.changes)">
        <div class="custom-scrollbar">
          <div v-for="sector in getGroupedChangesList(request.changes, request.targetType)" :key="sector.key" style="margin-bottom: 1rem;">
            <div style="font-weight: bold; padding: 0.5rem; background: var(--va-background-secondary); border-radius: 4px; font-size: 0.95rem; color: var(--va-primary); display: flex; align-items: center; gap: 0.5rem;">
              <va-icon name="folder" size="small" /> {{ sector.label }}
            </div>
            
            <div style="width: 100%; margin-top: 0.5rem; display: flex; flex-direction: column; gap: 0.5rem;">
              <div v-for="group in sector.groups" :key="group.key" style="border: 1px solid var(--va-background-border); border-radius: 4px; overflow: hidden; background: var(--va-background-element);">
                <div style="background: var(--va-background-secondary); padding: 0.75rem 1rem; font-weight: bold; font-size: 0.95rem; color: var(--va-text-primary); border-bottom: 1px solid var(--va-background-border);">
                  {{ group.label }}
                </div>
                <div style="display: grid; grid-template-columns: repeat(12, 1fr); gap: 1rem; padding: 0.75rem;">
                  <template v-for="f in group.fields" :key="f.key">
                    <div v-if="request.targetType !== 'RECORD_UPDATE' || (f.val.isChanged || (f.val.before !== f.val.after))" :style="{ gridColumn: 'span ' + (f.gridWidth || 12), border: '1px solid var(--va-background-border)', borderRadius: '8px', overflow: 'hidden', background: 'var(--va-background-element)', boxShadow: 'var(--va-box-shadow)' }">
                      <div style="background: var(--va-background-secondary); padding: 0.75rem 1rem; border-bottom: 1px solid var(--va-background-border); font-weight: 600; font-size: 0.85rem; color: var(--va-text-primary); display: flex; justify-content: space-between; align-items: center;">
                        {{ f.label }}
                        <va-badge v-if="request.targetType === 'RECORD_UPDATE' && f.val.isChanged" color="warning" size="small">{{ t('modified') }}</va-badge>
                      </div>
                      <div style="padding: 0;">
                        <template v-if="request.targetType === 'RECORD_UPDATE'">
                          <div v-if="f.val.isChanged" style="display: flex; flex-direction: column;">
                            <div style="background-color: rgba(229, 57, 53, 0.1); border-bottom: 1px solid rgba(229, 57, 53, 0.2); padding: 0.75rem 1rem; font-size: 0.85rem; display: flex; align-items: flex-start; gap: 0.5rem;">
                              <va-icon name="remove_circle_outline" color="danger" size="small" style="margin-top: 2px;" />
                              <div style="color: var(--va-danger); word-break: break-all; width: 100%;">
                                <template v-if="f.type === 'FILE' && getFilesList(f.val.before).length > 0">
                                  <div v-for="(fileUrl, idx) in getFilesList(f.val.before)" :key="idx" style="margin-bottom: 4px;">
                                    <a :href="fileUrl" target="_blank" style="color: var(--va-danger); text-decoration: underline; display: inline-flex; align-items: center; gap: 4px;">
                                      <va-icon name="attach_file" size="small" />{{ getFileName(fileUrl) }}
                                    </a>
                                  </div>
                                </template>
                                <template v-else>{{ f.val.before }}</template>
                              </div>
                            </div>
                            <div style="background-color: rgba(67, 160, 71, 0.1); padding: 0.75rem 1rem; font-size: 0.85rem; display: flex; align-items: flex-start; gap: 0.5rem;">
                              <va-icon name="add_circle_outline" color="success" size="small" style="margin-top: 2px;" />
                              <div style="color: var(--va-success); font-weight: 500; word-break: break-all; width: 100%;">
                                <template v-if="f.type === 'FILE' && getFilesList(f.val.after).length > 0">
                                  <div v-for="(fileUrl, idx) in getFilesList(f.val.after)" :key="idx" style="margin-bottom: 4px;">
                                    <a :href="fileUrl" target="_blank" style="color: var(--va-success); text-decoration: underline; display: inline-flex; align-items: center; gap: 4px;">
                                      <va-icon name="attach_file" size="small" />{{ getFileName(fileUrl) }}
                                    </a>
                                  </div>
                                </template>
                                <template v-else>{{ f.val.after }}</template>
                              </div>
                            </div>
                          </div>
                          <div v-else style="padding: 0.75rem 1rem; font-size: 0.85rem; color: var(--va-text-secondary); background: var(--va-background-primary);">
                            <template v-if="f.type === 'FILE' && getFilesList(f.val.before).length > 0">
                              <div v-for="(fileUrl, idx) in getFilesList(f.val.before)" :key="idx" style="margin-bottom: 4px;">
                                <a :href="fileUrl" target="_blank" style="color: var(--va-primary); text-decoration: underline; display: inline-flex; align-items: center; gap: 4px;">
                                  <va-icon name="attach_file" size="small" />{{ getFileName(fileUrl) }}
                                </a>
                              </div>
                            </template>
                            <template v-else>{{ f.val.before }}</template>
                          </div>
                        </template>
                        <template v-else>
                          <div style="padding: 0.75rem 1rem; font-size: 0.85rem; color: var(--va-text-primary);">
                            <template v-if="f.type === 'FILE' && getFilesList(f.val).length > 0">
                              <div v-for="(fileUrl, idx) in getFilesList(f.val)" :key="idx" style="margin-bottom: 4px;">
                                <a :href="fileUrl" target="_blank" style="color: var(--va-primary); text-decoration: underline; display: inline-flex; align-items: center; gap: 4px;">
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
              </div>
            </div>
          </div>
        </div>
      </template>
      <div v-else style="color: var(--va-text-secondary); font-style: italic; font-size: 0.9rem;">
        {{ t('noParsable') }}
      </div>
    </div>

    <!-- Simple Approval Line Summary -->
    <div style="margin-bottom: 1.5rem; padding: 0.75rem; background-color: var(--va-background-secondary); border-radius: 4px; border-left: 4px solid var(--va-primary); overflow-x: auto;">
      <div style="font-size: 0.85rem; color: var(--va-text-secondary); margin-bottom: 0.75rem;">{{ t('approvalLineSummary') }}</div>
      
      <div v-if="request?.steps && request.steps.length > 0" 
           style="display: flex; align-items: center; width: 100%; overflow-x: auto; padding: 0.25rem 0;">
        <template v-for="(s, idx) in getStepperSteps(request)" :key="idx">
          <!-- Step Node -->
          <div style="display: flex; flex-direction: row; align-items: center; gap: 0.5rem; flex-shrink: 0;">
            <div 
              :class="{'step-flash': s.isPending}"
              :style="{
                width: '32px', height: '32px', borderRadius: '50%', 
                backgroundColor: s.hasError ? 'var(--va-danger)' : (s.isPending ? 'var(--va-warning)' : (idx < getCurrentStepIndex(request) ? 'var(--va-primary)' : 'var(--va-background-element)')),
                border: idx <= getCurrentStepIndex(request) ? 'none' : '2px solid var(--va-background-border)',
                color: s.isPending ? '#262824' : (idx < getCurrentStepIndex(request) ? 'white' : 'var(--va-text-secondary)'),
                display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 'bold', fontSize: '0.9rem',
                boxShadow: s.isPending ? '0 0 0 rgba(255, 212, 58, 0.4)' : 'none'
              }"
            >
              {{ s.stepOrder }}
            </div>
            <div style="font-size: 0.85rem; color: var(--va-text-primary); white-space: nowrap; display: flex; flex-direction: column; justify-content: center;">
              <div>{{ s.name }} <span style="color: var(--va-text-secondary); font-size: 0.8rem;">({{ s.statusText }})</span></div>
              <div v-if="s.processedDate" style="font-size: 0.72rem; color: var(--va-text-secondary); opacity: 0.8; margin-top: 2px;">
                {{ s.processedDate }}
              </div>
            </div>
          </div>
          <!-- Line -->
          <div v-if="idx < getStepperSteps(request).length - 1" 
               style="flex-grow: 1; min-width: 40px; height: 2px; margin: 0 1rem;"
               :style="{ backgroundColor: idx < getCurrentStepIndex(request) - 0.5 ? 'var(--va-primary)' : 'var(--va-background-border)' }">
          </div>
        </template>
      </div>
      <div v-else style="font-weight: bold; color: var(--va-primary);">{{ t('noApprovalLine') }}</div>
    </div>

    <!-- Approval Line Status -->
    <div v-if="request?.steps && request.steps.length > 0" style="margin-bottom: 1.5rem; padding-top: 0.5rem; border-top: 1px solid var(--va-background-border);">
      <div style="font-weight: 600; font-size: 0.95rem; margin-bottom: 0.5rem; color: var(--va-text-primary);">{{ t('approvalLineStatus') }}</div>
      <div v-for="group in getGroupedSteps(request)" :key="group.order" style="margin-bottom: 0.25rem;">
        <div style="display: flex; gap: 0.5rem; flex-wrap: wrap;">
          <div v-for="s in group.steps" :key="s.id" style="flex: 1; min-width: 200px; background: var(--va-background-element); padding: 0.5rem; border-radius: 4px; font-size: 0.85rem; border: 1px solid var(--va-background-border);">
            <div style="display: flex; justify-content: space-between; margin-bottom: 4px; align-items: center;">
              <span style="font-weight: bold; color: var(--va-primary); display: flex; align-items: center;">
                <span style="display:inline-flex; align-items:center; justify-content:center; width:20px; height:20px; background-color:var(--va-primary); color:white; border-radius:50%; font-size:0.75rem; margin-right:6px; font-weight:bold;">{{ s.stepOrder }}</span>
                {{ getStepTypeLabel(s) }} - {{ getUserName(s.assigneeId) }}
              </span>
              <va-badge :color="s.stepType === 'DRAFT' ? 'info' : (s.status === 'APPROVED' ? 'success' : (s.status === 'REJECTED' ? 'danger' : 'warning'))" size="small">{{ getStepStatusLabel(s) }}</va-badge>
            </div>
            <div v-if="s.status === 'APPROVED' || s.status === 'REJECTED' || s.stepType === 'DRAFT'" style="font-size: 0.75rem; color: var(--va-text-secondary); margin-bottom: 4px; text-align: right;">
              {{ formatDate(s.updatedAt) }} {{ t('processed') }}
            </div>
            <div v-if="s.comment" style="color: var(--va-text-primary); background: var(--va-background-secondary); padding: 4px 8px; border-radius: 4px; border-left: 3px solid var(--va-primary); font-style: italic;">
              "{{ s.comment }}"
            </div>
            <div v-else style="color: var(--va-text-secondary); font-style: italic;">
              {{ t('noComment') }}
            </div>
          </div>
        </div>
      </div>
      
      <div v-if="getObserversList(request?.observerIds).length > 0" style="margin-top: 1rem; padding-top: 1rem; border-top: 1px dashed var(--va-background-border);">
        <div style="font-weight: 600; font-size: 0.9rem; margin-bottom: 0.5rem; color: var(--va-text-secondary);">{{ t('observers') }}</div>
        <div style="display: flex; gap: 0.5rem; flex-wrap: wrap;">
          <va-badge v-for="obsId in getObserversList(request?.observerIds)" :key="obsId" color="info" preset="secondary">{{ getUserName(obsId) }}</va-badge>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { useCookie } from '#app'
import { useI18n } from 'vue-i18n'
const { t } = useI18n()
const props = defineProps({
  request: {
    type: Object,
    required: true
  }
})

const currentLocale = useCookie('locale', { default: () => 'ko' })
const token = useCookie('auth_token')
const userList = ref([])
const fieldNameMap = ref({})
const domainRefDisplayMap = ref({})

const loadUsers = async () => {
  try {
    const res = await fetch('/api/auth/users', {
      headers: { 'Authorization': `Bearer ${token.value}` }
    })
    if (res.ok) {
      userList.value = await res.json()
    }
  } catch (e) {
    console.error("Failed to fetch users", e)
  }
}

const loadFieldNamesForRecord = async (targetId) => {
  try {
    const record = await $fetch(`/api/records/${targetId}`, { headers: { Authorization: `Bearer ${token.value}` } })
    const nodeId = record?.node?.id || record?.nodeId;
    if (nodeId) {
      const fields = await $fetch(`/api/nodes/${nodeId}/fields/effective`, { headers: { Authorization: `Bearer ${token.value}` } })
      if (fields && fields.length > 0) {
        fields.forEach(f => {
          fieldNameMap.value[f.key] = f
        })
      }
    }
  } catch (e) {
    console.error('Error loading field names for record:', e)
  }
}

watch(() => props.request, async (newReq) => {
  if (newReq && ['RECORD', 'RECORD_UPDATE', 'RECORD_CREATE', 'RECORD_DELETE', 'CREATE', 'UPDATE', 'DELETE'].includes(newReq.targetType) && newReq.targetId) {
    await loadFieldNamesForRecord(newReq.targetId);
  }
}, { immediate: true })

onMounted(async () => {
  await loadUsers()
})

const getUserName = (uuid) => {
  if (!uuid) return ''
  const u = userList.value.find(u => u.uuid === uuid)
  return u ? u.username : uuid
}

const getStepTypeLabel = (s) => {
  if (!s) return ''
  if (s.stepType === 'CONSENSUS') return t('typeConsensus')
  if (s.stepType === 'DRAFT') return t('typeDraft')
  return t('typeApproval')
}

const getStepStatusLabel = (s) => {
  if (!s) return ''
  return s.stepType === 'DRAFT' ? t('stepDraft') : s.status
}

const getParsedChanges = (changesString) => {
  if (!changesString) return null
  if (typeof changesString === 'object') return changesString
  try {
    let parsed = JSON.parse(changesString)
    if (typeof parsed === 'string') parsed = JSON.parse(parsed)
    if (typeof parsed === 'string') parsed = JSON.parse(parsed)
    if (Object.keys(parsed || {}).length === 0) return null
    return parsed
  } catch (e) {
    console.error('Failed to parse changes:', e, changesString)
    return null
  }
}

const fetchDomainRefName = async (uuid, targetDomainId) => {
  if (!uuid || domainRefDisplayMap.value[uuid]) return;
  domainRefDisplayMap.value[uuid] = 'Loading...'; 
  try {
    const rec = await $fetch(`/api/records/${uuid}`, { headers: { Authorization: `Bearer ${token.value}` } }).catch(() => null);
    
    if (!rec) {
      const uname = getUserName(uuid);
      domainRefDisplayMap.value[uuid] = (uname && uname !== uuid) ? uname : uuid;
      return;
    }
    
    let tDomainId = targetDomainId;
    if (!tDomainId && rec.node) {
      tDomainId = rec.node.domain?.id || rec.node.domainId;
    }
    if (!tDomainId) tDomainId = rec.domainId;
    
    if (!tDomainId) {
      domainRefDisplayMap.value[uuid] = uuid;
      return;
    }

    const domains = await $fetch('/api/domains', { headers: { Authorization: `Bearer ${token.value}` } })
    const tDomain = domains.find(d => d.id === tDomainId)
    if (!tDomain) {
      domainRefDisplayMap.value[uuid] = uuid;
      return;
    }
    const dFieldId = tDomain.displayNameFieldId || tDomain.identifierFieldId
    const tFields = await $fetch(`/api/domains/${tDomainId}/fields`, { headers: { Authorization: `Bearer ${token.value}` } })
    let f = tFields.find(x => x.id === dFieldId);
    if (!f) {
      f = tFields.find(x => {
        const n = JSON.stringify(x.name).toLowerCase();
        return n.includes('name') || n.includes('이름') || n.includes('사원명') || n.includes('title') || n.includes('제목');
      });
      if (!f) f = tFields.find(x => x.type === 'TEXT');
    }
    
    if (f && rec.data) {
      const dataObj = typeof rec.data === 'string' ? JSON.parse(rec.data) : rec.data;
      const rawVal = dataObj[f.key];
      if (rawVal) {
        let displayStr = rawVal;
        if (typeof rawVal === 'string') {
          try {
            const parsed = JSON.parse(rawVal);
            if (parsed && typeof parsed === 'object') {
              displayStr = parsed[currentLocale.value] || parsed.ko || parsed.en || rawVal;
            }
          } catch(e) {}
        } else if (typeof rawVal === 'object') {
          displayStr = rawVal[currentLocale.value] || rawVal.ko || rawVal.en || JSON.stringify(rawVal);
        }
        domainRefDisplayMap.value[uuid] = displayStr;
      } else {
        domainRefDisplayMap.value[uuid] = uuid;
      }
    } else {
      domainRefDisplayMap.value[uuid] = uuid;
    }
  } catch (e) {
    const uname = getUserName(uuid);
    domainRefDisplayMap.value[uuid] = (uname && uname !== uuid) ? uname : uuid;
  }
}

const getFilesList = (v) => {
  if (!v) return []
  if (typeof v === 'string') {
    if (v.startsWith('[')) {
      try {
        const arr = JSON.parse(v)
        if (Array.isArray(arr)) {
          return arr
        }
      } catch (e) {}
    }
    return [v]
  }
  return []
}

const getFileName = (url) => {
  if (!url) return ''
  if (typeof url !== 'string') return 'Unknown File'
  try {
    if (url.includes('?name=')) {
      const qs = url.split('?name=')[1]
      return decodeURIComponent(qs.split('&')[0])
    }
    const parts = url.split('/')
    let name = parts[parts.length - 1]
    if (name.includes('?')) {
      name = name.split('?')[0]
    }
    return decodeURIComponent(name)
  } catch (e) {
    return url
  }
}

const getGroupedChangesList = (changesString, targetType) => {
  let parsed = getParsedChanges(changesString)
  if (!parsed) return []
  
  const normalizeData = (dataObj) => {
    const normalized = {};
    Object.keys(dataObj).forEach(k => {
      normalized[k.toUpperCase()] = dataObj[k];
    });
    Object.values(fieldNameMap.value || {}).forEach(f => {
      const uKey = String(f.key).toUpperCase();
      if (f.type === 'MULTILINGUAL') {
        if (typeof normalized[uKey] === 'string') {
          try { normalized[uKey] = JSON.parse(normalized[uKey]); }
          catch (e) { normalized[uKey] = { ko: normalized[uKey], en: '' }; }
        } else if (!normalized[uKey]) {
          normalized[uKey] = { ko: '', en: '' };
        }
      }
    });
    return normalized;
  };

  if (targetType === 'RECORD_UPDATE') {
    parsed.before = normalizeData(parsed.before || {})
    parsed.after = normalizeData(parsed.after || {})
  } else {
    if (targetType === 'RECORD_CREATE' || targetType === 'RECORD' || targetType === 'CREATE') {
       if (parsed.after) parsed = parsed.after;
    }
    parsed = normalizeData(parsed || {})
  }

  const map = new Map()
  let keysToProcess = []
  if (targetType === 'RECORD_UPDATE') {
    const beforeKeys = Object.keys(parsed.before || {})
    const afterKeys = Object.keys(parsed.after || {})
    keysToProcess = [...new Set([...beforeKeys, ...afterKeys])]
  } else {
    keysToProcess = Object.keys(parsed)
  }
  
  keysToProcess.forEach(key => {
    let valBefore = null
    let valAfter = null
    if (targetType === 'RECORD_UPDATE') {
      valBefore = (parsed.before || {})[key]
      valAfter = (parsed.after || {})[key]
    } else {
      valAfter = parsed[key]
    }
    
    const f = Object.values(fieldNameMap.value || {}).find(field => String(field.key).toUpperCase() === key) || { name: key, fieldGroup: null }
    
    const parseName = (nameObj) => {
      if (!nameObj) return null;
      if (typeof nameObj === 'string') { try { return JSON.parse(nameObj) } catch(e){ return null } }
      return nameObj
    }
    const translate = (nameObj, defaultKo, defaultEn) => {
      const p = parseName(nameObj)
      if (!p) return currentLocale.value === 'ko' ? defaultKo : defaultEn
      return p[currentLocale.value] || p.ko || p.en || (currentLocale.value === 'ko' ? defaultKo : defaultEn)
    }
    
    const sObj = f.fieldGroup?.sector
    const gObj = f.fieldGroup

    const sName = translate(sObj?.name, '?쇰컲', 'General')
    const sKey = sObj?.id || 'default'
    const sOrder = sObj?.sortOrder || 0
    
    const gName = translate(gObj?.name, '?꾨뱶', 'Fields')
    const gKey = gObj?.id || 'default'
    const gOrder = gObj?.sortOrder || 0
    
    if (!map.has(sKey)) {
      map.set(sKey, { key: sKey, label: sName, order: sOrder, groups: new Map() })
    }
    const sectorObj = map.get(sKey)
    
    if (!sectorObj.groups.has(gKey)) {
      sectorObj.groups.set(gKey, { key: gKey, label: gName, order: gOrder, fields: [] })
    }
    
    let displayValBefore = valBefore;
    let displayValAfter = valAfter;
    
    if (f.type === 'DOMAIN_REFERENCE') {
      let tDomainId = null
      try { tDomainId = JSON.parse(f.options || '{}').targetDomainId } catch(e){}
      if (targetType === 'RECORD_UPDATE') {
        if (valBefore && !domainRefDisplayMap.value[valBefore]) fetchDomainRefName(valBefore, tDomainId);
        if (valAfter && !domainRefDisplayMap.value[valAfter]) fetchDomainRefName(valAfter, tDomainId);
        displayValBefore = domainRefDisplayMap.value[valBefore] || valBefore;
        displayValAfter = domainRefDisplayMap.value[valAfter] || valAfter;
      } else {
        if (valAfter && !domainRefDisplayMap.value[valAfter]) fetchDomainRefName(valAfter, tDomainId);
        displayValAfter = domainRefDisplayMap.value[valAfter] || valAfter;
      }
    } else if (f.type === 'MULTILINGUAL') {
      try {
        const parseMultilingual = (val) => {
          if (!val) return val;
          let obj = val;
          if (typeof val === 'string') {
            try {
              obj = JSON.parse(val);
            } catch (e) {
              return val;
            }
          }
          if (typeof obj === 'object') {
            const isEmpty = Object.values(obj).every(v => !v || String(v).trim() === '');
            if (isEmpty) return '-';
            const koStr = obj.ko ? `[KR] ${obj.ko}` : '';
            const enStr = obj.en ? `[EN] ${obj.en}` : '';
            if (koStr && enStr) return `${koStr} / ${enStr}`;
            return koStr || enStr || JSON.stringify(obj);
          }
          return val;
        };

        if (targetType === 'RECORD_UPDATE') {
          displayValBefore = parseMultilingual(valBefore);
          displayValAfter = parseMultilingual(valAfter);
        } else {
          displayValAfter = parseMultilingual(valAfter);
        }
      } catch (e) {}
    } else if (['SELECT', 'MULTI_SELECT'].includes(f.type) && f.options) {
      try {
        const opts = JSON.parse(f.options);
        const mapVal = (v) => {
          if (!v) return v;
          const found = opts.find(o => o.key === v);
          if (found && found.label) {
            return found.label[currentLocale.value] || found.label.ko || found.label.en || v;
          }
          return v;
        };
        
        if (targetType === 'RECORD_UPDATE') {
          if (Array.isArray(valBefore)) displayValBefore = valBefore.map(mapVal).join(', ');
          else displayValBefore = mapVal(valBefore);
          
          if (Array.isArray(valAfter)) displayValAfter = valAfter.map(mapVal).join(', ');
          else displayValAfter = mapVal(valAfter);
        } else {
          if (Array.isArray(valAfter)) displayValAfter = valAfter.map(mapVal).join(', ');
          else displayValAfter = mapVal(valAfter);
        }
      } catch(e) {}
    }
    
    if (typeof displayValBefore === 'string') {
      const uName = getUserName(displayValBefore);
      if (uName && uName !== displayValBefore) displayValBefore = uName;
    }
    if (typeof displayValAfter === 'string') {
      const uName = getUserName(displayValAfter);
      if (uName && uName !== displayValAfter) displayValAfter = uName;
    }
    
    if (f && ['NUMBER', 'INTEGER', 'DECIMAL', 'CALCULATED'].includes(f.type)) {
      if (displayValBefore !== null && displayValBefore !== undefined && displayValBefore !== '' && displayValBefore !== '-') {
        const numBefore = Number(displayValBefore);
        if (!isNaN(numBefore)) {
          displayValBefore = numBefore.toLocaleString('ko-KR');
          if (f.unit) displayValBefore += ` ${f.unit}`;
        }
      }
      if (displayValAfter !== null && displayValAfter !== undefined && displayValAfter !== '' && displayValAfter !== '-') {
        const numAfter = Number(displayValAfter);
        if (!isNaN(numAfter)) {
          displayValAfter = numAfter.toLocaleString('ko-KR');
          if (f.unit) displayValAfter += ` ${f.unit}`;
        }
      }
    }

    let finalVal = null;
    if (targetType === 'RECORD_UPDATE') {
      const vBefore = displayValBefore || '-';
      const vAfter = displayValAfter || '-';
      finalVal = {
        before: vBefore,
        after: vAfter,
        isChanged: String(vBefore) !== String(vAfter)
      }
    } else {
      finalVal = displayValAfter || '-'
    }
    
    sectorObj.groups.get(gKey).fields.push({ key, label: translate(f.name, key, key), val: finalVal, gridWidth: f.gridWidth, type: f.type })
  })
  
  const sectorsArray = Array.from(map.values())
  let sectors = sectorsArray
  
  if (targetType === 'RECORD_UPDATE') {
    sectors.forEach(s => {
      s.groups.forEach(g => {
        g.fields = g.fields.filter(f => f.val && f.val.isChanged)
      })
      Array.from(s.groups.keys()).forEach(k => {
        if (s.groups.get(k).fields.length === 0) {
          s.groups.delete(k)
        }
      })
    })
    sectors = sectors.filter(s => s.groups.size > 0)
  }

  sectors.sort((a, b) => a.order - b.order)
  
  return sectors.map(s => {
    const groups = Array.from(s.groups.values())
    groups.sort((a, b) => a.order - b.order)
    return {
      key: s.key,
      label: s.label,
      groups: groups
    }
  })
}

const getGroupedSteps = (request) => {
  if (!request) return []
  const steps = request.steps || []
  const map = new Map()

  const hasDraftStep = steps.some(s => s.stepOrder === 0 || s.stepType === 'DRAFT')
  if (!hasDraftStep && request.requesterId) {
    map.set(0, {
      order: 0,
      steps: [{
        id: 'draft-step-' + request.id,
        stepType: 'DRAFT',
        stepOrder: 0,
        assigneeId: request.requesterId,
        status: 'SUBMITTED',
        updatedAt: request.createdAt,
        comment: '' 
      }]
    })
  }

  if (steps.length > 0) {
    steps.forEach(s => {
      if (!map.has(s.stepOrder)) {
        map.set(s.stepOrder, { order: s.stepOrder, steps: [] })
      }
      map.get(s.stepOrder).steps.push(s)
    })
  }
  
  const result = Array.from(map.values())
  result.sort((a, b) => a.order - b.order)
  return result
}

const formatDate = (dateString) => {
  if (!dateString) return ''
  const date = parseDate(dateString)
  if (!date) return ''
  const tz = useCookie('timezone', { default: () => 'Asia/Seoul' }).value
  const formatted = date.toLocaleString(undefined, { timeZone: tz })
  return formatted.replace(/\s*(GMT|UTC|KST|PST|EST|CET)[-+0-9:]*/gi, '').trim()
}

const parseDate = (dateString) => {
  if (!dateString) return null
  let str = String(dateString).trim()
  if (/^\d+$/.test(str)) {
    return new Date(parseInt(str, 10))
  }
  if (!str.endsWith('Z') && !str.includes('+') && !/[-+]\d{2}:\d{2}$/.test(str)) {
    if (str.includes(' ') && !str.includes('T')) {
      str = str.replace(' ', 'T')
    }
    const serverOffset = useCookie('server_offset', { default: () => '+09:00' }).value
    str += serverOffset
  }
  const d = new Date(str)
  return isNaN(d.getTime()) ? new Date(dateString) : d
}

const formatStepDate = (dateString) => {
  if (!dateString) return '';
  const date = parseDate(dateString);
  if (!date) return '';
  const tz = useCookie('timezone', { default: () => 'Asia/Seoul' }).value;
  try {
    const formatter = new Intl.DateTimeFormat('ko-KR', {
      timeZone: tz,
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      hour12: false
    });
    const parts = formatter.formatToParts(date);
    const getPart = (type) => parts.find(p => p.type === type).value;

    const tzFormatter = new Intl.DateTimeFormat('en-US', {
      timeZone: tz,
      year: 'numeric', month: 'numeric', day: 'numeric',
      hour: 'numeric', minute: 'numeric', hour12: false
    });
    const tzParts = tzFormatter.formatToParts(date);
    const getTzVal = (type) => parseInt(tzParts.find(p => p.type === type).value, 10);

    const utcFormatter = new Intl.DateTimeFormat('en-US', {
      timeZone: 'UTC',
      year: 'numeric', month: 'numeric', day: 'numeric',
      hour: 'numeric', minute: 'numeric', hour12: false
    });
    const utcParts = utcFormatter.formatToParts(date);
    const getUtcVal = (type) => parseInt(utcParts.find(p => p.type === type).value, 10);

    const tzDate = new Date(Date.UTC(getTzVal('year'), getTzVal('month') - 1, getTzVal('day'), getTzVal('hour'), getTzVal('minute')));
    const utcDate = new Date(Date.UTC(getUtcVal('year'), getUtcVal('month') - 1, getUtcVal('day'), getUtcVal('hour'), getUtcVal('minute')));

    const diffMs = tzDate.getTime() - utcDate.getTime();
    const diffHours = diffMs / (1000 * 60 * 60);
    const sign = diffHours >= 0 ? '+' : '-';
    const absHours = Math.abs(diffHours);
    const hours = Math.floor(absHours);
    const offsetStr = `GMT${sign}${hours}`;

    return `${getPart('month')}/${getPart('day')} ${getPart('hour')}:${getPart('minute')}:${getPart('second')}`;
  } catch (e) {
    const pad = (n) => n.toString().padStart(2, '0');
    const month = pad(date.getMonth() + 1);
    const day = pad(date.getDate());
    const hours = pad(date.getHours());
    const minutes = pad(date.getMinutes());
    const seconds = pad(date.getSeconds());
    return `${month}/${day} ${hours}:${minutes}:${seconds}`;
  }
}

const getStepperSteps = (req) => {
  if (!req || !req.steps || req.steps.length === 0) return [];
  const sortedSteps = [...req.steps].sort((a, b) => a.stepOrder - b.stepOrder);
  const result = sortedSteps.map(s => {
    const name = getUserName(s.assigneeId);
    let statusText = '';
    if (s.stepType === 'DRAFT') statusText = t('stepDraft');
    else if (s.status === 'APPROVED') statusText = t('stepApproved');
    else if (s.status === 'REJECTED') statusText = t('stepRejected');
    else if (s.status === 'PENDING') statusText = t('stepPending');
    else statusText = t('stepScheduled');
    
    // DRAFT, APPROVED, REJECTED 완료 시점에 날짜 표시
    const isCompleted = s.stepType === 'DRAFT' || s.status === 'APPROVED' || s.status === 'REJECTED';
    const processedDate = isCompleted ? formatStepDate(s.updatedAt) : '';

    return {
      stepOrder: s.stepOrder,
      name: name,
      statusText: statusText,
      hasError: s.status === 'REJECTED',
      isPending: s.status === 'PENDING',
      processedDate: processedDate
    };
  });
  
  const isAllApproved = req.status === 'APPROVED';
  const isRejected = req.status === 'REJECTED';
  const isFinalized = isAllApproved || isRejected;
  
  let systemStatusText = '';
  if (isAllApproved) {
    systemStatusText = t('systemComplete');
  } else if (isRejected) {
    systemStatusText = t('systemCancelled');
  } else {
    systemStatusText = t('stepScheduled');
  }

  result.push({
    stepOrder: result.length > 0 ? result[result.length - 1].stepOrder + 1 : 1,
    name: t('systemApplied'),
    statusText: systemStatusText,
    hasError: false,
    isPending: false,
    processedDate: isFinalized ? formatStepDate(req.updatedAt) : ''
  });
  
  return result;
}

const getCurrentStepIndex = (req) => {
  if (!req || !req.steps || req.steps.length === 0) return 0;
  if (req.status === 'APPROVED') return req.steps.length + 1;
  const sortedSteps = [...req.steps].sort((a, b) => a.stepOrder - b.stepOrder);
  let currentIndex = sortedSteps.findIndex(s => s.status === 'PENDING');
  if (currentIndex === -1) {
    currentIndex = sortedSteps.findIndex(s => s.status === 'REJECTED');
    if (currentIndex === -1) {
      currentIndex = sortedSteps.length - 1; 
    }
  }
  return currentIndex;
}

const getObserversList = (obsString) => {
  if (!obsString) return []
  try {
    const parsed = JSON.parse(obsString)
    return Array.isArray(parsed) ? parsed : []
  } catch (e) {
    return []
  }
}

</script>

<style scoped>
.custom-scrollbar::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}
.custom-scrollbar::-webkit-scrollbar-track {
  background: var(--va-background-primary); 
  border-radius: 4px;
}
.custom-scrollbar::-webkit-scrollbar-thumb {
  background: var(--va-background-border); 
  border-radius: 4px;
}
.custom-scrollbar::-webkit-scrollbar-thumb:hover {
  background: var(--va-secondary); 
}
.step-flash {
  animation: pulse-border 2s infinite;
}
@keyframes pulse-border {
  0% { box-shadow: 0 0 0 0 rgba(255, 212, 58, 0.7); }
  70% { box-shadow: 0 0 0 6px rgba(255, 212, 58, 0); }
  100% { box-shadow: 0 0 0 0 rgba(255, 212, 58, 0); }
}
</style>

<i18n lang="json">
{
  "ko": {
    "requestedData": "요청 데이터",
    "noParsable": "파싱 가능한 데이터가 없습니다.",
    "approvalLineSummary": "결재라인 (요약):",
    "noApprovalLine": "결재라인이 없습니다.",
    "approvalLineStatus": "결재선 현황",
    "stepDraft": "상신완료",
    "stepApproved": "승인됨",
    "stepRejected": "반려됨",
    "stepPending": "대기중",
    "stepScheduled": "예정",
    "systemApplied": "시스템 반영",
    "systemComplete": "완료",
    "systemCancelled": "취소됨",
    "typeConsensus": "합의",
    "typeDraft": "기안",
    "typeApproval": "결재",
    "modified": "수정됨",
    "processed": "처리됨",
    "noComment": "의견 없음",
    "observers": "참조자(CC)"
  },
  "en": {
    "requestedData": "Requested Data:",
    "noParsable": "No parsable data provided.",
    "approvalLineSummary": "Approval Line (Summary):",
    "noApprovalLine": "No approval line.",
    "approvalLineStatus": "Approval Line Status",
    "stepDraft": "Submitted",
    "stepApproved": "Approved",
    "stepRejected": "Rejected",
    "stepPending": "Pending",
    "stepScheduled": "Scheduled",
    "systemApplied": "System Reflect",
    "systemComplete": "Complete",
    "systemCancelled": "Cancelled",
    "typeConsensus": "Consensus",
    "typeDraft": "Draft",
    "typeApproval": "Approval",
    "modified": "Modified",
    "processed": "Processed",
    "noComment": "No comment",
    "observers": "Observers (CC)"
  }
}
</i18n>
