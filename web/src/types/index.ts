/* Types matching the FridgeClear REST API contract (docs/api-contract.md) */

// --- Shared ---
export interface PageResponse<T> {
  items: T[]
  page: number
  size: number
  total: number
}

export interface ApiResponse<T> {
  code: string
  message: string
  data: T
  requestId?: string
  fieldErrors?: { field: string; message: string }[]
}

// --- Pantry ---
export type PantryItemStatus = 'AVAILABLE' | 'USED_UP' | 'EXPIRED' | 'DISCARDED'

export interface PantryItem {
  id: number
  rawName: string
  canonicalName?: string | null
  ingredientId?: number | null
  ingredientName?: string | null
  quantity: number | null
  unit: string | null
  purchaseDate: string | null
  expireDate: string | null
  status: PantryItemStatus
  expiringSoon?: boolean
  isExpiringSoon?: boolean
  note: string | null
}

// --- Recipe ---
export interface RecipeSummary {
  id: number
  name: string
  category: string
  description: string | null
  difficultyText: string | null
  difficultyLevel: number | null
  calories: number | null
  coverImageUrl: string | null
  ingredientCount: number
  /** Client-side computed: match percentage with current pantry */
  matchPercent?: number
  /** Client-side computed: estimated cooking time in minutes */
  cookingMinutes?: number
  /** Client-side tag for display */
  tag?: string
}

export interface RecipeIngredient {
  id: number
  name: string
  role: 'MAIN' | 'SEASONING' | 'TOOL' | 'UNKNOWN'
  isOptional: boolean
  rawQuantity: string | null
  quantityMin: number | null
  quantityMax: number | null
  unit: string | null
  quantityParseStatus: 'PARSED' | 'PARTIAL' | 'UNPARSED'
}

export interface RecipeStep {
  stepNo: number
  content: string
}

export interface RecipeMedia {
  id: number
  mediaType: 'IMAGE'
  sourcePath: string
  altText: string | null
  sortOrder: number
}

export interface RecipeDetail extends RecipeSummary {
  ingredients: RecipeIngredient[]
  steps: RecipeStep[]
  media: RecipeMedia[]
  source: {
    repository: string
    path: string
    commit: string
  }
}

// --- Meal Plan ---
export type MealPlanStatus = 'DRAFT' | 'ACTIVE' | 'COMPLETED' | 'ARCHIVED'
export type MealType = 'BREAKFAST' | 'LUNCH' | 'DINNER' | 'SNACK'
export type MealPlanItemStatus = 'PLANNED' | 'COOKED' | 'SKIPPED'

export interface MealPlanGenerateRequest {
  days: number
  peopleCount: number
  maxCookingMinutes: number
  mealTypes: MealType[]
  dietaryPreference?: string
  dislikedIngredients?: string[]
  availableAppliances?: string[]
  usePantryItemIds?: number[]
  preferredRecipeIds?: number[]
}

export interface ExpiringIngredient {
  pantryItemId: number
  name: string
  expireDate: string
  reason: string
}

export interface MealPlanItem {
  id: number
  planDate: string
  mealType: MealType
  recipe: { id: number; name: string; cookingMinutes?: number; coverImageUrl?: string | null }
  servings: number
  usedIngredients: string[]
  missingIngredients: string[]
  reason: string
  status: MealPlanItemStatus
}

export interface ShoppingListItem {
  id?: number
  name: string
  quantity: number | null
  unit: string | null
  reason: string | null
  status: 'TODO' | 'PURCHASED' | 'SKIPPED'
}

export interface MealPlan {
  id: number
  title: string
  startDate: string
  endDate: string
  status: MealPlanStatus
  constraintsJson?: MealPlanGenerateRequest
  summary?: string
  expiringIngredients?: ExpiringIngredient[]
  items?: MealPlanItem[]
  shoppingList?: ShoppingListItem[]
  createdAt?: string
}
