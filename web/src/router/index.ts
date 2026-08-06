import { createRouter, createWebHashHistory } from 'vue-router'

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    {
      path: '/',
      name: 'dashboard',
      meta: { keepAlive: true },
      component: () => import('../views/Dashboard.vue'),
    },
    {
      path: '/meal-plan',
      name: 'mealPlan',
      meta: { requiresAuth: true, keepAlive: true },
      component: () => import('../views/MealPlan.vue'),
    },
    {
      path: '/recipes/:id',
      name: 'recipeDetail',
      meta: { requiresAuth: true },
      component: () => import('../views/RecipeDetailView.vue'),
    },
    {
      path: '/recipes',
      name: 'recipes',
      meta: { requiresAuth: true, keepAlive: true },
      component: () => import('../views/RecipesView.vue'),
    },
    {
      path: '/pantry',
      name: 'pantry',
      meta: { requiresAuth: true, keepAlive: true },
      component: () => import('../views/PantryView.vue'),
    },
    {
      path: '/admin/ai-config',
      name: 'adminAiConfig',
      meta: { requiresAuth: true, requiresAdmin: true },
      component: () => import('../views/AdminAiConfig.vue'),
    },
    {
      path: '/admin/access-logs',
      name: 'adminAccessLogs',
      meta: { requiresAuth: true, requiresAdmin: true },
      component: () => import('../views/AdminAccessLogs.vue'),
    },
    { path: '/login', name: 'login', component: () => import('../views/AuthView.vue') },
    { path: '/register', name: 'register', component: () => import('../views/AuthView.vue') },
  ],
})

router.beforeEach((to) => {
  const token = localStorage.getItem('fridgeclear_access_token')
  if (to.meta.requiresAuth && !token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.meta.requiresAdmin) {
    const raw = localStorage.getItem('fridgeclear_user')
    const user = raw ? (JSON.parse(raw) as { role?: string }) : null
    if (!user || user.role !== 'ADMIN') return '/'
  }
  if ((to.name === 'login' || to.name === 'register') && token) return '/'
})

export default router
