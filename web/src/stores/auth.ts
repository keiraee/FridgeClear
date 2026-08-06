import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { currentUser, login, register, type AuthRequest, type UserResponse } from '../api/auth'
import { useFavoritesStore } from './favorites'
import { useMealPlansStore } from './mealPlans'
import { usePantryStore } from './pantry'
import { useRecipesStore } from './recipes'

const TOKEN_KEY = 'fridgeclear_access_token'
const USER_KEY = 'fridgeclear_user'

function readUser(): UserResponse | null {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) return null
  try { return JSON.parse(raw) as UserResponse } catch { return null }
}

export const useAuthStore = defineStore('auth', () => {
  const user = ref<UserResponse | null>(readUser())
  const token = ref(localStorage.getItem(TOKEN_KEY))
  const isAuthenticated = computed(() => Boolean(token.value && user.value))

  function saveAuth(accessToken: string | undefined, nextUser: UserResponse) {
    if (accessToken) {
      token.value = accessToken
      localStorage.setItem(TOKEN_KEY, accessToken)
    }
    user.value = nextUser
    localStorage.setItem(USER_KEY, JSON.stringify(nextUser))
  }

  async function signIn(email: string, password: string) {
    const data = await login({ email, password })
    saveAuth(data.accessToken, data.user)
    await useFavoritesStore().loadIds()
  }

  async function signUp(payload: AuthRequest) {
    const data = await register(payload)
    saveAuth(data.accessToken, data.user)
    await useFavoritesStore().loadIds()
  }

  async function restore() {
    if (!token.value) return
    try {
      saveAuth(token.value, await currentUser())
      await useFavoritesStore().loadIds()
    }
    catch { signOut() }
  }

  function signOut() {
    token.value = null
    user.value = null
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
    usePantryStore().reset()
    useRecipesStore().reset()
    useMealPlansStore().reset()
    useFavoritesStore().reset()
  }

  window.addEventListener('fridgeclear:unauthorized', () => {
    signOut()
    if (window.location.hash !== '#/login' && window.location.hash !== '#/register') {
      window.location.hash = '#/login'
    }
  })
  return { user, token, isAuthenticated, signIn, signUp, restore, signOut }
})
