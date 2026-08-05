import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getRecipes } from '../api/recipes'
import type { RecipeSummary } from '../types'
import { isCacheFresh } from '../utils/cache'

export const RECIPES_PAGE_SIZE = 12
const RECIPES_TTL_MS = 5 * 60_000

export const CATEGORY_LABELS: Record<string, string> = {
  AQUATIC: '水产',
  BREAKFAST: '早餐',
  CONDIMENT: '调味品',
  DESSERT: '甜点',
  DRINK: '饮品',
  MEAT_DISH: '肉菜',
  SEMI_FINISHED: '半成品',
  SOUP: '汤羹',
  STAPLE: '主食',
  VEGETABLE_DISH: '素菜',
  UNKNOWN: '其他',
}

export interface RecipeQuery {
  keyword?: string
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

export const useRecipesStore = defineStore('recipes', () => {
  const caches = ref(new Map<string, CacheEntry>())
  const loadingKeys = ref(new Set<string>())
  const errors = ref(new Map<string, string>())

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

  function invalidateAll() {
    caches.value.clear()
    errors.value.clear()
  }

  function reset() {
    caches.value.clear()
    loadingKeys.value.clear()
    errors.value.clear()
  }

  return { getCachedPage, getError, fetchPage, fetchPreview, invalidateAll, reset }
})
