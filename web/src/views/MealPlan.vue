<script setup lang="ts">
import { ref, reactive, watch, computed, onUnmounted, onMounted, onActivated } from 'vue'
import { useRoute } from 'vue-router'
import { storeToRefs } from 'pinia'
import type { MealPlan, MealPlanGenerateRequest, MealType, MealPlanItemStatus } from '../types'
import { archiveMealPlan, generateMealPlan, getMealPlan, updateMealPlanItemStatus, updateShoppingItemStatus } from '../api/mealPlans'
import { useAuthStore } from '../stores/auth'
import { useMealPlansStore } from '../stores/mealPlans'
import MealPlanResult from '../components/MealPlanResult.vue'
import LoadingWait from '../components/LoadingWait.vue'
import FcIcon from '../components/FcIcon.vue'
import { MEAL_PLAN_LOADING_STAGES } from '../composables/useElapsedTimer'

defineOptions({ name: 'MealPlan' })

const route = useRoute()
const auth = useAuthStore()
const mealPlansStore = useMealPlansStore()
const { history: historyPlans, loading: historyLoading } = storeToRefs(mealPlansStore)

const config = reactive<MealPlanGenerateRequest>({
  days: 3, peopleCount: 2, maxCookingMinutes: 30, mealTypes: ['DINNER'],
  dietaryPreference: '家常菜', dislikedIngredients: [], availableAppliances: ['炒锅'],
})
const plan = ref<MealPlan | null>(null)
const isGenerating = ref(false)
const generationError = ref('')
let generationAbortController: AbortController | undefined

function resetPageState() {
  generationAbortController?.abort()
  generationAbortController = undefined
  isGenerating.value = false
  generationError.value = ''
  plan.value = null
}

const mealTypeOptions: { value: MealType; label: string }[] = [
  { value: 'BREAKFAST', label: '早餐' },
  { value: 'LUNCH', label: '午餐' },
  { value: 'DINNER', label: '晚餐' },
  { value: 'SNACK', label: '甜点' },
]

const dietOptions = ['', '家常菜', '清淡', '低卡', '高蛋白', '素食']
const applianceOptions = ['炒锅', '电饭煲', '蒸锅', '烤箱', '空气炸锅', '微波炉', '压力锅']

const focusRecipeId = computed(() => {
  const raw = route.query.recipeId
  const id = typeof raw === 'string' ? Number(raw) : NaN
  return Number.isFinite(id) && id > 0 ? id : null
})

// --- Methods ---
async function handleGenerate() {
  if (config.mealTypes.length === 0) {
    generationError.value = '请至少选择一个餐次'
    return
  }
  isGenerating.value = true
  generationError.value = ''
  plan.value = null
  generationAbortController?.abort()
  generationAbortController = new AbortController()

  try {
    plan.value = await generateMealPlan({ ...config }, { signal: generationAbortController.signal })
    mealPlansStore.invalidate()
    await loadHistory()
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') return
    const axiosError = error as {
      code?: string
      message?: string
      response?: { status?: number; data?: { code?: string; message?: string } }
    }
    if (
      axiosError.code === 'AI_SERVICE_UNAVAILABLE'
      || axiosError.response?.data?.code === 'AI_SERVICE_UNAVAILABLE'
      || axiosError.response?.status === 503
      || axiosError.message?.includes('AI 服务暂不可用')
    ) {
      generationError.value = 'AI 服务暂不可用，请稍后再试。'
    } else if (axiosError.code === 'POLL_TIMEOUT' || axiosError.code === 'ECONNABORTED') {
      generationError.value = 'AI 响应时间较长，请稍后重试或检查模型服务状态。'
    } else {
      generationError.value = axiosError.response?.data?.message ?? axiosError.message ?? '生成失败，请稍后重试。'
    }
  } finally {
    isGenerating.value = false
    generationAbortController = undefined
  }
}

async function loadHistory() {
  await mealPlansStore.fetchHistory()
}

async function openHistory(item: { id: number }) {
  try { plan.value = await getMealPlan(item.id) }
  catch { generationError.value = '历史备餐计划加载失败' }
}

