<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import PantryStatus from '../components/PantryStatus.vue'
import RecipeCard from '../components/RecipeCard.vue'
import { getRecommendedRecipes } from '../api/recommendations'
import { useAuthStore } from '../stores/auth'
import { usePantryStore } from '../stores/pantry'
import { CATEGORY_LABELS } from '../stores/recipes'
import type { RecipeSummary } from '../types'

defineOptions({ name: 'Dashboard' })

const router = useRouter()
const auth = useAuthStore()
const pantryStore = usePantryStore()

const { availableItems, expiringCount, loading: loadingPantry, error: pantryError } = storeToRefs(pantryStore)

const recipes = ref<RecipeSummary[]>([])
const loadingRecipes = ref(false)
const recipeError = ref('')
const pantryIngredientCount = ref(0)

const pantrySummary = computed(() => ({
  totalItems: availableItems.value.length,
  expiringSoon: expiringCount.value,
  recipesAvailable: recipes.value.length,
}))

const sectionSubtitle = computed(() => {
  if (!auth.isAuthenticated) return '登录后根据你的冰箱库存推荐菜谱'
  if (pantryIngredientCount.value > 0) {
    return `已匹配 ${pantryIngredientCount.value} 种库存食材，优先展示高匹配菜谱`
  }
  return '添加库存食材后，可获得更精准的菜谱推荐'
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

async function loadRecipes() {
  if (!auth.isAuthenticated) return
  loadingRecipes.value = true
  recipeError.value = ''
  try {
    const result = await getRecommendedRecipes(8)
    pantryIngredientCount.value = result.pantryIngredientCount
    recipes.value = result.recipes.map(matchToSummary)
    if (!recipes.value.length) {
      recipeError.value = result.pantryIngredientCount
        ? '暂时没有找到与当前库存匹配的菜谱，试试补充更多食材。'
        : '先添加一些库存食材，我们就能为你推荐可做的菜。'
    }
  } catch (error) {
    const axiosError = error as { response?: { data?: { message?: string } } }
    recipeError.value = axiosError.response?.data?.message ?? '推荐菜谱加载失败'
    recipes.value = []
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

function handleAddToPlan(_recipeId: number) {
  router.push('/meal-plan')
}

function openRecipe(id: number) {
  router.push({ name: 'recipeDetail', params: { id: String(id) } })
}

function handleToggleFavorite(_recipeId: number) {
  // placeholder
}

onMounted(() => {
  void loadRecipes()
  void loadPantry()
})
</script>

<template>
  <main class="page-main">
    <!-- ============ HERO ============ -->
    <section class="hero-banner">
      <div class="hero-copy">
        <p class="overline">FRIDGECLEAR AI</p>
        <h1>今天，<br /><em>好好吃饭。</em></h1>
        <p class="hero-description">
          从你的冰箱出发，发现刚刚好的菜谱。优先消耗临期食材，也为每一餐保留一点惊喜。
        </p>
        <button class="cta-primary" type="button" @click="goToMealPlan">
          开始规划我的一周 <span class="arrow">→</span>
        </button>
      </div>

      <PantryStatus
        :total-items="loadingPantry ? 0 : pantrySummary.totalItems"
        :expiring-soon="pantrySummary.expiringSoon"
        :recipes-available="pantrySummary.recipesAvailable"
      />
      <p v-if="pantryError" class="hero-data-error">{{ pantryError }}</p>
    </section>

    <!-- ============ RECOMMENDED RECIPES ============ -->
    <section class="content-section">
      <div class="section-head">
        <div>
          <p class="overline">PANTRY MATCH</p>
          <h2>根据库存推荐</h2>
          <p class="section-subtitle">{{ sectionSubtitle }}</p>
        </div>
        <button class="view-all" type="button" @click="router.push('/recipes')">查看全部菜谱 →</button>
      </div>

      <p v-if="loadingRecipes" class="loading-copy">正在根据库存匹配菜谱…</p>
      <p v-else-if="recipeError" class="error-copy">{{ recipeError }}</p>
      <p v-else-if="!auth.isAuthenticated" class="empty-copy">登录后即可查看个性化推荐</p>
      <div v-else class="recipe-grid">
        <RecipeCard
          v-for="recipe in recipes"
          :key="recipe.id"
          :recipe="recipe"
          @add-to-plan="handleAddToPlan"
          @toggle-favorite="handleToggleFavorite"
          @open="openRecipe"
        />
      </div>
    </section>

    <!-- ============ PICK YOUR MEAL ============ -->
    <section class="meal-picker">
      <div>
        <p class="overline">PICK YOUR MEAL</p>
        <h2>按一餐开始探索</h2>
      </div>
      <div class="meal-pills">
        <button type="button" @click="router.push('/recipes')">☀️ 早餐</button>
        <button type="button" @click="router.push('/recipes')">🌤️ 午餐</button>
        <button type="button" @click="router.push('/recipes')">🌙 晚餐</button>
        <button type="button" @click="router.push('/recipes')">🍰 甜点</button>
      </div>
    </section>

    <!-- ============ AI PROMO STRIP ============ -->
    <section class="ai-strip">
      <div class="strip-text">
        <p class="overline">YOUR PERSONAL AI CHEF</p>
        <h2>冰箱里有什么，<br /><em>今晚就吃什么。</em></h2>
        <p class="strip-desc">
          输入库存、人数和忌口，FridgeClear 会帮你安排一份真正用得上的计划。
        </p>
      </div>
      <button class="cta-outline" type="button" @click="goToMealPlan">
        生成我的备餐计划 →
      </button>
    </section>
  </main>
</template>

<style scoped>
.section-subtitle {
  margin: 8px 0 0;
  color: var(--gray-text);
  font-size: 14px;
  line-height: 1.5;
}
</style>
