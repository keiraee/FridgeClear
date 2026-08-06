import { defineStore } from 'pinia'
import { ref, watch } from 'vue'

const STORAGE_KEY = 'fc-meal-plan-queue'

export type QueuedRecipe = {
  id: number
  name: string
  coverImageUrl?: string | null
}

function loadQueue(): QueuedRecipe[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return []
    const parsed = JSON.parse(raw) as QueuedRecipe[]
    if (!Array.isArray(parsed)) return []
    return parsed.filter((item) => Number.isFinite(item.id) && item.id > 0 && item.name)
  } catch {
    return []
  }
}

export const useMealPlanQueueStore = defineStore('mealPlanQueue', () => {
  const recipes = ref<QueuedRecipe[]>(loadQueue())

  watch(recipes, (value) => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(value))
  }, { deep: true })

  function add(recipe: QueuedRecipe) {
    if (recipes.value.some((item) => item.id === recipe.id)) return
    recipes.value.push(recipe)
  }

  function remove(id: number) {
    recipes.value = recipes.value.filter((item) => item.id !== id)
  }

  function clear() {
    recipes.value = []
  }

  function recipeIds() {
    return recipes.value.map((item) => item.id)
  }

  return { recipes, add, remove, clear, recipeIds }
})
