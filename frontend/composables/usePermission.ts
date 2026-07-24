import { computed } from 'vue'
import { useCookie } from '#app'

export interface UserPermissionData {
  username?: string
  role?: string
  permissions?: string[]
  [key: string]: any
}

export function usePermission() {
  const userCookie = useCookie<any>('user_data')
  const permissionsCookie = useCookie<string[]>('user_permissions')

  // 사용자의 세부 권한 목록 (Normalized uppercase/lowercase safe)
  const userPermissions = computed<string[]>(() => {
    let list: string[] = []

    if (permissionsCookie.value && Array.isArray(permissionsCookie.value)) {
      list = permissionsCookie.value
    } else if (userCookie.value) {
      try {
        const data: UserPermissionData = typeof userCookie.value === 'string'
          ? JSON.parse(userCookie.value)
          : userCookie.value
        if (Array.isArray(data?.permissions)) {
          list = data.permissions
        }
      } catch {
        list = []
      }
    }

    return list.map(p => (p || '').trim().toLowerCase())
  })

  /**
   * 와일드카드(*)를 포함한 세부 권한 매칭 함수
   * @param requiredPermission 요청 권한 코드 (예: 'record:read', 'record:write', 'domain:*')
   * @returns boolean 권한 소유 여부
   */
  const hasPermission = (requiredPermission: string): boolean => {
    if (!requiredPermission || typeof requiredPermission !== 'string') {
      return false
    }

    const currentPermissions = userPermissions.value
    const target = requiredPermission.trim().toLowerCase()

    // 1. 전역 와일드카드 (*:*, *) 체크
    if (
      currentPermissions.includes('*:*') ||
      currentPermissions.includes('*')
    ) {
      return true
    }

    // 2. 정확한 일치 (예: 'record:read')
    if (currentPermissions.includes(target)) {
      return true
    }

    // 3. 리소스 단위 와일드카드 매칭 (예: target이 'record:write' 일 때 'record:*' 보유 여부)
    if (target.includes(':')) {
      const resource = target.split(':')[0]
      const wildcard = `${resource}:*`
      if (currentPermissions.includes(wildcard)) {
        return true
      }
    }

    return false
  }

  /**
   - 배열 내 권한 중 하나라도 보유 시 true
   */
  const hasAnyPermission = (permissions: string[]): boolean => {
    if (!Array.isArray(permissions) || permissions.length === 0) {
      return false
    }
    return permissions.some(p => hasPermission(p))
  }

  /**
   - 배열 내 모든 권한 보유 시 true
   */
  const hasAllPermissions = (permissions: string[]): boolean => {
    if (!Array.isArray(permissions) || permissions.length === 0) {
      return false
    }
    return permissions.every(p => hasPermission(p))
  }

  return {
    userPermissions,
    hasPermission,
    hasAnyPermission,
    hasAllPermissions
  }
}
