import { ref } from 'vue'

const menus = ref([])

export const useMenu = () => {
  const fetchMenus = async () => {
    try {
      const token = useCookie('auth_token')
      const response = await $fetch('/api/menus/tree', {
        headers: token.value ? { Authorization: `Bearer ${token.value}` } : {}
      })
      menus.value = response || []
    } catch (error) {
      console.error('Failed to fetch menus:', error)
      menus.value = []
    }
  }

  const logAccess = async (menuPath) => {
    try {
      // Find menuId based on path, if available in the current tree
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
    logAccess
  }
}
