<script setup lang="ts">
import { computed } from 'vue'
import type { RecipeSummary } from '../types'

const props = defineProps<{
  recipe: RecipeSummary
}>()

const emit = defineEmits<{
  (e: 'addToPlan', recipeId: number): void
  (e: 'toggleFavorite', recipeId: number): void
}>()

const matchClass = computed(() => {
  const p = props.recipe.matchPercent ?? 0
  if (p >= 80) return 'high'
  if (p >= 50) return 'medium'
  return 'low'
})

const matchLabel = computed(() => {
  const p = props.recipe.matchPercent ?? 0
  if (p >= 95) return '库存可做'
  return `${p}% 库存匹配`
})

const toneClass = computed(() => {
  const id = props.recipe.id % 4
  const tones = ['tone-tomato', 'tone-egg', 'tone-shrimp', 'tone-soup'] as const
  return tones[id]
})

const foodIcon = computed(() => {
  const icons = ['🍝', '🍳', '🍤', '🥣']
  return icons[props.recipe.id % icons.length]
})
</script>

<template>
  <article class="recipe-card">
    <div class="match-bar" :class="matchClass" :title="matchLabel" />

    <div class="recipe-image" :class="toneClass">
      <div class="food-bg" />
      <div class="food-icon" aria-hidden="true">{{ foodIcon }}</div>

      <button
        class="card-save"
        type="button"
        aria-label="收藏菜谱"
        @click="emit('toggleFavorite', recipe.id)"
      >♡</button>

      <span class="match-badge" :class="{ 'full-match': recipe.matchPercent && recipe.matchPercent >= 95 }">
        {{ matchLabel }}
      </span>
    </div>

    <div class="recipe-body">
      <h3>{{ recipe.name }}</h3>
      <p class="meta">
        {{ recipe.category }}
        <template v-if="recipe.cookingMinutes"> · {{ recipe.cookingMinutes }} 分钟</template>
        <template v-if="recipe.difficultyText"> · {{ recipe.difficultyText }}</template>
        <template v-if="recipe.tag">
          <span class="recipe-tag">{{ recipe.tag }}</span>
        </template>
      </p>

      <button class="card-action" type="button" @click="emit('addToPlan', recipe.id)">
        ＋ 加入备餐计划
      </button>
    </div>
  </article>
</template>
