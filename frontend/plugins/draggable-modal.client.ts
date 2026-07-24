import { defineNuxtPlugin } from '#app'

export default defineNuxtPlugin((nuxtApp) => {
  nuxtApp.vueApp.directive('draggable-modal', {
    mounted(el) {
      // Find the actual modal dialog element inside va-modal wrapper
      const findTarget = () => {
        return el.querySelector('.va-modal__dialog') || el.querySelector('.va-modal__container') || el
      }

      setTimeout(() => {
        const dialog = findTarget() as HTMLElement
        if (!dialog) return

        let handle = dialog.querySelector('.va-modal__header') as HTMLElement || 
                     dialog.querySelector('.va-modal__title') as HTMLElement ||
                     dialog.firstElementChild as HTMLElement || 
                     dialog

        if (!handle) return

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
          // Ignore clicks on buttons, inputs, or close icons inside header
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

        // Store cleanup on element for unmounted hook
        ;(el as any)._draggableCleanup = () => {
          handle.removeEventListener('mousedown', onMouseDown)
          window.removeEventListener('mousemove', onMouseMove)
          window.removeEventListener('mouseup', onMouseUp)
        }
      }, 100)
    },
    unmounted(el) {
      if ((el as any)._draggableCleanup) {
        ;(el as any)._draggableCleanup()
      }
    }
  })
})
