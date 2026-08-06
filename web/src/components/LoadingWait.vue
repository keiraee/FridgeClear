<script setup lang="ts">
import { computed, watch } from 'vue'
import {
  DEFAULT_LOADING_STAGES,
  resolveLoadingStage,
  useElapsedTimer,
  type LoadingStage,
} from '../composables/useElapsedTimer'

const props = withDefaults(
  defineProps<{
    active: boolean
    stages?: LoadingStage[]
    hint?: string
    compact?: boolean
  }>(),
  {
    stages: () => DEFAULT_LOADING_STAGES,
    hint: '通常几秒内完成',
    compact: false,
  },
)

const { elapsedSeconds, elapsedLabel, start, stop, reset } = useElapsedTimer()

const stageText = computed(() => resolveLoadingStage(props.stages, elapsedSeconds.value))

watch(
  () => props.active,
  (isActive) => {
    if (isActive) {
      reset()
      start()
    } else {
      stop()
    }
  },
  { immediate: true },
)
</script>

<template>
  <div
    v-if="active"
    class="loading-wait"
    :class="{ 'loading-wait--compact': compact }"
    role="status"
    aria-live="polite"
  >
    <div class="loading-spinner" aria-hidden="true" />
    <h3>{{ stageText }}</h3>
    <p class="loading-wait-elapsed">已等待 <strong>{{ elapsedLabel }}</strong></p>
    <p v-if="hint" class="loading-wait-hint">{{ hint }}</p>
    <div class="loading-wait-bar" aria-hidden="true">
      <span class="loading-wait-bar-fill" />
    </div>
  </div>
</template>
