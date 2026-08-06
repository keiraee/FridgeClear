<script setup lang="ts">
import { computed } from 'vue'
import FcIcon from './FcIcon.vue'
import { RECIPE_FALLBACK_ICONS } from '../assets/icons/registry'
import type { IconName } from '../assets/icons/registry'
import type { RecipeSummary } from '../types'
import { resolveCookingMinutes } from '../utils/recipe'

const props = withDefaults(
  defineProps<{
    recipe: RecipeSummary
    favorited?: boolean
    variant?: 'compact' | 'list'
  }>(),
  { variant: 'compact' },
)

const emit = defineEmits<{
  (e: 'toggleFavorite', recipeId: number): void
  (e: 'open', recipeId: number): void
}>()

const matchLabel = computed(() => {
  const p = props.recipe.matchPercent ?? 0
  if (!props.recipe.matchPercent) return ''
  if (p >= 95) return '库存可做'
  return `${p}% 匹配`
})

const fallbackIcon = computed<IconName>(() => RECIPE_FALLBACK_ICONS[props.recipe.id % RECIPE_FALLBACK_ICONS.length]!)

const cookingMinutes = computed(() => resolveCookingMinutes(props.recipe))

const keyInfo = computed(() => {
  if (props.variant === 'list') {
    const parts: string[] = []
    if (props.recipe.category) parts.push(props.recipe.category)
    if (cookingMinutes.value) parts.push(`约 ${cookingMinutes.value} 分钟`)
    if (props.recipe.ingredientCount) parts.push(`${props.recipe.ingredientCount} 种食材`)
    return parts.join(' · ')
  }
  if (matchLabel.value) return matchLabel.value
  if (cookingMinutes.value) return `约 ${cookingMinutes.value} 分钟`
  if (props.recipe.category) return props.recipe.category
  return ''
})

const showMatchBadge = computed(() => props.variant === 'compact' && !!matchLabel.value)
</script>

<template>
  <article class="recipe-card" :class="{ 'recipe-card--list': variant === 'list' }">
    <div
      class="recipe-image tone-neutral"
      role="button"
      tabindex="0"
      @click="emit('open', recipe.id)"
      @keyup.enter="emit('open', recipe.id)"
    >
      <div class="food-bg" />
      <img
        v-if="recipe.coverImageUrl"
        class="recipe-real-image"
        :src="recipe.coverImageUrl"
        :alt="recipe.name"
        loading="lazy"
      />
      <div v-else class="food-icon" aria-hidden="true">
        <FcIcon :name="fallbackIcon" :size="28" />
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

      <span
        v-if="showMatchBadge"
        class="match-badge"
        :class="{ 'full-match': recipe.matchPercent && recipe.matchPercent >= 95 }"
      >
        {{ matchLabel }}
      </span>
    </div>

    <div class="recipe-body">
      <h3
        class="recipe-title-link"
        role="button"
        tabindex="0"
        @click="emit('open', recipe.id)"
        @keyup.enter="emit('open', recipe.id)"
      >
        {{ recipe.name }}
      </h3>
      <p v-if="keyInfo" class="recipe-key-info">{{ keyInfo }}</p>
    </div>
  </article>
</template>

<style scoped>
.recipe-image[role='button'],
.recipe-title-link {
  cursor: pointer;
}

.recipe-title-link {
  margin: 0;
  transition: color 0.15s;
}

.recipe-title-link:hover {
  color: var(--sage);
}

.recipe-key-info {
  margin: 6px 0 0;
  font-size: 13px;
  color: var(--gray-text);
  line-height: 1.4;
}

.recipe-card--list .recipe-key-info {
  font-size: 12px;
}
</style>
