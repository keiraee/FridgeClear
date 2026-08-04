import http from './http'
import type { ApiResponse, MealPlan, MealPlanGenerateRequest, MealPlanItemStatus } from '../types'

interface BackendPlan {
  mealPlanId: number
  summary: string
  expiringIngredients: MealPlan['expiringIngredients']
  items: NonNullable<MealPlan['items']>
  shoppingList: NonNullable<MealPlan['shoppingList']>
}

interface MealPlanListResponse {
  items: Array<{ id: number; title: string; startDate: string; endDate: string; status: MealPlan['status'] }>
  page: number
  size: number
  total: number
}

export async function generateMealPlan(payload: MealPlanGenerateRequest): Promise<MealPlan> {
  // AI 需要读取候选菜谱并等待模型响应，不能使用普通接口的 30 秒超时。
  const response = await http.post<ApiResponse<BackendPlan>>('/meal-plans/generate', payload, { timeout: 180000 })
  const data = response.data.data
  return {
    id: data.mealPlanId,
    title: 'AI 冰箱消耗计划',
    startDate: data.items[0]?.planDate ?? new Date().toISOString().slice(0, 10),
    endDate: data.items.at(-1)?.planDate ?? new Date().toISOString().slice(0, 10),
    status: 'ACTIVE',
    summary: data.summary,
    expiringIngredients: data.expiringIngredients,
    items: data.items,
    shoppingList: data.shoppingList,
  }
}

export async function listMealPlans() {
  const response = await http.get<ApiResponse<MealPlanListResponse>>('/meal-plans', { params: { page: 0, size: 20 } })
  return response.data.data
}

export async function getMealPlan(id: number): Promise<MealPlan> {
  const response = await http.get<ApiResponse<{
    id: number; title: string; startDate: string; endDate: string; status: MealPlan['status'];
    items: NonNullable<MealPlan['items']>; shoppingList: NonNullable<MealPlan['shoppingList']>
  }>>(`/meal-plans/${id}`)
  const data = response.data.data
  return { ...data, id: data.id, title: data.title, startDate: data.startDate, endDate: data.endDate, status: data.status }
}

export async function archiveMealPlan(id: number) {
  await http.delete(`/meal-plans/${id}`)
}

export async function updateMealPlanItemStatus(planId: number, itemId: number, status: MealPlanItemStatus) {
  const response = await http.patch<ApiResponse<NonNullable<MealPlan['items']>[number]>>(
    `/meal-plans/${planId}/items/${itemId}/status`, { status },
  )
  return response.data.data
}

export async function updateShoppingItemStatus(itemId: number, status: 'TODO' | 'PURCHASED' | 'SKIPPED') {
  const response = await http.patch<ApiResponse<NonNullable<MealPlan['shoppingList']>[number]>>(
    `/shopping-list-items/${itemId}/status`, { status },
  )
  return response.data.data
}
