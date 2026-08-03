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
      component: () => import('../views/MealPlan.vue'),
    },
    {
      path: '/recipes',
      name: 'recipes',
      component: () => import('../views/Dashboard.vue'), // placeholder
    },
    {
      path: '/pantry',
      name: 'pantry',
      component: () => import('../views/Dashboard.vue'), // placeholder
    },
  ],
})

export default router
