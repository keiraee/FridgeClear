<script setup lang="ts">
import { computed, onActivated, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import RecipeCard from '../components/RecipeCard.vue'
import LoadingWait from '../components/LoadingWait.vue'
import { RECIPE_LIST_LOADING_STAGES } from '../composables/useElapsedTimer'
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

function syncSearchFromRoute() {
  const q = typeof route.query.q === 'string' ? route.query.q.trim() : ''
  keyword.value = q
  activeKeyword.value = q
}

function updateRouteSearch(nextKeyword: string) {
  const q = nextKeyword || undefined
  return router.replace({
    name: 'recipes',
    query: {
      ...route.query,
      q,
      category: route.query.category,
    },
  })
}

async function handleSearch() {
  showFavoritesOnly.value = false
  const next = keyword.value.trim()
  const current = typeof route.query.q === 'string' ? route.query.q.trim() : ''
  activeKeyword.value = next
  if (next !== current) {
    await updateRouteSearch(next)
    return
  }
  await loadFirstPage(true)
}

function openRecipe(id: number) {
  router.push({ name: 'recipeDetail', params: { id: String(id) } })
}

function resetFilters() {
  keyword.value = ''
  activeKeyword.value = ''
  showFavoritesOnly.value = false
  activeCategory.value = ''
  void router.replace({ name: 'recipes', query: {} })
  void loadFirstPage(true)
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
  syncSearchFromRoute()
  void loadFirstPage()
})

onActivated(() => {
  syncCategoryFromRoute()
  syncSearchFromRoute()
})

watch(
  () => route.query.category,
  () => {
    const previous = activeCategory.value
    syncCategoryFromRoute()
    if (previous !== activeCategory.value) void loadFirstPage(true)
  },
)

watch(
  () => route.query.q,
  () => {
    syncSearchFromRoute()
    void loadFirstPage(true)
  },
)
</script>

<template>
  <main class="page-main recipes-page">
    <section class="page-heading-row">
      <div>
        <h1>菜谱</h1>
        <p class="page-desc">按分类浏览，或搜索菜名、描述与食材。</p>
      </div>
    </section>

    <form class="recipe-search-form" @submit.prevent="handleSearch">
      <input v-model="keyword" placeholder="搜索菜名、食材，例如：西红柿" aria-label="搜索菜谱" />
      <button class="cta-primary" type="submit" :disabled="loading">{{ loading ? '搜索中…' : '搜索' }}</button>
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

    <LoadingWait
      v-if="loading"
      :active="loading"
      :stages="RECIPE_LIST_LOADING_STAGES"
      hint="首次进入可能需要几秒"
    />
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
          variant="list"
          :favorited="favoritesStore.isFavorite(recipe.id)"
          @toggle-favorite="toggleFavorite"
          @open="openRecipe"
        />
      </div>
      <p v-if="errorMessage" class="error-copy recipes-inline-error">{{ errorMessage }}</p>
      <div v-if="hasMore" class="recipes-load-more">
        <LoadingWait
          v-if="loadingMore"
          :active="loadingMore"
          :stages="RECIPE_LIST_LOADING_STAGES"
          hint="正在加载更多菜谱"
          compact
        />
        <button v-else class="secondary-btn" type="button" @click="loadMore">
          加载更多
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
