<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import PantryStatus from '../components/PantryStatus.vue'
import RecipeCard from '../components/RecipeCard.vue'
import { getRecipes } from '../api/recipes'
import { getPantryItems } from '../api/pantry'
import { useAuthStore } from '../stores/auth'
import type { PantryItem, RecipeSummary } from '../types'

const router = useRouter()
const auth = useAuthStore()

const activeFilter = ref('热门')
const activeMealType = ref<string | null>(null)
const recipes = ref<RecipeSummary[]>([])
const pantryItems = ref<PantryItem[]>([])
const loadingRecipes = ref(false)
const loadingPantry = ref(false)
const recipeError = ref('')
const pantryError = ref('')

const filters = ['热门', '快手菜', '一锅料理', '健康轻食', '早餐', '午餐', '晚餐', '汤羹']

const mealTypes = [
  { key: 'breakfast', label: '☀️ 早餐' },
  { key: 'lunch', label: '🌤️ 午餐' },
  { key: 'dinner', label: '🌙 晚餐' },
  { key: 'dessert', label: '🍰 甜点' },
]

const filteredRecipes = computed(() => {
  return recipes.value
})

const categoryLabels: Record<string, string> = {
  AQUATIC: '水产', BREAKFAST: '早餐', CONDIMENT: '调味品', DESSERT: '甜点',
  DRINK: '饮品', MEAT_DISH: '肉菜', SEMI_FINISHED: '半成品', SOUP: '汤羹',
  STAPLE: '主食', VEGETABLE_DISH: '素菜', UNKNOWN: '其他',
}

async function loadRecipes() {
  if (!auth.isAuthenticated) return
  loadingRecipes.value = true
  recipeError.value = ''
  try {
    const result = await getRecipes({ page: 0, size: 8 })
    recipes.value = result.items.map((recipe) => ({
      ...recipe,
      category: categoryLabels[recipe.category] ?? recipe.category,
    }))
  } catch {
    recipeError.value = '菜谱加载失败，请确认后端服务已启动'
  } finally {
    loadingRecipes.value = false
  }
}

async function loadPantry() {
  if (!auth.isAuthenticated) return
  loadingPantry.value = true
  pantryError.value = ''
  try {
    pantryItems.value = (await getPantryItems({ status: 'AVAILABLE', page: 0, size: 100 })).items
  } catch {
    pantryError.value = '库存加载失败'
  } finally {
    loadingPantry.value = false
  }
}

const pantrySummary = computed(() => ({
  totalItems: pantryItems.value.length,
  expiringSoon: pantryItems.value.filter((item) => item.expiringSoon ?? item.isExpiringSoon).length,
  recipesAvailable: recipes.value.length,
}))

function goToMealPlan() {
  router.push('/meal-plan')
}

function handleAddToPlan(_recipeId: number) {
  router.push('/meal-plan')
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

    <!-- ============ TRENDING RECIPES ============ -->
    <section class="content-section">
      <div class="section-head">
        <div>
          <p class="overline">WEEKLY INSPIRATION</p>
          <h2>今天想吃什么？</h2>
        </div>
        <button class="view-all" type="button">查看全部菜谱 →</button>
      </div>

      <!-- Filter pills -->
      <div class="filter-row">
        <button
          v-for="filter in filters"
          :key="filter"
          type="button"
          :class="{ selected: activeFilter === filter }"
          @click="activeFilter = filter"
        >
          {{ filter }}
        </button>
      </div>

      <!-- Recipe grid -->
      <p v-if="loadingRecipes" class="loading-copy">正在加载真实菜谱…</p>
      <p v-else-if="recipeError" class="error-copy">{{ recipeError }}</p>
      <p v-else-if="!filteredRecipes.length" class="empty-copy">登录后即可查看菜谱内容</p>
      <div v-else class="recipe-grid">
        <RecipeCard
          v-for="recipe in filteredRecipes"
          :key="recipe.id"
          :recipe="recipe"
          @add-to-plan="handleAddToPlan"
          @toggle-favorite="handleToggleFavorite"
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
        <button
          v-for="mt in mealTypes"
          :key="mt.key"
          type="button"
          :class="{ active: activeMealType === mt.key }"
          @click="activeMealType = mt.key"
        >
          {{ mt.label }}
        </button>
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
