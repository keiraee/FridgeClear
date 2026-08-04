import { createRouter, createWebHashHistory } from 'vue-router'

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    {
      path: '/',
      name: 'dashboard',
      component: () => import('../views/Dashboard.vue'),
    },
    {
      path: '/meal-plan',
      name: 'mealPlan',
      meta: { requiresAuth: true },
      component: () => import('../views/MealPlan.vue'),
    },
    {
      path: '/recipes',
      name: 'recipes',
      meta: { requiresAuth: true },
      component: () => import('../views/RecipesView.vue'),
    },
    {
      path: '/pantry',
      name: 'pantry',
      meta: { requiresAuth: true },
      component: () => import('../views/PantryView.vue'),
    },
    { path: '/login', name: 'login', component: () => import('../views/AuthView.vue') },
    { path: '/register', name: 'register', component: () => import('../views/AuthView.vue') },
  ],
})

router.beforeEach((to) => {
  if (to.meta.requiresAuth && !localStorage.getItem('fridgeclear_access_token')) return '/login'
  if ((to.name === 'login' || to.name === 'register') && localStorage.getItem('fridgeclear_access_token')) return '/'
})

export default router
