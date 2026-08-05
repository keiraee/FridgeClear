<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import RecipeCard from '../components/RecipeCard.vue'
import { RECIPES_PAGE_SIZE, useRecipesStore } from '../stores/recipes'
import type { RecipeSummary } from '../types'

defineOptions({ name: 'Recipes' })

const router = useRouter()
const recipesStore = useRecipesStore()

const keyword = ref('')
const activeKeyword = ref('')
const recipes = ref<RecipeSummary[]>([])
const page = ref(0)
const total = ref(0)
const loading = ref(false)
const loadingMore = ref(false)
const errorMessage = ref('')

const hasMore = computed(() => recipes.value.length < total.value)
const listSummary = computed(() => {
  if (!total.value) return ''
  return `已显示 ${recipes.value.length} / ${total.value} 道菜谱`
})

function listQuery(pageNumber = page.value) {
  return {
    keyword: activeKeyword.value || undefined,
    page: pageNumber,
    size: RECIPES_PAGE_SIZE,
  }
}

async function loadFirstPage(force = false) {
  page.value = 0
  const query = listQuery(0)
  if (!force) {
    const cached = recipesStore.getCachedPage(query)
    if (cached) {
      recipes.value = cached.items
      total.value = cached.total
      errorMessage.value = recipesStore.getError(query)
      return
    }
  }
  loading.value = true
  errorMessage.value = ''
  try {
    const result = await recipesStore.fetchPage(query, { force })
    recipes.value = result.items
    total.value = result.total
    errorMessage.value = recipesStore.getError(query)
  } finally {
    loading.value = false
  }
}

async function loadMore() {
  if (!hasMore.value || loadingMore.value || loading.value) return
  const nextPage = page.value + 1
  const query = listQuery(nextPage)
  loadingMore.value = true
  errorMessage.value = ''
  try {
    const result = await recipesStore.fetchPage(query)
    recipes.value = [...recipes.value, ...result.items]
    page.value = result.page
    total.value = result.total
    errorMessage.value = recipesStore.getError(query)
  } finally {
    loadingMore.value = false
  }
}

async function handleSearch() {
  activeKeyword.value = keyword.value.trim()
  await loadFirstPage(true)
}

function goToPlan() {
  router.push('/meal-plan')
}

function toggleFavorite(_id: number) {
  // 收藏功能下一步接入
}

onMounted(() => loadFirstPage())
</script>

<template>
  <main class="page-main recipes-page">
    <section class="page-heading-row">
      <div>
        <p class="overline">RECIPE LIBRARY</p>
        <h1>菜谱</h1>
        <p class="page-desc">从 HowToCook 菜谱库中找到下一道想做的菜。每次加载 {{ RECIPES_PAGE_SIZE }} 道，减轻等待时间。</p>
      </div>
    </section>

    <form class="recipe-search-form" @submit.prevent="handleSearch">
      <input v-model="keyword" placeholder="搜索菜名，例如：西红柿" aria-label="搜索菜谱" />
      <button class="cta-primary" type="submit" :disabled="loading">搜索</button>
    </form>

    <p v-if="loading" class="loading-copy">正在加载菜谱…</p>
    <p v-else-if="errorMessage && !recipes.length" class="error-copy">{{ errorMessage }}</p>
    <p v-else-if="!recipes.length" class="empty-copy">没有找到匹配的菜谱。</p>

    <template v-else>
      <p v-if="listSummary" class="list-count recipes-list-summary">{{ listSummary }}</p>
      <div class="recipe-grid recipes-grid">
        <RecipeCard
          v-for="recipe in recipes"
          :key="recipe.id"
          :recipe="recipe"
          @add-to-plan="goToPlan"
          @toggle-favorite="toggleFavorite"
        />
      </div>
      <p v-if="errorMessage" class="error-copy recipes-inline-error">{{ errorMessage }}</p>
      <div v-if="hasMore" class="recipes-load-more">
        <button class="secondary-btn" type="button" :disabled="loadingMore" @click="loadMore">
          {{ loadingMore ? '加载中…' : '加载更多' }}
        </button>
      </div>
      <p v-else-if="total > RECIPES_PAGE_SIZE" class="empty-copy recipes-end-copy">已加载全部菜谱</p>
    </template>
  </main>
</template>

<style scoped>
.recipes-list-summary { margin: 0 0 16px; }
.recipes-load-more { display: flex; justify-content: center; margin: 28px 0 12px; }
.recipes-load-more .secondary-btn { padding: 12px 28px; font-weight: 600; cursor: pointer; }
.recipes-load-more .secondary-btn:disabled { opacity: 0.65; cursor: wait; }
.recipes-inline-error { margin-top: 12px; }
.recipes-end-copy { margin-top: 20px; text-align: center; }
</style>
