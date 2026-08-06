<script setup lang="ts">
import { computed } from 'vue'
import FcIcon from './FcIcon.vue'
import { buildRecipeImageSearchUrl } from '../utils/recipeImageSearch'

const props = defineProps<{
  recipeName: string
  compact?: boolean
}>()

const searchUrl = computed(() => buildRecipeImageSearchUrl(props.recipeName))
</script>

<template>
  <a
    class="recipe-image-search-link"
    :class="{ 'recipe-image-search-link--compact': compact }"
    :href="searchUrl"
    target="_blank"
    rel="noopener noreferrer"
    @click.stop
  >
    <FcIcon name="search" :size="compact ? 14 : 16" />
    <span>搜索参考图</span>
  </a>
</template>

<style scoped>
.recipe-image-search-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 8px 14px;
  border-radius: 999px;
  border: 1px solid var(--sage-border);
  background: rgba(255, 255, 255, 0.92);
  color: var(--deep-green);
  font-size: 13px;
  font-weight: 600;
  text-decoration: none;
  box-shadow: var(--shadow-sm);
  transition: border-color 0.15s, background 0.15s, color 0.15s;
}

.recipe-image-search-link:hover {
  border-color: var(--sage);
  background: var(--white);
  color: var(--sage);
}

.recipe-image-search-link--compact {
  padding: 6px 10px;
  font-size: 12px;
  gap: 4px;
}
</style>
