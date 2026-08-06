import { onBeforeUnmount, onMounted } from 'vue'
import { onBeforeRouteLeave } from 'vue-router'
import { showConfirm } from './useConfirm'

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

  onBeforeRouteLeave(() => {
    if (!isDirty()) return true
    return showConfirm({
      title: '放弃未保存内容？',
      message,
      confirmLabel: '离开',
      danger: true,
    })
  })
}

export async function confirmDiscardDraft(message = DEFAULT_MESSAGE): Promise<boolean> {
  return showConfirm({
    title: '放弃未保存内容？',
    message,
    confirmLabel: '放弃',
    danger: true,
  })
}
