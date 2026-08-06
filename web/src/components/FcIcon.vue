<script setup lang="ts">
import { computed } from 'vue'
import { ICON_COMPONENTS, type IconName } from '../assets/icons/registry'

const props = withDefaults(
  defineProps<{
    name: IconName
    size?: number | string
    label?: string
  }>(),
  {
    size: 20,
  },
)

const IconComponent = computed(() => ICON_COMPONENTS[props.name])
const pixelSize = computed(() => (typeof props.size === 'number' ? `${props.size}px` : props.size))
</script>

<template>
  <component
    :is="IconComponent"
    class="fc-icon"
    :style="{ width: pixelSize, height: pixelSize }"
    :aria-hidden="label ? undefined : true"
    :aria-label="label"
    role="img"
  />
</template>

<style scoped>
.fc-icon {
  display: inline-block;
  flex-shrink: 0;
  color: inherit;
  line-height: 0;
  vertical-align: middle;
}
</style>