async function archiveCurrentPlan() {
  if (!plan.value || !window.confirm('确定归档当前备餐计划吗？')) return
  try {
    await archiveMealPlan(plan.value.id)
    plan.value = null
    mealPlansStore.invalidate()
    await loadHistory()
  } catch { generationError.value = '归档备餐计划失败' }
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

async function handleUpdateItemStatus(itemId: number, status: string) {
  if (!plan.value?.items) return
  const item = plan.value.items.find((i) => i.id === itemId)
  if (!item || !plan.value.id) return
  try {
    const updated = await updateMealPlanItemStatus(plan.value.id, itemId, status as MealPlanItemStatus)
    item.status = updated.status
  } catch { generationError.value = '更新计划状态失败' }
}

async function handleToggleShoppingItem(itemId: number) {
  if (!plan.value?.shoppingList) return
  const item = plan.value.shoppingList.find((current) => current.id === itemId)
  if (!item || !item.id) return
  const status = item.status === 'PURCHASED' ? 'TODO' : 'PURCHASED'
  try { item.status = (await updateShoppingItemStatus(item.id, status)).status }
  catch { generationError.value = '更新采购状态失败' }
}

// --- Lifecycle ---
watch(() => config.mealTypes, () => { generationError.value = '' })

watch(() => auth.user?.id, (userId, previousId) => {
  if (previousId === undefined) return
  if (userId === previousId) return
  resetPageState()
  if (userId) void mealPlansStore.fetchHistory({ force: true })
})

onActivated(() => {
  if (!auth.isAuthenticated) {
    resetPageState()
    return
  }
  if (!historyPlans.value.length) void loadHistory()
})

onUnmounted(() => {
  generationAbortController?.abort()
})
onMounted(() => {
  if (auth.isAuthenticated) void loadHistory()
})
</script>

<template>
  <main class="page-main meal-plan-page">
    <section class="plan-config-section">
      <div class="plan-page-header">
        <h1>备餐计划</h1>
        <p class="page-desc">设置天数、人数和忌口，根据库存生成一周菜单和采购清单。</p>
        <p v-if="focusRecipeId" class="plan-focus-hint">
          已从菜谱详情带入关注菜谱 #{{ focusRecipeId }}。生成计划时 AI 仍会结合库存综合安排，可在结果中查看是否被选入。
        </p>
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
              <span class="spinner" /> 生成中…
            </template>
            <template v-else>
              <FcIcon name="spark" :size="18" />
              生成备餐计划
            </template>
          </button>
          <p v-if="generationError" class="form-error">{{ generationError }}</p>
        </div>
      </div>

      <!-- History -->
      <div class="plan-history-card">
        <div class="section-head compact">
          <div><h2>历史计划</h2></div>
          <span class="list-count">{{ historyLoading ? '加载中…' : `${historyPlans.length} 份` }}</span>
        </div>
        <div v-if="!historyLoading && !historyPlans.length" class="plan-history-empty">
          <p>还没有保存的计划</p>
          <p class="plan-history-empty-hint">生成后会自动保存在这里。</p>
        </div>
        <div v-else class="plan-history-list">
          <button v-for="item in historyPlans" :key="item.id" class="plan-history-item" type="button" :class="{ selected: plan?.id === item.id }" @click="openHistory(item)">
            <span><strong>{{ item.title }}</strong><small>{{ item.startDate }} 至 {{ item.endDate }}</small></span>
            <em>{{ item.status === 'ACTIVE' ? '进行中' : '已归档' }}</em>
          </button>
        </div>
        <button v-if="plan" class="archive-plan-btn" type="button" @click="archiveCurrentPlan">归档当前计划</button>
      </div>
    </section>

    <!-- ============ RESULTS ============ -->
    <section class="plan-results-section">
      <!-- Loading -->
      <LoadingWait
        v-if="isGenerating"
        :active="isGenerating"
        :stages="MEAL_PLAN_LOADING_STAGES"
        hint="模型响应通常需要 1–3 分钟，请保持页面打开。"
        class="plan-loading-state"
      />

      <!-- Error -->
      <div v-else-if="generationError && !plan" class="error-state">
        <div class="error-icon"><FcIcon name="warning" :size="48" /></div>
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

      <!-- Empty (initial) — desktop only; mobile 已在上方看到表单 -->
      <div v-else class="empty-state plan-results-empty">
        <div class="empty-icon"><FcIcon name="plan" :size="48" /></div>
        <h3>填写左侧条件后生成</h3>
        <p>会根据你的库存和临期食材安排菜单，并列出需要补购的食材。</p>
      </div>
    </section>
  </main>
</template>
