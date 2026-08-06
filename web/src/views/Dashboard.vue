<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import PantryStatus from '../components/PantryStatus.vue'
import RecipeCard from '../components/RecipeCard.vue'
import { getRecommendedRecipes, type RecommendationFilter } from '../api/recommendations'
import { useAuthStore } from '../stores/auth'
import { useFavoritesStore } from '../stores/favorites'
import { usePantryStore } from '../stores/pantry'
import { CATEGORY_LABELS } from '../stores/recipes'
import type { IconName } from '../assets/icons/registry'
import type { RecipeSummary } from '../types'
import FcIcon from '../components/FcIcon.vue'
import LoadingWait from '../components/LoadingWait.vue'
import { RECOMMEND_LOADING_STAGES } from '../composables/useElapsedTimer'

defineOptions({ name: 'Dashboard' })

const router = useRouter()
const auth = useAuthStore()
const favoritesStore = useFavoritesStore()
const pantryStore = usePantryStore()

const { availableItems, loading: loadingPantry, error: pantryError } = storeToRefs(pantryStore)

const FILTER_ORDER: RecommendationFilter[] = ['ready_now', 'high_match', 'all']

const recipes = ref<RecipeSummary[]>([])
const loadingRecipes = ref(false)
const recipeError = ref('')
const pantryIngredientCount = ref(0)
const recommendationFilter = ref<RecommendationFilter>('ready_now')
const emptyReason = ref<'none' | 'no-pantry' | 'no-match' | 'no-filter-match' | ''>('')

const recommendationFilters: { key: RecommendationFilter; label: string; hint: string }[] = [
  { key: 'ready_now', label: '现在能做', hint: '最多缺 2 样食材' },
  { key: 'high_match', label: '高匹配', hint: '匹配率 ≥ 60%' },
  { key: 'all', label: '可尝试', hint: '匹配率 ≥ 40%' },
]

const mealTypes: { key: string; label: string; icon: IconName }[] = [
  { key: 'BREAKFAST', label: '早餐', icon: 'sun' },
  { key: 'STAPLE', label: '主食', icon: 'bowl' },
  { key: 'MEAT_DISH', label: '荤菜', icon: 'knife' },
  { key: 'DESSERT', label: '甜点', icon: 'cake' },
]

const expiringItems = computed(() =>
  availableItems.value
    .filter((item) => item.expiringSoon ?? item.isExpiringSoon)
    .slice(0, 6),
)

const pantrySummary = computed(() => ({
  totalItems: availableItems.value.length,
  recipesAvailable: recipes.value.length,
}))

const sectionSubtitle = computed(() => {
  if (!auth.isAuthenticated) return ''
  const active = recommendationFilters.find((item) => item.key === recommendationFilter.value)
  if (pantryIngredientCount.value > 0 && active) return active.hint
  return '添加库存食材后可获得更精准推荐'
})

const showEmptyGuide = computed(() => {
  if (!auth.isAuthenticated || loadingRecipes.value) return false
  return emptyReason.value === 'no-pantry'
    || emptyReason.value === 'no-match'
    || emptyReason.value === 'no-filter-match'
    || (!recipes.value.length && !!recipeError.value)
})

function matchToSummary(match: Awaited<ReturnType<typeof getRecommendedRecipes>>['recipes'][number]): RecipeSummary {
  return {
    id: match.recipeId,
    name: match.recipeName,
    category: CATEGORY_LABELS[match.category] ?? match.category,
    description: match.description,
    difficultyText: match.difficultyText,
    difficultyLevel: match.difficultyLevel,
    calories: match.calories,
    coverImageUrl: match.coverImageUrl,
    ingredientCount: match.ingredientCount,
    matchPercent: match.matchRate,
  }
}

