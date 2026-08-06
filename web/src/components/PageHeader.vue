<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import FcIcon from './FcIcon.vue'
import type { IconName } from '../assets/icons/registry'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const searchQuery = ref('')

watch(
  () => route.query.q,
  (value) => {
    searchQuery.value = typeof value === 'string' ? value : ''
  },
  { immediate: true },
)

const navItems = computed(() => {
  const items: { label: string; path: string; icon: IconName }[] = [
    { label: '首页', path: '/', icon: 'home' },
    { label: '菜谱', path: '/recipes', icon: 'recipe' },
    { label: '备餐', path: '/meal-plan', icon: 'plan' },
    { label: '冰箱', path: '/pantry', icon: 'pantry' },
  ]
  if (auth.user?.role === 'ADMIN') items.push({ label: '管理', path: '/admin/ai-config', icon: 'spark' })
  return items
})

const bottomNavColCount = computed(() => navItems.value.length)

const activeNav = computed(() => {
  if (route.path === '/') return '/'
  const matched = navItems.value.filter((n) => n.path !== '/' && route.path.startsWith(n.path))
  matched.sort((a, b) => b.path.length - a.path.length)
  return matched[0]?.path ?? '/'
})

const hideChrome = computed(() => route.name === 'login' || route.name === 'register')

function navigate(path: string) {
  router.push(path)
}

function submitSearch() {
  const keyword = searchQuery.value.trim()
  if (!auth.isAuthenticated) {
    void router.push('/login')
    return
  }
  void router.push({
    name: 'recipes',
    query: keyword ? { q: keyword } : {},
  })
}

function signOut() {
  auth.signOut()
  router.push('/login')
}
</script>

<template>
  <header v-if="!hideChrome" class="site-header">
    <a class="logo" href="#" @click.prevent="navigate('/')">
      <mark>F</mark> FridgeClear
    </a>

    <nav class="top-nav" aria-label="主导航">
      <button
        v-for="item in navItems"
        :key="item.path"
        type="button"
        :class="{ active: activeNav === item.path }"
        @click="navigate(item.path)"
      >
        {{ item.label }}
      </button>
    </nav>

    <div class="header-actions">
      <form class="global-search" role="search" @submit.prevent="submitSearch">
        <FcIcon name="search" :size="18" class="global-search-icon" />
        <input
          v-model="searchQuery"
          type="search"
          enterkeyhint="search"
          placeholder="搜索菜谱、食材…"
          aria-label="全局搜索菜谱"
        />
      </form>
      <template v-if="auth.isAuthenticated">
        <span class="user-greeting">你好，{{ auth.user?.nickname }}</span>
        <button class="sign-in-btn" type="button" @click="signOut">
          <FcIcon name="logout" :size="16" />
          退出
        </button>
      </template>
      <button v-else class="sign-in-btn" type="button" @click="navigate('/login')">
        <FcIcon name="user" :size="16" />
        登录
      </button>
    </div>
  </header>

  <nav
    v-if="!hideChrome"
    class="bottom-nav"
    :style="{ '--bottom-nav-cols': bottomNavColCount }"
    aria-label="底部导航"
  >
    <button
      v-for="item in navItems"
      :key="item.path"
      type="button"
      :class="{ active: activeNav === item.path }"
      @click="navigate(item.path)"
    >
      <FcIcon :name="item.icon" :size="20" />
      <span>{{ item.label }}</span>
    </button>
  </nav>
</template>
