<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const message = ref('')
const visible = ref(false)
let hideTimer: ReturnType<typeof setTimeout> | undefined

function show(text: string) {
  message.value = text
  visible.value = true
  if (hideTimer) clearTimeout(hideTimer)
  hideTimer = setTimeout(() => {
    visible.value = false
  }, 3200)
}

function onUnauthorized() {
  show('登录已失效，请重新登录')
}

function onForbidden(event: Event) {
  const detail = (event as CustomEvent<string>).detail
  show(detail || '无权访问该资源')
}

function onLoginRequired(event: Event) {
  const detail = (event as CustomEvent<string>).detail
  show(detail || '请先登录')
}

function onToast(event: Event) {
  const detail = (event as CustomEvent<string>).detail
  if (detail) show(detail)
}

onMounted(() => {
  window.addEventListener('fridgeclear:unauthorized', onUnauthorized)
  window.addEventListener('fridgeclear:forbidden', onForbidden)
  window.addEventListener('fridgeclear:login-required', onLoginRequired)
  window.addEventListener('fridgeclear:toast', onToast)
})

onUnmounted(() => {
  window.removeEventListener('fridgeclear:unauthorized', onUnauthorized)
  window.removeEventListener('fridgeclear:forbidden', onForbidden)
  window.removeEventListener('fridgeclear:login-required', onLoginRequired)
  window.removeEventListener('fridgeclear:toast', onToast)
  if (hideTimer) clearTimeout(hideTimer)
})
</script>

<template>
  <Transition name="toast-fade">
    <div v-if="visible" class="app-toast" role="status">
      <span>{{ message }}</span>
      <button
        v-if="message.includes('登录')"
        type="button"
        class="app-toast-action"
        @click="router.push('/login'); visible = false"
      >
        去登录
      </button>
    </div>
  </Transition>
</template>

<style scoped>
.app-toast {
  position: fixed;
  left: 50%;
  bottom: calc(24px + env(safe-area-inset-bottom, 0px));
  transform: translateX(-50%);
  z-index: 100;
  display: flex;
  align-items: center;
  gap: 12px;
  max-width: min(92vw, 420px);
  padding: 12px 16px;
  border-radius: 12px;
  background: var(--deep-green);
  color: var(--white);
  font-size: 14px;
  font-weight: 500;
  box-shadow: var(--shadow-lg);
}

.app-toast-action {
  border: 0;
  background: rgba(255, 255, 255, 0.18);
  color: var(--white);
  border-radius: 8px;
  padding: 6px 10px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
}

.app-toast-action:hover {
  background: rgba(255, 255, 255, 0.28);
}

.toast-fade-enter-active,
.toast-fade-leave-active {
  transition: opacity 0.2s, transform 0.2s;
}

.toast-fade-enter-from,
.toast-fade-leave-to {
  opacity: 0;
  transform: translateX(-50%) translateY(8px);
}

@media (max-width: 640px) {
  .app-toast {
    bottom: calc(72px + env(safe-area-inset-bottom, 0px));
  }
}
</style>