async function loadRecipes(options?: { smartDefault?: boolean }) {
  if (!auth.isAuthenticated) return
  loadingRecipes.value = true
  recipeError.value = ''
  emptyReason.value = ''

  const applyResult = (
    result: Awaited<ReturnType<typeof getRecommendedRecipes>>,
    filter: RecommendationFilter,
  ) => {
    recommendationFilter.value = filter
    pantryIngredientCount.value = result.pantryIngredientCount
    recipes.value = result.recipes.map(matchToSummary)
    if (!recipes.value.length) {
      if (!result.pantryIngredientCount) {
        emptyReason.value = 'no-pantry'
      } else if (filter !== 'all') {
        emptyReason.value = 'no-filter-match'
      } else {
        emptyReason.value = 'no-match'
      }
    }
  }

  try {
    if (options?.smartDefault) {
      for (const filter of FILTER_ORDER) {
        const result = await getRecommendedRecipes(8, filter)
        if (result.recipes.length > 0 || filter === 'all') {
          applyResult(result, filter)
          return
        }
      }
      return
    }

    const result = await getRecommendedRecipes(8, recommendationFilter.value)
    applyResult(result, recommendationFilter.value)
  } catch (error) {
    const axiosError = error as { response?: { data?: { message?: string } } }
    recipeError.value = axiosError.response?.data?.message ?? '推荐菜谱加载失败'
    recipes.value = []
    emptyReason.value = 'none'
  } finally {
    loadingRecipes.value = false
  }
}

async function loadPantry() {
  if (!auth.isAuthenticated) return
  await pantryStore.fetchAvailable()
}

function goToMealPlan() {
  router.push('/meal-plan')
}

function goToPantry() {
  router.push('/pantry')
}

function goToRecipes(category?: string) {
  router.push({
    name: 'recipes',
    query: category ? { category } : undefined,
  })
}

function goToLogin() {
  router.push('/login')
}

function openRecipe(id: number) {
  router.push({ name: 'recipeDetail', params: { id: String(id) } })
}

function setRecommendationFilter(filter: RecommendationFilter) {
  if (recommendationFilter.value === filter) return
  recommendationFilter.value = filter
  void loadRecipes()
}

function handleToggleFavorite(recipeId: number) {
  void favoritesStore.toggle(recipeId)
}

onMounted(() => {
  void loadRecipes({ smartDefault: true })
  void loadPantry()
})
</script>

