<script setup lang="ts">
import { computed } from 'vue'
import type { MealPlan } from '../types'

const props = defineProps<{
  plan: MealPlan
}>()

const emit = defineEmits<{
  (e: 'updateItemStatus', itemId: number, status: string): void
  (e: 'toggleShoppingItem', itemId: number): void
}>()

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
            <span class="meal-type-badge">{{ mealTypeLabel(item.mealType) }}</span>
            <strong class="plan-recipe-name">{{ item.recipe.name }}</strong>
            <span v-if="item.recipe.cookingMinutes" class="cook-time">{{ item.recipe.cookingMinutes }} 分钟</span>
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
