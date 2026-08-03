import type { MealPlan, MealPlanGenerateRequest } from '../types'

export const defaultPlanConfig: MealPlanGenerateRequest = {
  days: 3,
  peopleCount: 2,
  maxCookingMinutes: 45,
  mealTypes: ['DINNER'],
  dietaryPreference: '',
  dislikedIngredients: [],
  availableAppliances: ['炒锅', '电饭煲', '蒸锅'],
  usePantryItemIds: [1, 2, 3, 4, 5, 6, 7, 8, 10],
}

export const mockGeneratedPlan: MealPlan = {
  id: 2001,
  title: '我的 3 天晚餐计划',
  startDate: '2026-08-03',
  endDate: '2026-08-05',
  status: 'ACTIVE',
  summary: '优先消耗西红柿、虾仁和豆腐，3 天预计需要额外采购 4 种食材。',
  expiringIngredients: [
    { pantryItemId: 1, name: '西红柿', expireDate: '2026-08-04', reason: '临近过期，安排在第一天使用' },
    { pantryItemId: 3, name: '虾仁', expireDate: '2026-08-05', reason: '冷冻虾仁，建议尽快食用' },
    { pantryItemId: 4, name: '豆腐', expireDate: '2026-08-04', reason: '新鲜豆腐保质期短，安排在第一天' },
  ],
  items: [
    {
      id: 3001,
      planDate: '2026-08-03',
      mealType: 'DINNER',
      recipe: { id: 102, name: '番茄虾仁意面', cookingMinutes: 25 },
      servings: 2,
      usedIngredients: ['西红柿', '虾仁', '意面', '蒜'],
      missingIngredients: ['黄油', '芝士粉'],
      reason: '库存匹配度 85%，优先消耗即将过期的西红柿和虾仁',
      status: 'PLANNED',
    },
    {
      id: 3002,
      planDate: '2026-08-04',
      mealType: 'DINNER',
      recipe: { id: 104, name: '清爽番茄豆腐汤', cookingMinutes: 20 },
      servings: 2,
      usedIngredients: ['西红柿', '豆腐', '鸡蛋'],
      missingIngredients: ['小葱'],
      reason: '清淡搭配前一天的西式餐点，消耗剩余西红柿和豆腐',
      status: 'PLANNED',
    },
    {
      id: 3003,
      planDate: '2026-08-05',
      mealType: 'DINNER',
      recipe: { id: 106, name: '青椒肉丝', cookingMinutes: 15 },
      servings: 2,
      usedIngredients: ['青椒', '猪肉', '蒜'],
      missingIngredients: ['生抽', '淀粉'],
      reason: '快速家常菜，库存匹配良好',
      status: 'PLANNED',
    },
  ],
  shoppingList: [
    { name: '黄油', quantity: 50, unit: 'g', reason: '番茄虾仁意面使用', status: 'TODO' },
    { name: '芝士粉', quantity: 30, unit: 'g', reason: '番茄虾仁意面使用', status: 'TODO' },
    { name: '小葱', quantity: 2, unit: '根', reason: '番茄豆腐汤使用', status: 'TODO' },
    { name: '生抽', quantity: 1, unit: '瓶', reason: '青椒肉丝使用', status: 'TODO' },
    { name: '淀粉', quantity: 1, unit: '袋', reason: '青椒肉丝使用', status: 'TODO' },
  ],
  createdAt: '2026-08-03T10:30:00+08:00',
}

/** Simulate AI generation delay */
export function simulatePlanGeneration(): Promise<MealPlan> {
  return new Promise((resolve) => {
    setTimeout(() => resolve({ ...mockGeneratedPlan, id: Date.now() }), 1500)
  })
}
