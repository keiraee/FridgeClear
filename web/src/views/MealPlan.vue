<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import type { MealPlan, MealPlanGenerateRequest, MealType, MealPlanItemStatus } from '../types'
import { defaultPlanConfig, simulatePlanGeneration } from '../mock/mealPlans'
import MealPlanResult from '../components/MealPlanResult.vue'

// --- State ---
const config = reactive<MealPlanGenerateRequest>({ ...defaultPlanConfig })
const plan = ref<MealPlan | null>(null)
const isGenerating = ref(false)
const generationError = ref('')

const mealTypeOptions: { value: MealType; label: string }[] = [
  { value: 'BREAKFAST', label: '早餐' },
  { value: 'LUNCH', label: '午餐' },
  { value: 'DINNER', label: '晚餐' },
  { value: 'SNACK', label: '甜点' },
]

const dietOptions = ['', '家常菜', '清淡', '低卡', '高蛋白', '素食']
const applianceOptions = ['炒锅', '电饭煲', '蒸锅', '烤箱', '空气炸锅', '微波炉', '压力锅']

// --- Methods ---
async function handleGenerate() {
  if (config.mealTypes.length === 0) {
    generationError.value = '请至少选择一个餐次'
    return
  }
  isGenerating.value = true
  generationError.value = ''
  plan.value = null

  try {
    const result = await simulatePlanGeneration()
    plan.value = result
  } catch {
    generationError.value = '生成失败，请稍后重试。'
  } finally {
    isGenerating.value = false
  }
}

function toggleMealType(mt: MealType) {
  const idx = config.mealTypes.indexOf(mt)
  if (idx >= 0) {
    config.mealTypes.splice(idx, 1)
  } else {
    config.mealTypes.push(mt)
  }
  // Trigger reactivity
  config.mealTypes = [...config.mealTypes]
}

function toggleAppliance(app: string) {
  if (!config.availableAppliances) config.availableAppliances = []
  const idx = config.availableAppliances.indexOf(app)
  if (idx >= 0) {
    config.availableAppliances.splice(idx, 1)
  } else {
    config.availableAppliances.push(app)
  }
  config.availableAppliances = [...config.availableAppliances]
}

function handleUpdateItemStatus(itemId: number, status: string) {
  if (!plan.value?.items) return
  const item = plan.value.items.find((i) => i.id === itemId)
  if (item) item.status = status as MealPlanItemStatus
}

function handleToggleShoppingItem(idx: number) {
  if (!plan.value?.shoppingList) return
  const item = plan.value.shoppingList[idx]
  item.status = item.status === 'PURCHASED' ? 'TODO' : 'PURCHASED'
}

// --- Lifecycle ---
// Reset error when config changes
watch(() => config.mealTypes, () => { generationError.value = '' })
</script>

<template>
  <main class="page-main meal-plan-page">
    <section class="plan-config-section">
      <div class="plan-page-header">
        <p class="overline">AI MEAL PLANNER</p>
        <h1>AI 备餐计划</h1>
        <p class="page-desc">告诉 AI 你的需求和限制，生成一份专属备餐计划。</p>
      </div>

      <!-- Config Form -->
      <div class="config-form">
        <div class="config-row">
          <label class="config-label">计划天数</label>
          <div class="config-control">
            <input type="range" min="1" max="7" v-model.number="config.days" />
            <span class="range-value">{{ config.days }} 天</span>
          </div>
        </div>

        <div class="config-row">
          <label class="config-label">用餐人数</label>
          <div class="config-control">
            <button
              type="button"
              class="stepper-btn"
              @click="config.peopleCount = Math.max(1, config.peopleCount - 1)"
            >−</button>
            <span class="stepper-value">{{ config.peopleCount }} 人</span>
            <button
              type="button"
              class="stepper-btn"
              @click="config.peopleCount = Math.min(10, config.peopleCount + 1)"
            >+</button>
          </div>
        </div>

        <div class="config-row">
          <label class="config-label">最长烹饪时间</label>
          <div class="config-control">
            <input type="range" min="10" max="180" step="5" v-model.number="config.maxCookingMinutes" />
            <span class="range-value">{{ config.maxCookingMinutes }} 分钟</span>
          </div>
        </div>

        <div class="config-row">
          <label class="config-label">餐次</label>
          <div class="config-control chips">
            <button
              v-for="mt in mealTypeOptions"
              :key="mt.value"
              type="button"
              class="chip-btn"
              :class="{ active: config.mealTypes.includes(mt.value) }"
              @click="toggleMealType(mt.value)"
            >
              {{ mt.label }}
            </button>
          </div>
        </div>

        <div class="config-row">
          <label class="config-label">饮食偏好</label>
          <div class="config-control">
            <select v-model="config.dietaryPreference" class="config-select">
              <option v-for="d in dietOptions" :key="d" :value="d">
                {{ d || '不限' }}
              </option>
            </select>
          </div>
        </div>

        <div class="config-row">
          <label class="config-label">忌口食材</label>
          <div class="config-control">
            <input
              type="text"
              class="config-input"
              placeholder="输入忌口食材，用逗号分隔"
              :value="config.dislikedIngredients?.join('，') ?? ''"
              @change="config.dislikedIngredients = ($event.target as HTMLInputElement).value.split(/[,，]/).map(s => s.trim()).filter(Boolean)"
            />
          </div>
        </div>

        <div class="config-row">
          <label class="config-label">可用厨具</label>
          <div class="config-control chips">
            <button
              v-for="app in applianceOptions"
              :key="app"
              type="button"
              class="chip-btn"
              :class="{ active: config.availableAppliances?.includes(app) }"
              @click="toggleAppliance(app)"
            >
              {{ app }}
            </button>
          </div>
        </div>

        <!-- Generate button -->
        <div class="config-actions">
          <button
            class="generate-btn"
            type="button"
            :disabled="isGenerating"
            @click="handleGenerate"
          >
            <template v-if="isGenerating">
              <span class="spinner" /> AI 正在生成计划...
            </template>
            <template v-else>
              ✨ 生成备餐计划
            </template>
          </button>
          <p v-if="generationError" class="form-error">{{ generationError }}</p>
        </div>
      </div>
    </section>

    <!-- ============ RESULTS ============ -->
    <section class="plan-results-section">
      <!-- Loading -->
      <div v-if="isGenerating" class="loading-state">
        <div class="loading-spinner" />
        <p>AI 正在分析你的库存，生成最佳搭配方案...</p>
      </div>

      <!-- Error -->
      <div v-else-if="generationError && !plan" class="error-state">
        <div class="error-icon">⚠️</div>
        <h3>生成失败</h3>
        <p>{{ generationError }}</p>
        <button class="retry-btn" type="button" @click="handleGenerate">重试</button>
      </div>

      <!-- Result -->
      <MealPlanResult
        v-else-if="plan"
        :plan="plan"
        @update-item-status="handleUpdateItemStatus"
        @toggle-shopping-item="handleToggleShoppingItem"
      />

      <!-- Empty (initial) -->
      <div v-else class="empty-state">
        <div class="empty-icon">🤖</div>
        <h3>准备好开始规划了吗？</h3>
        <p>配置你的偏好后，点击「生成备餐计划」，AI 会为你量身定制。</p>
      </div>
    </section>
  </main>
</template>
