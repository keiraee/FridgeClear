<script setup lang="ts">
import type { PantryItem } from '../types'
import { formatExpireLabel } from '../utils/pantry'

defineProps<{
  totalItems: number
  recipesAvailable: number
  expiringItems: PantryItem[]
}>()

const emit = defineEmits<{
  goPantry: []
  goMealPlan: []
}>()
</script>

<template>
  <section class="home-pantry-bar">
    <div class="home-pantry-top">
      <div class="home-pantry-summary">
        <h1 class="home-title">今天吃什么</h1>
        <p class="home-subtitle">
          库存 {{ totalItems }} 种
          <template v-if="recipesAvailable > 0"> · 可做 {{ recipesAvailable }} 道菜</template>
        </p>
      </div>
      <div class="home-pantry-actions">
        <button class="cta-primary" type="button" @click="emit('goMealPlan')">
          用现有食材规划本周
        </button>
        <button class="secondary-btn" type="button" @click="emit('goPantry')">
          管理库存
        </button>
      </div>
    </div>

    <div v-if="expiringItems.length" class="expiring-strip">
      <span class="expiring-strip-label">临期优先</span>
      <div class="expiring-chips" role="list">
        <span
          v-for="item in expiringItems"
          :key="item.id"
          class="expiring-chip"
          role="listitem"
        >
          {{ formatExpireLabel(item) }}
        </span>
      </div>
    </div>
  </section>
</template>
