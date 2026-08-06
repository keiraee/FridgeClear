import { ref } from 'vue'
import { defineStore } from 'pinia'
import {
  addFavoriteRecipe,
  getFavoriteRecipeIds,
  removeFavoriteRecipe,
} from '../api/favorites'
import { useAuthStore } from './auth'

function showToast(message: string) {
  window.dispatchEvent(new CustomEvent('fridgeclear:toast', { detail: message }))
}

export const useFavoritesStore = defineStore('favorites', () => {
  const ids = ref<Set<number>>(new Set())
  const loaded = ref(false)
  const loading = ref(false)
  const syncTargets = new Map<number, boolean>()
  const syncTasks = new Map<number, Promise<void>>()

  function isFavorite(recipeId: number) {
    return ids.value.has(recipeId)
  }

  function setFavorite(recipeId: number, favorited: boolean) {
    const next = new Set(ids.value)
    if (favorited) next.add(recipeId)
    else next.delete(recipeId)
    ids.value = next
  }

  async function loadIds() {
    const auth = useAuthStore()
    if (!auth.isAuthenticated) {
      reset()
      return
    }
    loading.value = true
    try {
      const recipeIds = await getFavoriteRecipeIds()
      ids.value = new Set(recipeIds)
      loaded.value = true
    } finally {
      loading.value = false
    }
  }

  async function flushSync(recipeId: number) {
    while (syncTargets.has(recipeId)) {
      const target = syncTargets.get(recipeId)!
      syncTargets.delete(recipeId)
      try {
        if (target) await addFavoriteRecipe(recipeId)
        else await removeFavoriteRecipe(recipeId)
        showToast(target ? '已收藏' : '已取消收藏')
      } catch {
        try {
          const recipeIds = await getFavoriteRecipeIds()
          ids.value = new Set(recipeIds)
        } catch {
          // 回滚失败时保持当前 UI，避免二次打断
        }
        showToast('收藏操作失败，请稍后重试')
        return
      }
    }
  }

  function scheduleSync(recipeId: number, favorited: boolean) {
    syncTargets.set(recipeId, favorited)
    if (!syncTasks.has(recipeId)) {
      const task = flushSync(recipeId).finally(() => {
        syncTasks.delete(recipeId)
      })
      syncTasks.set(recipeId, task)
    }
  }

  function toggle(recipeId: number) {
    const auth = useAuthStore()
    if (!auth.isAuthenticated) {
      window.dispatchEvent(new CustomEvent('fridgeclear:login-required', {
        detail: '登录后可收藏菜谱',
      }))
      return
    }

    const favorited = !ids.value.has(recipeId)
    setFavorite(recipeId, favorited)
    scheduleSync(recipeId, favorited)
  }

  function reset() {
    ids.value = new Set()
    loaded.value = false
    loading.value = false
    syncTargets.clear()
    syncTasks.clear()
  }

  return { ids, loaded, loading, isFavorite, loadIds, toggle, reset }
})
