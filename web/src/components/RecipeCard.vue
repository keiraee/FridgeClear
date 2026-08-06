<script setup lang="ts">
import { computed } from 'vue'
import FcIcon from './FcIcon.vue'
import { RECIPE_FALLBACK_ICONS } from '../assets/icons/registry'
import type { IconName } from '../assets/icons/registry'
import type { RecipeSummary } from '../types'
import {
  difficultyStars,
  formatCalories,
  resolveCookingMinutes,
  truncateText,
} from '../utils/recipe'

const props = defineProps<{
  recipe: RecipeSummary
  favorited?: boolean
}>()

const emit = defineEmits<{
  (e: 'addToPlan', recipeId: number): void
  (e: 'toggleFavorite', recipeId: number): void
  (e: 'open', recipeId: number): void
}>()

const matchClass = computed(() => {
  const p = props.recipe.matchPercent ?? 0
  if (p >= 80) return 'high'
  if (p >= 50) return 'medium'
  return 'low'
})

const matchLabel = computed(() => {
  const p = props.recipe.matchPercent ?? 0
  if (!props.recipe.matchPercent) return '精选菜谱'
  if (p >= 95) return '库存可做'
  return `${p}% 库存匹配`
})

const toneClass = computed(() => {
  const id = props.recipe.id % 4
  const tones = ['tone-tomato', 'tone-egg', 'tone-shrimp', 'tone-soup'] as const
  return tones[id]
})

const fallbackIcon = computed<IconName>(() => RECIPE_FALLBACK_ICONS[props.recipe.id % RECIPE_FALLBACK_ICONS.length]!)

const stars = computed(() => difficultyStars(props.recipe.difficultyLevel))
const caloriesLabel = computed(() => formatCalories(props.recipe.calories))
const cookingMinutes = computed(() => resolveCookingMinutes(props.recipe))
const descriptionSnippet = computed(() => truncateText(props.recipe.description))
</script>

<template>
  <article class="recipe-card">
    <div class="match-bar" :class="matchClass" :title="matchLabel" />

    <div class="recipe-image" :class="toneClass" role="button" tabindex="0" @click="emit('open', recipe.id)" @keyup.enter="emit('open', recipe.id)">
      <div class="food-bg" />
      <img
        v-if="recipe.coverImageUrl"
        class="recipe-real-image"
        :src="recipe.coverImageUrl"
        :alt="recipe.name"
        loading="lazy"
      />
      <div v-else class="food-icon" aria-hidden="true">
        <FcIcon :name="fallbackIcon" :size="32" />
      </div>

      <button
        class="card-save"
        :class="{ 'is-favorited': favorited }"
        type="button"
        :aria-label="favorited ? '取消收藏' : '收藏菜谱'"
        :aria-pressed="favorited ? 'true' : 'false'"
        @click.stop="emit('toggleFavorite', recipe.id)"
      >
        <FcIcon :name="favorited ? 'heart-filled' : 'heart'" :size="18" />
      </button>

      <span class="match-badge" :class="{ 'full-match': recipe.matchPercent && recipe.matchPercent >= 95 }">
        {{ matchLabel }}
      </span>
    </div>

    <div class="recipe-body">
      <h3 class="recipe-title-link" role="button" tabindex="0" @click="emit('open', recipe.id)" @keyup.enter="emit('open', recipe.id)">{{ recipe.name }}</h3>

      <div v-if="stars || caloriesLabel || cookingMinutes" class="recipe-stats">
        <span v-if="stars" class="recipe-stars" :title="recipe.difficultyText ?? '难度'">{{ stars }}</span>
        <span v-if="caloriesLabel" class="recipe-stat">{{ caloriesLabel }}</span>
        <span v-if="cookingMinutes" class="recipe-stat">约 {{ cookingMinutes }} 分钟</span>
      </div>

      <p class="meta">
        {{ recipe.category }}
        <template v-if="recipe.ingredientCount"> · {{ recipe.ingredientCount }} 种食材</template>
        <template v-if="recipe.tag">
          <span class="recipe-tag">{{ recipe.tag }}</span>
        </template>
      </p>

      <p v-if="descriptionSnippet" class="recipe-desc">{{ descriptionSnippet }}</p>

      <button class="card-action" type="button" @click.stop="emit('addToPlan', recipe.id)">
        <FcIcon name="plus" :size="14" />
        加入备餐计划
      </button>
    </div>
  </article>
</template>

<style scoped>
.recipe-image[role='button'],
.recipe-title-link {
  cursor: pointer;
}
.recipe-body .card-action {
  cursor: pointer;
}
.recipe-title-link {
  margin: 0;
  transition: color 0.15s;
}
.recipe-title-link:hover {
  color: var(--light-orange);
}
.recipe-stats {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 10px;
  margin: 0 0 6px;
}
.recipe-stars {
  color: var(--light-orange);
  font-size: 12px;
  letter-spacing: 0.04em;
}
.recipe-stat {
  font-size: 11px;
  font-weight: 600;
  color: var(--gray-text);
  padding: 2px 8px;
  border-radius: 10px;
  background: var(--cream);
}
.recipe-desc {
  margin: 8px 0 0;
  font-size: 12px;
  line-height: 1.55;
  color: var(--gray-text);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
