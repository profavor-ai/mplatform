import { ref, computed } from 'vue'
import { useCookie } from '#app'

export interface RoleInfo {
  id?: string
  organizationId?: string
  name: string
  displayName?: string
  description?: string
  isSystemRole?: boolean
}

// Global cached role map by code (name)
const roleMap = ref<Record<string, RoleInfo>>({})
const isFetched = ref(false)

const defaultFallbackMap: Record<string, string> = {
  ADMIN: '시스템 관리자 (System Admin)',
  ROLE_ADMIN: '시스템 관리자 (System Admin)',
  ORG_ADMIN: '조직 관리자 (Org Admin)',
  DATA_STEWARD: '데이터 스튜어드 (Data Steward)',
  DOMAIN_EDITOR: '도메인 편집자 (Domain Editor)',
  DQ_MANAGER: '데이터 품질 관리자 (DQ Manager)',
  VIEWER: '조회자 (Viewer)',
  USER: '일반 사용자 (General User)',
  ROLE_USER: '일반 사용자 (General User)'
}

export function useRoles() {
  const token = useCookie('auth_token')

  const fetchRoles = async (orgId?: string) => {
    try {
      const endpoint = orgId ? `/api/roles/org/${orgId}` : '/api/roles'
      const headers = token.value ? { Authorization: `Bearer ${token.value}` } : {}
      const list = await $fetch<RoleInfo[]>(endpoint, { headers })
      
      if (Array.isArray(list)) {
        list.forEach(r => {
          if (r && r.name) {
            roleMap.value[r.name] = r
          }
        })
        isFetched.value = true
      }
    } catch (e) {
      console.warn('Failed to fetch roles from DB API:', e)
    }
  }

  const getRoleDisplayName = (code: string): string => {
    if (!code) return ''
    const cleanCode = code.startsWith('ROLE_') ? code.replace('ROLE_', '') : code
    
    // 1. Check DB cached roleMap
    if (roleMap.value[code]?.displayName) {
      return roleMap.value[code].displayName!
    }
    if (roleMap.value[cleanCode]?.displayName) {
      return roleMap.value[cleanCode].displayName!
    }

    // 2. Fallback to default name if DB map has not loaded this role yet
    if (defaultFallbackMap[code]) return defaultFallbackMap[code]
    if (defaultFallbackMap[cleanCode]) return defaultFallbackMap[cleanCode]

    return code
  }

  const formatRoleText = (code: string): string => {
    if (!code) return ''
    const disp = getRoleDisplayName(code)
    if (disp && disp !== code && !disp.startsWith(code)) {
      return `${code} (${disp})`
    }
    return disp || code
  }

  return {
    roleMap,
    isFetched,
    fetchRoles,
    getRoleDisplayName,
    formatRoleText
  }
}
