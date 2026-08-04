<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import RecipeCard from '../components/RecipeCard.vue'
import { getRecipes } from '../api/recipes'
import type { RecipeSummary } from '../types'

const router = useRouter()
const keyword = ref('')
const recipes = ref<RecipeSummary[]>([])
const loading = ref(false)
const errorMessage = ref('')
const categoryLabels: Record<string, string> = { AQUATIC: '水产', BREAKFAST: '早餐', CONDIMENT: '调味品', DESSERT: '甜点', DRINK: '饮品', MEAT_DISH: '肉菜', SEMI_FINISHED: '半成品', SOUP: '汤羹', STAPLE: '主食', VEGETABLE_DISH: '素菜', UNKNOWN: '其他' }

async function loadRecipes() {
  loading.value = true
  errorMessage.value = ''
  try {
    const result = await getRecipes({ keyword: keyword.value.trim() || undefined, page: 0, size: 40 })
    recipes.value = result.items.map((recipe) => ({ ...recipe, category: categoryLabels[recipe.category] ?? recipe.category }))
  } catch { errorMessage.value = '菜谱加载失败，请确认后端服务已启动' }
  finally { loading.value = false }
}

function goToPlan() { router.push('/meal-plan') }
function toggleFavorite(_id: number) { /* 收藏功能下一步接入 */ }
onMounted(loadRecipes)
</script>

<template>
  <main class="page-main recipes-page">
    <section class="page-heading-row">
      <div><p class="overline">RECIPE LIBRARY</p><h1>菜谱</h1><p class="page-desc">从 HowToCook 菜谱库中找到下一道想做的菜。</p></div>
    </section>
    <form class="recipe-search-form" @submit.prevent="loadRecipes">
      <input v-model="keyword" placeholder="搜索菜名，例如：西红柿" aria-label="搜索菜谱" />
      <button class="cta-primary" type="submit">搜索</button>
    </form>
    <p v-if="loading" class="loading-copy">正在加载菜谱…</p>
    <p v-else-if="errorMessage" class="error-copy">{{ errorMessage }}</p>
    <p v-else-if="!recipes.length" class="empty-copy">没有找到匹配的菜谱。</p>
    <div v-else class="recipe-grid recipes-grid">
      <RecipeCard v-for="recipe in recipes" :key="recipe.id" :recipe="recipe" @add-to-plan="goToPlan" @toggle-favorite="toggleFavorite" />
    </div>
  </main>
</template>
