import http from './http'
import type { ApiResponse, PageResponse, RecipeSummary } from '../types'

export async function getFavoriteRecipeIds() {
  const response = await http.get<ApiResponse<{ recipeIds: number[] }>>('/favorites/ids')
  return response.data.data.recipeIds
}

export async function getFavoriteRecipes(params: { page?: number; size?: number } = {}) {
  const response = await http.get<ApiResponse<PageResponse<RecipeSummary>>>('/favorites', { params })
  return response.data.data
}

export async function addFavoriteRecipe(recipeId: number) {
  await http.post(`/favorites/${recipeId}`)
}

export async function removeFavoriteRecipe(recipeId: number) {
  await http.delete(`/favorites/${recipeId}`)
}
