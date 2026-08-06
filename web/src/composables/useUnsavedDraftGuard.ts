import { onBeforeUnmount, onMounted } from 'vue'
import { onBeforeRouteLeave } from 'vue-router'

const DEFAULT_MESSAGE = '你有未保存的食材填写内容，离开或刷新页面后将会丢失。'

export function useUnsavedDraftGuard(isDirty: () => boolean, message = DEFAULT_MESSAGE) {
  function beforeUnload(event: BeforeUnloadEvent) {
    if (!isDirty()) return
    event.preventDefault()
    event.returnValue = message
  }

  onMounted(() => {
    window.addEventListener('beforeunload', beforeUnload)
  })

  onBeforeUnmount(() => {
    window.removeEventListener('beforeunload', beforeUnload)
  })

  onBeforeRouteLeave((_to, _from, next) => {
    if (!isDirty()) {
      next()
      return
    }
    if (window.confirm(message)) next()
    else next(false)
  })
}

export function confirmDiscardDraft(message = DEFAULT_MESSAGE): boolean {
  return window.confirm(message)
}
