<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import type { ConfirmOptions } from '../composables/useConfirm'

type PendingConfirm = ConfirmOptions & { resolve: (value: boolean) => void }

const visible = ref(false)
const pending = ref<PendingConfirm | null>(null)

function onConfirmRequest(event: Event) {
  const detail = (event as CustomEvent<PendingConfirm>).detail
  if (!detail?.resolve) return
  pending.value = detail
  visible.value = true
}

function close(result: boolean) {
  visible.value = false
  pending.value?.resolve(result)
  pending.value = null
}

function onKeydown(event: KeyboardEvent) {
  if (!visible.value) return
  if (event.key === 'Escape') close(false)
}

onMounted(() => {
  window.addEventListener('fridgeclear:confirm', onConfirmRequest)
  window.addEventListener('keydown', onKeydown)
})

onUnmounted(() => {
  window.removeEventListener('fridgeclear:confirm', onConfirmRequest)
  window.removeEventListener('keydown', onKeydown)
})
</script>

<template>
  <Teleport to="body">
    <Transition name="confirm-fade">
      <div v-if="visible && pending" class="confirm-overlay" @click.self="close(false)">
        <div
          class="confirm-dialog"
          role="alertdialog"
          aria-modal="true"
          :aria-labelledby="pending.title ? 'confirm-title' : undefined"
          aria-describedby="confirm-message"
        >
          <h2 v-if="pending.title" id="confirm-title" class="confirm-title">{{ pending.title }}</h2>
          <p id="confirm-message" class="confirm-message">{{ pending.message }}</p>
          <div class="confirm-actions">
            <button class="secondary-btn" type="button" @click="close(false)">
              {{ pending.cancelLabel ?? '取消' }}
            </button>
            <button
              class="cta-primary"
              :class="{ 'confirm-danger': pending.danger }"
              type="button"
              @click="close(true)"
            >
              {{ pending.confirmLabel ?? '确定' }}
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.confirm-overlay {
  position: fixed;
  inset: 0;
  z-index: 200;
  display: grid;
  place-items: center;
  padding: 24px;
  background: rgba(31, 54, 48, 0.42);
}

.confirm-dialog {
  width: min(100%, 420px);
  padding: 24px;
  border-radius: var(--radius-lg);
  background: var(--white);
  border: 1px solid var(--sage-border);
  box-shadow: var(--shadow-lg);
}

.confirm-title {
  margin: 0 0 10px;
  font-size: 18px;
  line-height: 1.3;
}

.confirm-message {
  margin: 0;
  color: var(--gray-text);
  line-height: 1.6;
  font-size: 14px;
}

.confirm-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 20px;
}

.confirm-danger {
  background: #c45c5c;
  border-color: #c45c5c;
}

.confirm-danger:hover {
  background: #b34f4f;
  border-color: #b34f4f;
}

.confirm-fade-enter-active,
.confirm-fade-leave-active {
  transition: opacity 0.18s ease;
}

.confirm-fade-enter-from,
.confirm-fade-leave-to {
  opacity: 0;
}
</style>
