import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getRecipes, getRecipeDetail } from '../api/recipes'
import type { RecipeDetail, RecipeSummary } from '../types'
import { isCacheFresh } from '../utils/cache'

export const RECIPES_PAGE_SIZE = 12
const RECIPES_TTL_MS = 5 * 60_000

export const CATEGORY_LABELS: Record<string, string> = {
  AQUATIC: '水产',
  BREAKFAST: '早餐',
  CONDIMENT: '调味品',
  DESSERT: '甜点',
  DRINK: '饮品',
  MEAT_DISH: '荤菜',
  SEMI_FINISHED: '半成品',
  SOUP: '汤羹',
  STAPLE: '主食',
  VEGETABLE_DISH: '素菜',
  UNKNOWN: '其他',
}

export const CATEGORY_OPTIONS = Object.entries(CATEGORY_LABELS)
  .filter(([key]) => key !== 'UNKNOWN' && key !== 'CONDIMENT')
  .map(([value, label]) => ({ value, label }))

export interface RecipeQuery {
  keyword?: string
  category?: string
  page?: number
  size?: number
}

export interface RecipePageResult {
  items: RecipeSummary[]
  page: number
  size: number
  total: number
  hasMore: boolean
}

interface CacheEntry {
  pageResult: RecipePageResult
  fetchedAt: number
}

function queryKey(params: RecipeQuery) {
  return JSON.stringify({
    keyword: params.keyword?.trim() ?? '',
    category: params.category ?? '',
    page: params.page ?? 0,
    size: params.size ?? RECIPES_PAGE_SIZE,
  })
}

function labelRecipe(recipe: RecipeSummary): RecipeSummary {
  return { ...recipe, category: CATEGORY_LABELS[recipe.category] ?? recipe.category }
}

function toPageResult(page: number, size: number, total: number, items: RecipeSummary[]): RecipePageResult {
  return {
    items,
    page,
    size,
    total,
    hasMore: (page + 1) * size < total,
  }
}

function labelDetail(recipe: RecipeDetail): RecipeDetail {
  return { ...recipe, category: CATEGORY_LABELS[recipe.category] ?? recipe.category }
}

export const useRecipesStore = defineStore('recipes', () => {
  const caches = ref(new Map<string, CacheEntry>())
  const detailCaches = ref(new Map<number, { recipe: RecipeDetail; fetchedAt: number }>())
  const loadingKeys = ref(new Set<string>())
  const detailLoadingIds = ref(new Set<number>())
  const errors = ref(new Map<string, string>())
  const detailErrors = ref(new Map<number, string>())

  function getCachedPage(params: RecipeQuery): RecipePageResult | null {
    const entry = caches.value.get(queryKey(params))
    if (!entry || !isCacheFresh(entry.fetchedAt, RECIPES_TTL_MS)) return null
    return entry.pageResult
  }

  function getError(params: RecipeQuery) {
    return errors.value.get(queryKey(params)) ?? ''
  }

  async function fetchPage(params: RecipeQuery, options: { force?: boolean } = {}): Promise<RecipePageResult> {
    const key = queryKey(params)
    const { force = false } = options
    const cached = caches.value.get(key)

    if (!force && cached && isCacheFresh(cached.fetchedAt, RECIPES_TTL_MS)) {
      return cached.pageResult
    }

    if (!cached || force) loadingKeys.value.add(key)
    errors.value.delete(key)

    try {
      const result = await getRecipes(params)
      const items = result.items.map(labelRecipe)
      const pageResult = toPageResult(result.page, result.size, result.total, items)
      caches.value.set(key, { pageResult, fetchedAt: Date.now() })
      return pageResult
    } catch (error) {
      const axiosError = error as { code?: string; response?: { status?: number; data?: { message?: string } } }
      const status = axiosError.response?.status
      let message: string
      if (axiosError.code === 'ECONNABORTED') {
        message = '菜谱加载超时，请稍后重试'
      } else if (status === 401 || status === 403) {
        message = '登录已失效，请重新登录'
      } else {
        message = axiosError.response?.data?.message ?? '菜谱加载失败，请稍后重试'
      }
      errors.value.set(key, message)
      if (cached) return cached.pageResult
      return toPageResult(params.page ?? 0, params.size ?? RECIPES_PAGE_SIZE, 0, [])
    } finally {
      loadingKeys.value.delete(key)
    }
  }

  /** 首页预览：只取第一页少量数据 */
  async function fetchPreview(size = 8, options: { force?: boolean } = {}) {
    return fetchPage({ page: 0, size }, options)
  }

  function getCachedDetail(id: number): RecipeDetail | null {
    const entry = detailCaches.value.get(id)
    if (!entry || !isCacheFresh(entry.fetchedAt, RECIPES_TTL_MS)) return null
    return entry.recipe
  }

  function getDetailError(id: number) {
    return detailErrors.value.get(id) ?? ''
  }

  async function fetchDetail(id: number, options: { force?: boolean } = {}): Promise<RecipeDetail | null> {
    const { force = false } = options
    const cached = detailCaches.value.get(id)
    if (!force && cached && isCacheFresh(cached.fetchedAt, RECIPES_TTL_MS)) {
      return cached.recipe
    }

    if (!cached || force) detailLoadingIds.value.add(id)
    detailErrors.value.delete(id)

    try {
      const recipe = labelDetail(await getRecipeDetail(id))
      detailCaches.value.set(id, { recipe, fetchedAt: Date.now() })
      return recipe
    } catch (error) {
      const axiosError = error as { code?: string; response?: { status?: number; data?: { message?: string } } }
      const status = axiosError.response?.status
      let message: string
      if (status === 404) {
        message = '菜谱不存在或已下架'
      } else if (axiosError.code === 'ECONNABORTED') {
        message = '加载超时，请稍后重试'
      } else if (status === 401 || status === 403) {
        message = '登录已失效，请重新登录'
      } else {
        message = axiosError.response?.data?.message ?? '菜谱详情加载失败'
      }
      detailErrors.value.set(id, message)
      if (cached) return cached.recipe
      return null
    } finally {
      detailLoadingIds.value.delete(id)
    }
  }

  function invalidateAll() {
    caches.value.clear()
    detailCaches.value.clear()
    errors.value.clear()
    detailErrors.value.clear()
  }

  function reset() {
    caches.value.clear()
    detailCaches.value.clear()
    loadingKeys.value.clear()
    detailLoadingIds.value.clear()
    errors.value.clear()
    detailErrors.value.clear()
  }

  return { getCachedPage, getCachedDetail, getError, getDetailError, fetchPage, fetchPreview, fetchDetail, invalidateAll, reset }
})
