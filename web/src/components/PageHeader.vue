<script setup lang="ts">
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const navItems = computed(() => {
  const items = [
    { label: '首页', path: '/' },
    { label: '菜谱', path: '/recipes' },
    { label: '备餐计划', path: '/meal-plan' },
    { label: '我的冰箱', path: '/pantry' },
  ]
  if (auth.user?.role === 'ADMIN') items.push({ label: '管理', path: '/admin/ai-config' })
  return items
})

const activeNav = computed(() => {
  if (route.path === '/') return '/'
  const matched = navItems.value.filter((n) => n.path !== '/' && route.path.startsWith(n.path))
  matched.sort((a, b) => b.path.length - a.path.length)
  return matched[0]?.path ?? '/'
})

function navigate(path: string) {
  router.push(path)
}

function signOut() {
  auth.signOut()
  router.push('/login')
}
</script>

<template>
  <header class="site-header">
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
      <button class="search-btn" type="button" aria-label="搜索">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="11" cy="11" r="8"/><path d="m21 21-4.35-4.35"/></svg>
        <span>搜索菜谱和食材</span>
      </button>
      <button class="icon-btn" type="button" aria-label="收藏">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z"/></svg>
      </button>
      <template v-if="auth.isAuthenticated">
        <span class="user-greeting">你好，{{ auth.user?.nickname }}</span>
        <button class="sign-in-btn" type="button" @click="signOut">退出</button>
      </template>
      <button v-else class="sign-in-btn" type="button" @click="navigate('/login')">登录</button>
    </div>
  </header>
</template>
