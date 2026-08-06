import http from './http'
import type { ApiResponse } from '../types'

export type RecommendationFilter = 'all' | 'high_match' | 'ready_now'

export interface RecipeMatch {
  recipeId: number
  recipeName: string
  category: string
  description: string | null
  difficultyText: string | null
  difficultyLevel: number | null
  calories: number | null
  coverImageUrl: string | null
  ingredientCount: number
  matchedIngredientCount: number
  requiredIngredientCount: number
  missingIngredientCount: number
  matchRate: number
  matchedIngredients: string[]
  missingIngredients: string[]
  expiringMatchedIngredients: string[]
  expiringMatchedCount: number
  readyToCook: boolean
}

export interface RecipeMatchResponse {
  pantryIngredientCount: number
  recipes: RecipeMatch[]
}

export async function getRecommendedRecipes(limit = 8, filter: RecommendationFilter = 'ready_now') {
  const response = await http.get<ApiResponse<RecipeMatchResponse>>('/recommendations/recipes', {
    params: { limit, filter },
    timeout: 60000,
  })
  return response.data.data
}

export async function getRecipeMatch(recipeId: number) {
  const response = await http.get<ApiResponse<RecipeMatch | null>>(`/recommendations/recipes/${recipeId}/match`, {
    timeout: 30000,
  })
  return response.data.data
}
