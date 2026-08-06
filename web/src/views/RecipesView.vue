<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import RecipeCard from '../components/RecipeCard.vue'
import { getFavoriteRecipes } from '../api/favorites'
import { CATEGORY_OPTIONS, RECIPES_PAGE_SIZE, useRecipesStore } from '../stores/recipes'
import { useFavoritesStore } from '../stores/favorites'
import type { RecipeSummary } from '../types'

defineOptions({ name: 'Recipes' })

const route = useRoute()
const router = useRouter()
const recipesStore = useRecipesStore()
const favoritesStore = useFavoritesStore()

const keyword = ref('')
const activeKeyword = ref('')
const activeCategory = ref('')
const showFavoritesOnly = ref(false)
const recipes = ref<RecipeSummary[]>([])
const page = ref(0)
const total = ref(0)
const loading = ref(false)
const loadingMore = ref(false)
const errorMessage = ref('')

const validCategories = new Set(CATEGORY_OPTIONS.map((item) => item.value))

const hasMore = computed(() => recipes.value.length < total.value)
const listSummary = computed(() => {
  if (!total.value) return ''
  return `已显示 ${recipes.value.length} / ${total.value} 道菜谱`
})

function listQuery(pageNumber = page.value) {
  return {
    keyword: activeKeyword.value || undefined,
    category: activeCategory.value || undefined,
    page: pageNumber,
    size: RECIPES_PAGE_SIZE,
  }
}

function syncCategoryFromRoute() {
  const raw = typeof route.query.category === 'string' ? route.query.category : ''
  activeCategory.value = validCategories.has(raw) ? raw : ''
}

function selectCategory(category: string) {
  showFavoritesOnly.value = false
  activeCategory.value = category
  void router.replace({
    name: 'recipes',
    query: {
      ...route.query,
      category: category || undefined,
    },
  })
  void loadFirstPage(true)
}

