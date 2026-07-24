import { defineNuxtPlugin } from '#app'
import { usePermission } from '~/composables/usePermission'

export default defineNuxtPlugin((nuxtApp) => {
  const { hasPermission, hasAnyPermission } = usePermission()

  const checkPermission = (el: HTMLElement, binding: any) => {
    const value = binding.value

    if (!value) return

    let allowed = false
    if (typeof value === 'string') {
      allowed = hasPermission(value)
    } else if (Array.isArray(value)) {
      allowed = hasAnyPermission(value)
    }

    if (!allowed) {
      // DOM 요소를 숨기거나 제거
      el.style.display = 'none'
      el.setAttribute('aria-hidden', 'true')
    } else {
      el.style.display = ''
      el.removeAttribute('aria-hidden')
    }
  }

  nuxtApp.vueApp.directive('permission', {
    mounted(el, binding) {
      checkPermission(el, binding)
    },
    updated(el, binding) {
      checkPermission(el, binding)
    }
  })
})
