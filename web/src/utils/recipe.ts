import type { RecipeSummary } from '../types'

type RecipeMeta = Pick<RecipeSummary, 'difficultyLevel' | 'ingredientCount' | 'calories' | 'description'>

export function recipeMediaUrl(recipeId: number, sortOrder: number) {
  return `/api/v1/recipes/${recipeId}/media/${sortOrder}`
}

export function difficultyStars(level: number | null | undefined): string {
  if (!level || level < 1) return ''
  const filled = Math.min(level, 5)
  return `${'★'.repeat(filled)}${'☆'.repeat(5 - filled)}`
}

/** 无烹饪时长字段时，按难度与食材数量粗估 */
export function estimateCookingMinutes(recipe: RecipeMeta): number | null {
  if (recipe.difficultyLevel == null) return null
  const baseByLevel = [15, 25, 40, 60, 90]
  const base = baseByLevel[Math.min(Math.max(recipe.difficultyLevel, 1), 5) - 1] ?? 30
  const extra = Math.min((recipe.ingredientCount ?? 0) * 2, 20)
  return base + extra
}

export function formatCalories(calories: number | null | undefined): string | null {
  if (calories == null) return null
  const value = Number(calories)
  if (!Number.isFinite(value)) return null
  return `${Math.round(value)} 千卡`
}

export function truncateText(text: string | null | undefined, max = 88): string {
  if (!text) return ''
  const clean = text.replace(/\s+/g, ' ').trim()
  if (clean.length <= max) return clean
  return `${clean.slice(0, max)}…`
}

export function resolveCookingMinutes(recipe: RecipeMeta & { cookingMinutes?: number }) {
  return recipe.cookingMinutes ?? estimateCookingMinutes(recipe)
}
