import http from './http'
import type { ApiResponse } from '../types'

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
}

export interface RecipeMatchResponse {
  pantryIngredientCount: number
  recipes: RecipeMatch[]
}

export async function getRecommendedRecipes(limit = 8) {
  const response = await http.get<ApiResponse<RecipeMatchResponse>>('/recommendations/recipes', {
    params: { limit },
    timeout: 60000,
  })
  return response.data.data
}