<template>
  <main class="page-main dashboard-page">
    <section v-if="auth.isAuthenticated" class="home-header">
      <PantryStatus
        :total-items="loadingPantry ? 0 : pantrySummary.totalItems"
        :recipes-available="pantrySummary.recipesAvailable"
        :expiring-items="expiringItems"
        @go-pantry="goToPantry"
        @go-meal-plan="goToMealPlan"
      />
      <p v-if="pantryError" class="hero-data-error">{{ pantryError }}</p>
    </section>

    <section v-else class="home-header home-header--guest">
      <h1 class="home-title">今天吃什么</h1>
      <p class="home-subtitle">登录后根据冰箱库存推荐菜谱，并规划一周备餐。</p>
      <button class="cta-primary" type="button" @click="goToLogin">登录开始使用</button>
    </section>

    <section class="content-section">
      <div class="section-head section-head--tool">
        <div>
          <h2>根据库存推荐</h2>
          <p v-if="sectionSubtitle" class="section-subtitle">{{ sectionSubtitle }}</p>
        </div>
        <button class="view-all" type="button" @click="goToRecipes()">
          全部菜谱
          <FcIcon name="arrow-right" :size="16" />
        </button>
      </div>

      <div v-if="auth.isAuthenticated" class="filter-row recommend-filters">
        <button
          v-for="item in recommendationFilters"
          :key="item.key"
          type="button"
          :class="{ active: recommendationFilter === item.key }"
          :aria-pressed="recommendationFilter === item.key"
          @click="setRecommendationFilter(item.key)"
        >
          {{ item.label }}
        </button>
      </div>

      <LoadingWait
        v-if="loadingRecipes"
        :active="loadingRecipes"
        :stages="RECOMMEND_LOADING_STAGES"
        hint="通常 5–15 秒内完成"
      />
      <p v-else-if="!auth.isAuthenticated" class="empty-copy">登录后即可查看个性化推荐</p>
      <p v-else-if="recipeError && !showEmptyGuide" class="error-copy">{{ recipeError }}</p>

      <div v-else-if="showEmptyGuide" class="recommend-empty">
        <template v-if="emptyReason === 'no-pantry'">
          <h3>还没有登记库存</h3>
          <p>添加食材后，会按匹配度推荐可做的菜。</p>
          <div class="recommend-empty-actions">
            <button class="cta-primary" type="button" @click="goToPantry">去添加库存</button>
            <button class="secondary-btn" type="button" @click="goToRecipes()">先浏览菜谱</button>
          </div>
        </template>
        <template v-else-if="emptyReason === 'no-match'">
          <h3>暂无匹配菜谱</h3>
          <p>可以补充常用食材，或直接浏览菜谱库。</p>
          <div class="recommend-empty-actions">
            <button class="cta-primary" type="button" @click="goToPantry">补充库存</button>
            <button class="secondary-btn" type="button" @click="goToRecipes()">浏览菜谱</button>
          </div>
        </template>
        <template v-else-if="emptyReason === 'no-filter-match'">
          <h3>当前筛选下没有结果</h3>
          <p>试试「可尝试」，或再补充一些库存。</p>
          <div class="recommend-empty-actions">
            <button class="cta-primary" type="button" @click="setRecommendationFilter('all')">查看可尝试</button>
            <button class="secondary-btn" type="button" @click="goToPantry">补充库存</button>
          </div>
        </template>
        <template v-else>
          <h3>推荐暂时不可用</h3>
          <p>{{ recipeError || '请稍后重试，或先浏览菜谱。' }}</p>
          <div class="recommend-empty-actions">
            <button class="cta-primary" type="button" @click="() => loadRecipes()">重试</button>
            <button class="secondary-btn" type="button" @click="goToRecipes()">浏览菜谱</button>
          </div>
        </template>
      </div>

      <div v-else class="recipe-grid">
        <RecipeCard
          v-for="recipe in recipes"
          :key="recipe.id"
          :recipe="recipe"
          :favorited="favoritesStore.isFavorite(recipe.id)"
          @toggle-favorite="handleToggleFavorite"
          @open="openRecipe"
        />
      </div>
    </section>

    <section class="meal-picker">
      <h2>按分类找菜</h2>
      <div class="meal-pills">
        <button
          v-for="mt in mealTypes"
          :key="mt.key"
          type="button"
          @click="goToRecipes(mt.key)"
        >
          <FcIcon :name="mt.icon" :size="16" />
          {{ mt.label }}
        </button>
      </div>
    </section>
  </main>
</template>

<style scoped>
.dashboard-page {
  padding-top: 32px;
}

.recommend-filters {
  margin-bottom: 20px;
}

.section-subtitle {
  margin: 6px 0 0;
  color: var(--gray-text);
  font-size: 14px;
  line-height: 1.5;
}

.recommend-empty {
  padding: 24px 20px;
  border: 1px solid var(--sage-border);
  border-radius: var(--radius-md);
  background: var(--white);
}

.recommend-empty h3 {
  margin: 0 0 8px;
  color: var(--deep-green);
  font-size: 17px;
  font-weight: 600;
}

.recommend-empty p {
  margin: 0 0 16px;
  color: var(--gray-text);
  font-size: 14px;
  line-height: 1.55;
  max-width: 420px;
}

.recommend-empty-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

@media (max-width: 640px) {
  .dashboard-page {
    padding-top: 20px;
  }

  .recommend-empty {
    padding: 18px 16px;
  }

  .recommend-empty-actions {
    flex-direction: column;
  }

  .recommend-empty-actions .cta-primary,
  .recommend-empty-actions .secondary-btn {
    width: 100%;
    justify-content: center;
  }
}
</style>