async function loadFirstPage(force = false) {
  page.value = 0
  if (showFavoritesOnly.value) {
    loading.value = true
    errorMessage.value = ''
    try {
      const result = await getFavoriteRecipes({ page: 0, size: RECIPES_PAGE_SIZE })
      recipes.value = result.items
      total.value = result.total
    } catch {
      errorMessage.value = '加载收藏失败，请稍后重试'
      recipes.value = []
      total.value = 0
    } finally {
      loading.value = false
    }
    return
  }

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
  loadingMore.value = true
  errorMessage.value = ''
  try {
    if (showFavoritesOnly.value) {
      const result = await getFavoriteRecipes({ page: nextPage, size: RECIPES_PAGE_SIZE })
      recipes.value = [...recipes.value, ...result.items]
      page.value = nextPage
      total.value = result.total
      return
    }
    const query = listQuery(nextPage)
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
  showFavoritesOnly.value = false
  activeKeyword.value = keyword.value.trim()
  await loadFirstPage(true)
}

function goToPlan() {
  router.push('/meal-plan')
}

function openRecipe(id: number) {
  router.push({ name: 'recipeDetail', params: { id: String(id) } })
}

function resetFilters() {
  keyword.value = ''
  activeKeyword.value = ''
  showFavoritesOnly.value = false
  selectCategory('')
}

function selectFavoritesOnly() {
  showFavoritesOnly.value = true
  activeCategory.value = ''
  void router.replace({ name: 'recipes', query: { ...route.query, category: undefined } })
  void loadFirstPage(true)
}

function toggleFavorite(id: number) {
  favoritesStore.toggle(id)
  if (showFavoritesOnly.value && !favoritesStore.isFavorite(id)) {
    recipes.value = recipes.value.filter((recipe) => recipe.id !== id)
    total.value = Math.max(0, total.value - 1)
  }
}

onMounted(() => {
  syncCategoryFromRoute()
  void loadFirstPage()
})

watch(
  () => route.query.category,
  () => {
    const previous = activeCategory.value
    syncCategoryFromRoute()
    if (previous !== activeCategory.value) void loadFirstPage(true)
  },
)
</script>

<template>
  <main class="page-main recipes-page">
    <section class="page-heading-row">
      <div>
        <p class="overline">RECIPE LIBRARY</p>
        <h1>菜谱</h1>
        <p class="page-desc">浏览菜谱库，找到下一道想做的菜。每次加载 {{ RECIPES_PAGE_SIZE }} 道，减轻等待时间。</p>
      </div>
    </section>

    <form class="recipe-search-form" @submit.prevent="handleSearch">
      <input v-model="keyword" placeholder="搜索菜名，例如：西红柿" aria-label="搜索菜谱" />
      <button class="cta-primary" type="submit" :disabled="loading">搜索</button>
    </form>

    <div class="recipes-category-row" role="tablist" aria-label="菜谱分类">
      <button
        type="button"
        class="category-chip"
        :class="{ selected: showFavoritesOnly }"
        @click="selectFavoritesOnly"
      >
        我的收藏
      </button>
      <button
        type="button"
        class="category-chip"
        :class="{ selected: !showFavoritesOnly && !activeCategory }"
        @click="showFavoritesOnly = false; selectCategory('')"
      >
        全部分类
      </button>
      <button
        v-for="option in CATEGORY_OPTIONS"
        :key="option.value"
        type="button"
        class="category-chip"
        :class="{ selected: !showFavoritesOnly && activeCategory === option.value }"
        @click="showFavoritesOnly = false; selectCategory(option.value)"
      >
        {{ option.label }}
      </button>
    </div>

    <p v-if="loading" class="loading-copy">正在加载菜谱…</p>
    <p v-else-if="errorMessage && !recipes.length" class="error-copy">{{ errorMessage }}</p>
    <div v-else-if="!recipes.length" class="recipes-empty">
      <p class="empty-copy">{{ showFavoritesOnly ? '还没有收藏的菜谱，去首页或列表里点 ♡ 收藏吧。' : '没有找到匹配的菜谱。' }}</p>
      <button class="secondary-btn" type="button" @click="resetFilters">
        查看全部菜谱
      </button>
    </div>

    <template v-else>
      <p v-if="listSummary" class="list-count recipes-list-summary">{{ listSummary }}</p>
      <div class="recipe-grid recipes-grid">
        <RecipeCard
          v-for="recipe in recipes"
          :key="recipe.id"
          :recipe="recipe"
          :favorited="favoritesStore.isFavorite(recipe.id)"
          @add-to-plan="goToPlan"
          @toggle-favorite="toggleFavorite"
          @open="openRecipe"
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
.recipes-category-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 0 0 20px;
}
.category-chip {
  border: 1px solid var(--sage-border);
  border-radius: 20px;
  padding: 8px 14px;
  background: var(--white);
  color: var(--gray-text);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s;
  white-space: nowrap;
  flex-shrink: 0;
}
.category-chip:hover {
  border-color: var(--sage);
  color: var(--sage);
}
.category-chip.selected {
  background: var(--sage-light);
  border-color: var(--sage);
  color: var(--deep-green);
  font-weight: 600;
}
.recipes-empty {
  display: grid;
  gap: 12px;
  justify-items: start;
}
.recipes-load-more { display: flex; justify-content: center; margin: 28px 0 12px; }
.recipes-load-more .secondary-btn { padding: 12px 28px; font-weight: 600; cursor: pointer; }
.recipes-load-more .secondary-btn:disabled { opacity: 0.65; cursor: wait; }
.recipes-inline-error { margin-top: 12px; }
.recipes-end-copy { margin-top: 20px; text-align: center; }

@media (max-width: 640px) {
  .recipe-search-form {
    flex-direction: column;
  }
  .recipe-search-form input {
    min-height: 44px;
    font-size: 16px;
  }
  .recipe-search-form .cta-primary {
    width: 100%;
    justify-content: center;
  }
  .recipes-category-row {
    flex-wrap: nowrap;
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
    padding-bottom: 6px;
    margin-right: -16px;
    padding-right: 16px;
    scrollbar-width: none;
  }
  .recipes-category-row::-webkit-scrollbar {
    display: none;
  }
  .category-chip {
    min-height: 40px;
  }
}
</style>
