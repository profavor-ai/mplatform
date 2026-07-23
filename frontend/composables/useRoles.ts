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

// Global cached role maps & lists
const roleList = ref<RoleInfo[]>([])
const roleMap = ref<Record<string, RoleInfo>>({})
const isFetched = ref(false)
const currentLoadedOrgId = ref<string | null>(null)

export function useRoles() {
  const token = useCookie('auth_token')
  const userCookie = useCookie<any>('user_data')

  const getUserOrgId = (): string | null => {
    if (!userCookie.value) return null
    try {
      const data = typeof userCookie.value === 'string' ? JSON.parse(userCookie.value) : userCookie.value
      return data?.organizationId || null
    } catch {
      return null
    }
  }

  const fetchRoles = async (orgId?: string, forceRefresh = false) => {
    const targetOrgId = orgId || getUserOrgId()

    // Skip if already fetched for the same orgId unless forced
    if (isFetched.value && !forceRefresh && currentLoadedOrgId.value === (targetOrgId || 'ALL')) {
      return
    }

    try {
      const endpoint = targetOrgId ? `/api/roles/org/${targetOrgId}` : '/api/roles'
      const headers = token.value ? { Authorization: `Bearer ${token.value}` } : {}
      const list = await $fetch<RoleInfo[]>(endpoint, { headers })

      if (Array.isArray(list)) {
        roleList.value = list
        const map: Record<string, RoleInfo> = {}
        list.forEach(r => {
          if (r && r.name) {
            map[r.name] = r
          }
        })
        roleMap.value = map
        isFetched.value = true
        currentLoadedOrgId.value = targetOrgId || 'ALL'
      }
    } catch (e) {
      console.error('Failed to fetch DB roles:', e)
    }
  }

  const getRoleDisplayName = (code: string): string => {
    if (!code) return ''
    const cleanCode = code.startsWith('ROLE_') ? code.replace('ROLE_', '') : code

    const role = roleMap.value[code] || roleMap.value[cleanCode]
    if (role && role.displayName) {
      return role.displayName
    }
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
    roleList,
    roleMap,
    isFetched,
    fetchRoles,
    getRoleDisplayName,
    formatRoleText,
    getUserOrgId
  }
}
