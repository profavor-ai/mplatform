import { ref } from 'vue'
import { useCookie } from '#app'
import { getMultilingualText } from '~/utils/multilingual'

export interface RoleInfo {
  id?: string
  organizationId?: string
  name: string
  displayName?: string
  description?: string
  isSystemRole?: boolean
}

// Cache role lists by organization ID
const orgRolesMap = ref<Record<string, RoleInfo[]>>({})
// Global role lookup map for code -> RoleInfo
const globalRoleLookupMap = ref<Record<string, RoleInfo>>({})

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

  const fetchRolesForOrg = async (orgId?: string | null, forceRefresh = false): Promise<RoleInfo[]> => {
    const targetOrgId = orgId || getUserOrgId()
    const cacheKey = targetOrgId || 'GLOBAL'

    if (!forceRefresh && orgRolesMap.value[cacheKey]) {
      return orgRolesMap.value[cacheKey]
    }

    try {
      const endpoint = targetOrgId ? `/api/roles/org/${targetOrgId}` : '/api/roles'
      const headers = token.value ? { Authorization: `Bearer ${token.value}` } : {}
      const list = await $fetch<RoleInfo[]>(endpoint, { headers })

      if (Array.isArray(list)) {
        orgRolesMap.value[cacheKey] = list
        list.forEach(r => {
          if (r && r.name) {
            globalRoleLookupMap.value[r.name] = r
          }
        })
        return list
      }
    } catch (e) {
      console.error(`Failed to fetch roles for org (${cacheKey}):`, e)
    }

    return orgRolesMap.value[cacheKey] || []
  }

  const getRoleDisplayName = (code: string): string => {
    if (!code) return ''
    const cleanCode = code.startsWith('ROLE_') ? code.replace('ROLE_', '') : code

    const role = globalRoleLookupMap.value[code] || globalRoleLookupMap.value[cleanCode]
    if (role && role.displayName) {
      return getMultilingualText(role.displayName)
    }
    return code
  }

  const formatRoleText = (code: string): string => {
    if (!code) return ''
    const rawDisp = getRoleDisplayName(code)
    const disp = getMultilingualText(rawDisp)
    if (disp && disp !== code && !disp.startsWith(code)) {
      return `${code} (${disp})`
    }
    return disp || code
  }

  return {
    orgRolesMap,
    globalRoleLookupMap,
    fetchRolesForOrg,
    getRoleDisplayName,
    formatRoleText,
    getUserOrgId
  }
}
