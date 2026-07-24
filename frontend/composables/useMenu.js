import { ref } from 'vue'
import { useCookie } from '#app'

const menus = ref([])
let fetchPromise = null

export const useMenu = () => {
  const fetchMenus = async (forceRefresh = false) => {
    if (fetchPromise && !forceRefresh) {
      await fetchPromise
      return menus.value
    }
    fetchPromise = (async () => {
      try {
        const token = useCookie('auth_token')
        const response = await $fetch('/api/menus/tree', {
          headers: token.value ? { Authorization: `Bearer ${token.value}` } : {}
        })
        menus.value = response || []
      } catch (error) {
        console.error('Failed to fetch menus:', error)
        menus.value = []
        fetchPromise = null
      }
    })()
    await fetchPromise
    return menus.value
  }

  const fetchMenuTree = fetchMenus

  const logAccess = async (menuPath) => {
    try {
      let menuId = null
      
      const findIdByPath = (items) => {
        for (const item of items) {
          if (item.path === menuPath) return item.id
          if (item.children) {
            const childId = findIdByPath(item.children)
            if (childId) return childId
          }
        }
        return null
      }
      
      menuId = findIdByPath(menus.value)

      const token = useCookie('auth_token')
      await $fetch('/api/menus/access', {
        method: 'POST',
        headers: token.value ? { Authorization: `Bearer ${token.value}` } : {},
        body: { menuId, menuPath }
      })
    } catch (error) {
      console.error('Failed to log menu access:', error)
    }
  }

  return {
    menus,
    fetchMenus,
    fetchMenuTree,
    logAccess
  }
}
