<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import PantryStatus from '../components/PantryStatus.vue'
import RecipeCard from '../components/RecipeCard.vue'
import { mockRecipes } from '../mock/recipes'
import { pantrySummary } from '../mock/pantry'

const router = useRouter()

const activeFilter = ref('热门')
const activeMealType = ref<string | null>(null)

const filters = ['热门', '快手菜', '一锅料理', '健康轻食', '早餐', '午餐', '晚餐', '汤羹']

const mealTypes = [
  { key: 'breakfast', label: '☀️ 早餐' },
  { key: 'lunch', label: '🌤️ 午餐' },
  { key: 'dinner', label: '🌙 晚餐' },
  { key: 'dessert', label: '🍰 甜点' },
]

const filteredRecipes = computed(() => {
  // In a real app, filter by API. For now, show all mock recipes.
  return mockRecipes
})

function goToMealPlan() {
  router.push('/meal-plan')
}

function handleAddToPlan(_recipeId: number) {
  router.push('/meal-plan')
}

function handleToggleFavorite(_recipeId: number) {
  // placeholder
}
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
        :total-items="pantrySummary.totalItems"
        :expiring-soon="pantrySummary.expiringSoon"
        :recipes-available="pantrySummary.recipesAvailable"
      />
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
      <div class="recipe-grid">
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
