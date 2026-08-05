<script setup lang="ts">
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const navItems = computed(() => {
  const items = [
    { label: '首页', path: '/', icon: 'home' },
    { label: '菜谱', path: '/recipes', icon: 'recipes' },
    { label: '备餐', path: '/meal-plan', icon: 'plan' },
    { label: '冰箱', path: '/pantry', icon: 'pantry' },
  ]
  if (auth.user?.role === 'ADMIN') items.push({ label: '管理', path: '/admin/ai-config', icon: 'admin' })
  return items
})

const mobileNavItems = computed(() => navItems.value.filter((item) => item.path !== '/admin/ai-config'))

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

function goSearch() {
  router.push('/recipes')
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
      <button class="search-btn" type="button" aria-label="搜索菜谱" @click="goSearch">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="11" cy="11" r="8"/><path d="m21 21-4.35-4.35"/></svg>
        <span>搜索菜谱</span>
      </button>
      <template v-if="auth.isAuthenticated">
        <span class="user-greeting">你好，{{ auth.user?.nickname }}</span>
        <button class="sign-in-btn" type="button" @click="signOut">退出</button>
      </template>
      <button v-else class="sign-in-btn" type="button" @click="navigate('/login')">登录</button>
    </div>
  </header>

  <!-- Mobile bottom tabs -->
  <nav v-if="!hideChrome" class="bottom-nav" aria-label="底部导航">
    <button
      v-for="item in mobileNavItems"
      :key="item.path"
      type="button"
      :class="{ active: activeNav === item.path }"
      @click="navigate(item.path)"
    >
      <span class="bottom-nav-icon" aria-hidden="true">
        <svg v-if="item.icon === 'home'" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="m3 10 9-7 9 7v10a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2Z"/><path d="M9 22V12h6v10"/></svg>
        <svg v-else-if="item.icon === 'recipes'" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/></svg>
        <svg v-else-if="item.icon === 'plan'" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="4" width="18" height="18" rx="2"/><path d="M16 2v4M8 2v4M3 10h18"/></svg>
        <svg v-else width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 7H4a2 2 0 0 0-2 2v10a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V9a2 2 0 0 0-2-2Z"/><path d="M16 7V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v2"/></svg>
      </span>
      <span>{{ item.label }}</span>
    </button>
  </nav>
</template>
