import { defineStore } from 'pinia'
import { ref } from 'vue'
import { listMealPlans } from '../api/mealPlans'
import type { MealPlan } from '../types'
import { isCacheFresh } from '../utils/cache'

const MEAL_PLAN_TTL_MS = 30_000

export type MealPlanListItem = {
  id: number
  title: string
  startDate: string
  endDate: string
  status: MealPlan['status']
}

export const useMealPlansStore = defineStore('mealPlans', () => {
  const history = ref<MealPlanListItem[]>([])
  const loading = ref(false)
  const fetchedAt = ref<number | null>(null)

  async function fetchHistory(options: { force?: boolean } = {}) {
    const { force = false } = options
    if (!force && fetchedAt.value !== null && isCacheFresh(fetchedAt.value, MEAL_PLAN_TTL_MS)) {
      return history.value
    }

    const showLoading = fetchedAt.value === null
    if (showLoading) loading.value = true
    try {
      history.value = (await listMealPlans()).items
      fetchedAt.value = Date.now()
      return history.value
    } catch {
      if (fetchedAt.value === null) history.value = []
      return history.value
    } finally {
      if (showLoading) loading.value = false
    }
  }

  function invalidate() {
    fetchedAt.value = null
  }

  function reset() {
    history.value = []
    loading.value = false
    fetchedAt.value = null
  }

  return { history, loading, fetchHistory, invalidate, reset }
})
