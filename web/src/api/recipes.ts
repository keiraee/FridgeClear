import http from './http'
import type { ApiResponse, PageResponse, RecipeSummary } from '../types'

export async function getRecipes(params: { keyword?: string; page?: number; size?: number } = {}) {
  const response = await http.get<ApiResponse<PageResponse<RecipeSummary>>>('/recipes', {
    params,
    timeout: 120000,
  })
  return response.data.data
}
