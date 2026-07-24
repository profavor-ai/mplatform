import { defineNuxtPlugin } from '#app'

export default defineNuxtPlugin(() => {
  if (typeof window === 'undefined') return

  let isDragging = false
  let currentDialog: HTMLElement | null = null
  let currentHandle: HTMLElement | null = null
  let startX = 0
  let startY = 0
  let initialX = 0
  let initialY = 0

  const getTransformValues = (element: HTMLElement) => {
    const style = window.getComputedStyle(element)
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

  window.addEventListener('mousedown', (e: MouseEvent) => {
    const targetNode = e.target as HTMLElement
    if (!targetNode) return

    const dialog = targetNode.closest('.va-modal__dialog, .va-modal__container') as HTMLElement
    if (!dialog) return

    const handle = targetNode.closest('.va-modal__header, .va-modal__title') as HTMLElement ||
                   (dialog.firstElementChild as HTMLElement)

    if (!handle || !handle.contains(targetNode)) return

    if (targetNode.tagName === 'BUTTON' || targetNode.tagName === 'INPUT' || targetNode.closest('button') || targetNode.closest('.va-button')) {
      return
    }

    isDragging = true
    currentDialog = dialog
    currentHandle = handle

    handle.style.cursor = 'grabbing'
    handle.style.userSelect = 'none'

    startX = e.clientX
    startY = e.clientY

    const current = getTransformValues(dialog)
    initialX = current.x
    initialY = current.y
  })

  window.addEventListener('mousemove', (e: MouseEvent) => {
    if (!isDragging || !currentDialog) return

    const dx = e.clientX - startX
    const dy = e.clientY - startY

    const newX = initialX + dx
    const newY = initialY + dy

    currentDialog.style.transform = `translate3d(${newX}px, ${newY}px, 0px)`
  })

  window.addEventListener('mouseup', () => {
    if (isDragging && currentHandle) {
      currentHandle.style.cursor = 'grab'
    }
    isDragging = false
    currentDialog = null
    currentHandle = null
  })
})
