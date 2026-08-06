import http from './http'
import type { ApiResponse, MealPlan, MealPlanGenerateRequest, MealPlanItemStatus } from '../types'

interface BackendPlan {
  mealPlanId: number
  summary: string
  expiringIngredients: MealPlan['expiringIngredients']
  items: NonNullable<MealPlan['items']>
  shoppingList: NonNullable<MealPlan['shoppingList']>
}

interface BackendPlanList {
  content: Array<{ id: number; title: string; startDate: string; endDate: string; status: MealPlan['status'] }>
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface MealPlanList {
  items: BackendPlanList['content']
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export type MealPlanTaskStatus = 'RUNNING' | 'SUCCESS' | 'FAILED'

export interface MealPlanTask {
  taskId: number
  status: MealPlanTaskStatus
  mealPlanId?: number | null
  errorMessage?: string | null
  result?: BackendPlan | null
}

const GENERATION_POLL_INTERVAL_MS = 2000
const GENERATION_TIMEOUT_MS = 180000

function sleep(ms: number) {
  return new Promise<void>((resolve) => {
    window.setTimeout(resolve, ms)
  })
}

function mapBackendPlan(data: BackendPlan): MealPlan {
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

export async function submitMealPlanGeneration(payload: MealPlanGenerateRequest): Promise<number> {
  const response = await http.post<ApiResponse<{ taskId: number }>>('/meal-plans/generate', payload, { timeout: 15000 })
  return response.data.data.taskId
}

export async function getMealPlanTask(taskId: number): Promise<MealPlanTask> {
  const response = await http.get<ApiResponse<MealPlanTask>>(`/meal-plans/generate/tasks/${taskId}`, { timeout: 15000 })
  return response.data.data
}

export async function generateMealPlan(
  payload: MealPlanGenerateRequest,
  options?: { signal?: AbortSignal },
): Promise<MealPlan> {
  const taskId = await submitMealPlanGeneration(payload)
  const deadline = Date.now() + GENERATION_TIMEOUT_MS

  while (Date.now() < deadline) {
    if (options?.signal?.aborted) {
      throw new DOMException('生成已取消', 'AbortError')
    }

    const task = await getMealPlanTask(taskId)
    if (task.status === 'SUCCESS') {
      if (task.result) return mapBackendPlan(task.result)
      if (task.mealPlanId) return getMealPlan(task.mealPlanId)
      throw new Error('生成成功但未返回备餐计划')
    }
    if (task.status === 'FAILED') {
      const error = new Error(task.errorMessage ?? '生成失败，请稍后重试。') as Error & { code?: string }
      if (task.errorMessage?.includes('AI 服务暂不可用')) {
        error.code = 'AI_SERVICE_UNAVAILABLE'
      }
      throw error
    }

    await sleep(GENERATION_POLL_INTERVAL_MS)
  }

  const timeoutError = new Error('AI 响应时间较长，请稍后重试或检查模型服务状态。') as Error & { code?: string }
  timeoutError.code = 'POLL_TIMEOUT'
  throw timeoutError
}

export async function listMealPlans(): Promise<MealPlanList> {
  const response = await http.get<ApiResponse<BackendPlanList>>('/meal-plans', { params: { page: 0, size: 20 } })
  const data = response.data.data
  return { items: data.content, page: data.page, size: data.size, totalElements: data.totalElements, totalPages: data.totalPages }
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
