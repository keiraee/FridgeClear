<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import type { MealPlan, MealPlanItem } from '../types'

const props = defineProps<{
  plan: MealPlan
}>()

const emit = defineEmits<{
  (e: 'updateItemStatus', itemId: number, status: string): void
  (e: 'toggleShoppingItem', itemId: number): void
}>()

const router = useRouter()

function mealTypeLabel(type: string): string {
  const map: Record<string, string> = {
    BREAKFAST: '早餐',
    LUNCH: '午餐',
    DINNER: '晚餐',
    SNACK: '甜点',
  }
  return map[type] ?? type
}

function formatDate(dateStr: string): string {
  const d = new Date(dateStr)
  const days = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  return `${dateStr} ${days[d.getDay()]}`
}

// Group items by date
const groupedItems = computed(() => {
  const groups: Record<string, NonNullable<typeof props.plan.items>> = {}
  props.plan.items?.forEach((item) => {
    if (!groups[item.planDate]) groups[item.planDate] = []
    groups[item.planDate]!.push(item)
  })
  return groups
})

function recipeCoverUrl(item: MealPlanItem) {
  return item.recipe.coverImageUrl ?? null
}

function openRecipe(recipeId: number) {
  router.push({ name: 'recipeDetail', params: { id: String(recipeId) } })
}
</script>

<template>
  <div v-if="plan" class="plan-result">
    <!-- Summary -->
    <div v-if="plan.summary" class="plan-summary-card">
      <div class="summary-icon">✨</div>
      <div>
        <h3>AI 规划摘要</h3>
        <p>{{ plan.summary }}</p>
      </div>
    </div>

    <!-- Expiring alerts -->
    <div v-if="plan.expiringIngredients?.length" class="expiring-alerts">
      <span class="alert-label">⚠️ 优先消耗</span>
      <span v-for="ing in plan.expiringIngredients" :key="ing.pantryItemId" class="alert-chip">
        {{ ing.name }}（{{ ing.expireDate }} 到期）
      </span>
    </div>

    <!-- Meal plan by day -->
    <div class="plan-days">
      <div v-for="(items, date) in groupedItems" :key="date" class="plan-day-group">
        <h3 class="day-heading">{{ formatDate(date) }}</h3>

        <div v-for="item in items" :key="item.id" class="plan-item">
          <div class="plan-item-header">
            <button
              type="button"
              class="plan-recipe-thumb"
              :aria-label="`查看菜谱：${item.recipe.name}`"
              @click="openRecipe(item.recipe.id)"
            >
              <img v-if="recipeCoverUrl(item)" :src="recipeCoverUrl(item)!" :alt="item.recipe.name" loading="lazy" />
              <span v-else class="plan-recipe-thumb-fallback" aria-hidden="true">🍳</span>
            </button>
            <div class="plan-item-title">
              <div class="plan-item-title-row">
                <span class="meal-type-badge">{{ mealTypeLabel(item.mealType) }}</span>
                <button type="button" class="plan-recipe-name" @click="openRecipe(item.recipe.id)">
                  {{ item.recipe.name }}
                </button>
                <span v-if="item.recipe.cookingMinutes" class="cook-time">{{ item.recipe.cookingMinutes }} 分钟</span>
              </div>
            </div>
          </div>

          <p class="plan-reason">💡 {{ item.reason }}</p>

          <div class="plan-ingredients">
            <div class="ingredient-group">
              <span class="ing-label have">✅ 已有食材</span>
              <span v-for="name in item.usedIngredients" :key="name" class="ing-chip have">{{ name }}</span>
              <span v-if="!item.usedIngredients.length" class="ing-empty">无</span>
            </div>
            <div class="ingredient-group">
              <span class="ing-label missing">🛒 需要购买</span>
              <span v-for="name in item.missingIngredients" :key="name" class="ing-chip missing">{{ name }}</span>
              <span v-if="!item.missingIngredients.length" class="ing-empty">无</span>
            </div>
          </div>

          <div class="plan-item-actions">
            <select
              class="status-select"
              :value="item.status"
              @change="emit('updateItemStatus', item.id, ($event.target as HTMLSelectElement).value)"
            >
              <option value="PLANNED">📋 已计划</option>
              <option value="COOKED">✅ 已完成</option>
              <option value="SKIPPED">⏭️ 跳过</option>
            </select>
          </div>
        </div>
      </div>
    </div>

    <!-- Shopping list -->
    <div v-if="plan.shoppingList?.length" class="shopping-section">
      <h3>🛒 采购清单</h3>
      <div class="shopping-table">
        <div class="shopping-header">
          <span>食材</span>
          <span>数量</span>
          <span>原因</span>
          <span>状态</span>
        </div>
        <div v-for="item in plan.shoppingList" :key="item.id" class="shopping-row" :class="{ purchased: item.status === 'PURCHASED' }">
          <span class="shopping-name">{{ item.name }}</span>
          <span class="shopping-qty">{{ item.quantity }} {{ item.unit }}</span>
          <span class="shopping-reason">{{ item.reason }}</span>
          <button
            class="buy-btn"
            :class="{ done: item.status === 'PURCHASED' }"
            type="button"
            @click="emit('toggleShoppingItem', item.id ?? 0)"
          >
            {{ item.status === 'PURCHASED' ? '已购买 ✓' : '标记已买' }}
          </button>
        </div>
      </div>
    </div>
  </div>

  <!-- Empty state -->
  <div v-else class="empty-state">
    <div class="empty-icon">📋</div>
    <h3>还没有备餐计划</h3>
    <p>配置你的偏好，让 AI 帮你生成一份专属计划。</p>
  </div>
</template>

<style scoped>
.plan-item-header {
  align-items: flex-start;
}

.plan-recipe-thumb {
  width: 72px;
  height: 72px;
  flex-shrink: 0;
  padding: 0;
  border: 1px solid var(--sage-border);
  border-radius: var(--radius-md);
  overflow: hidden;
  background: linear-gradient(145deg, var(--sage-light), #f6efe8);
  cursor: pointer;
}

.plan-recipe-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.plan-recipe-thumb-fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  font-size: 28px;
}

.plan-item-title {
  flex: 1;
  min-width: 0;
}

.plan-item-title-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px 12px;
}

.plan-recipe-name {
  margin: 0;
  padding: 0;
  border: 0;
  background: transparent;
  font-size: 17px;
  font-weight: 700;
  color: var(--deep-green);
  text-align: left;
  cursor: pointer;
  transition: color 0.15s;
}

.plan-recipe-name:hover {
  color: var(--light-orange);
}
</style>
