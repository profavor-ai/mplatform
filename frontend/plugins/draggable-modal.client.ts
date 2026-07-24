import { defineNuxtPlugin } from '#app'

export default defineNuxtPlugin((nuxtApp) => {
  nuxtApp.vueApp.directive('draggable-modal', {
    mounted(el) {
      const setupDrag = () => {
        // Find dialog element inside teleported container or current element
        const dialog = (el.querySelector('.va-modal__dialog') ||
                       el.querySelector('.va-modal__container') ||
                       el) as HTMLElement

        if (!dialog || (dialog as any)._hasDragBinding) return

        let handle = (dialog.querySelector('.va-modal__header') ||
                     dialog.querySelector('.va-modal__title') ||
                     dialog.firstElementChild ||
                     dialog) as HTMLElement

        if (!handle) return

        (dialog as any)._hasDragBinding = true
        handle.style.cursor = 'grab'
        handle.style.userSelect = 'none'
        handle.title = '드래그하여 모달 창 이동'

        let isDragging = false
        let startX = 0
        let startY = 0
        let initialX = 0
        let initialY = 0

        const getTransformValues = () => {
          const style = window.getComputedStyle(dialog)
          const matrix = style.transform
          if (matrix === 'none' || !matrix) {
            return { x: 0, y: 0 }
          }
          const values = matrix.match(/matrix.*\((.+)\)/)?.[1].split(', ')
          if (values && values.length >= 6) {
            return { x: parseFloat(values[4]), y: parseFloat(values[5]) }
          }
          return { x: 0, y: 0 }
        }

        const onMouseDown = (e: MouseEvent) => {
          const targetNode = e.target as HTMLElement
          if (targetNode && (targetNode.tagName === 'BUTTON' || targetNode.tagName === 'INPUT' || targetNode.closest('button') || targetNode.closest('.va-button'))) {
            return
          }

          isDragging = true
          handle.style.cursor = 'grabbing'
          
          startX = e.clientX
          startY = e.clientY

          const current = getTransformValues()
          initialX = current.x
          initialY = current.y

          window.addEventListener('mousemove', onMouseMove)
          window.addEventListener('mouseup', onMouseUp)
        }

        const onMouseMove = (e: MouseEvent) => {
          if (!isDragging) return
          const dx = e.clientX - startX
          const dy = e.clientY - startY

          const newX = initialX + dx
          const newY = initialY + dy

          dialog.style.transform = `translate3d(${newX}px, ${newY}px, 0px)`
        }

        const onMouseUp = () => {
          isDragging = false
          handle.style.cursor = 'grab'
          window.removeEventListener('mousemove', onMouseMove)
          window.removeEventListener('mouseup', onMouseUp)
        }

        handle.addEventListener('mousedown', onMouseDown)

        ;(el as any)._draggableCleanup = () => {
          handle.removeEventListener('mousedown', onMouseDown)
          window.removeEventListener('mousemove', onMouseMove)
          window.removeEventListener('mouseup', onMouseUp)
        }
      }

      // Check repeatedly for dynamic/teleported modal DOM appearance
      setupDrag()
      const timer = setInterval(setupDrag, 250)
      ;(el as any)._draggableTimer = timer
    },
    unmounted(el) {
      if ((el as any)._draggableTimer) {
        clearInterval((el as any)._draggableTimer)
      }
      if ((el as any)._draggableCleanup) {
        ;(el as any)._draggableCleanup()
      }
    }
  })
})
