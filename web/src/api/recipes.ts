import http from './http'
import type { ApiResponse, PageResponse, RecipeDetail, RecipeSummary } from '../types'

export async function getRecipes(params: { keyword?: string; category?: string; page?: number; size?: number } = {}) {
  const response = await http.get<ApiResponse<PageResponse<RecipeSummary>>>('/recipes', {
    params,
    timeout: 120000,
  })
  return response.data.data
}

export async function getRecipeDetail(id: number) {
  const response = await http.get<ApiResponse<RecipeDetail>>(`/recipes/${id}`, { timeout: 60000 })
  return response.data.data
}
