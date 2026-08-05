<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useRecipesStore } from '../stores/recipes'
import type { RecipeDetail, RecipeIngredient } from '../types'

defineOptions({ name: 'RecipeDetail' })

const route = useRoute()
const router = useRouter()
const recipesStore = useRecipesStore()

const recipeId = computed(() => Number(route.params.id))
const recipe = ref<RecipeDetail | null>(null)
const loading = ref(true)
const errorMessage = ref('')

const coverUrl = computed(() => {
  const detail = recipe.value
  if (!detail) return null
  if (detail.coverImageUrl) return detail.coverImageUrl
  const first = detail.media?.[0]
  if (!first) return null
  return `/api/v1/recipes/${detail.id}/media/${first.sortOrder}`
})

const metaLine = computed(() => {
  const detail = recipe.value
  if (!detail) return ''
  const parts: string[] = []
  if (detail.category) parts.push(detail.category)
  if (detail.difficultyText) parts.push(detail.difficultyText)
  if (detail.calories != null) parts.push(`${detail.calories} kcal`)
  return parts.join(' · ')
})

const ROLE_LABELS: Record<RecipeIngredient['role'], string> = {
  MAIN: '主料',
  SEASONING: '调料',
  TOOL: '厨具',
  UNKNOWN: '其他',
}

const ingredientGroups = computed(() => {
  const detail = recipe.value
  if (!detail?.ingredients?.length) return []
  const order: RecipeIngredient['role'][] = ['MAIN', 'SEASONING', 'TOOL', 'UNKNOWN']
  const grouped = new Map<RecipeIngredient['role'], RecipeIngredient[]>()
  for (const item of detail.ingredients) {
    const list = grouped.get(item.role) ?? []
    list.push(item)
    grouped.set(item.role, list)
  }
  return order
    .filter((role) => grouped.has(role))
    .map((role) => ({ role, label: ROLE_LABELS[role], items: grouped.get(role)! }))
})

const sourceLabel = computed(() => {
  const source = recipe.value?.source
  if (!source) return ''
  return source.path || source.repository
})

function formatQuantity(item: RecipeIngredient) {
  if (item.rawQuantity) return item.rawQuantity
  const min = item.quantityMin
  const max = item.quantityMax
  const unit = item.unit ?? ''
  if (min != null && max != null && min !== max) return `${min}-${max}${unit}`
  if (min != null) return `${min}${unit}`
  if (max != null) return `${max}${unit}`
  return ''
}

async function loadDetail(force = false) {
  if (!Number.isFinite(recipeId.value) || recipeId.value <= 0) {
    loading.value = false
    errorMessage.value = '无效的菜谱链接'
    recipe.value = null
    return
  }

  const cached = recipesStore.getCachedDetail(recipeId.value)
  if (cached && !force) {
    recipe.value = cached
    errorMessage.value = recipesStore.getDetailError(recipeId.value)
    loading.value = false
    window.scrollTo({ top: 0, behavior: 'smooth' })
    return
  }

  loading.value = true
  errorMessage.value = ''
  const result = await recipesStore.fetchDetail(recipeId.value, { force })
  recipe.value = result
  errorMessage.value = recipesStore.getDetailError(recipeId.value)
  loading.value = false
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function goBack() {
  if (window.history.length > 1) router.back()
  else router.push('/recipes')
}

function goToPlan() {
  router.push('/meal-plan')
}

onMounted(() => loadDetail())
watch(recipeId, () => loadDetail())
</script>

<template>
  <main class="page-main recipe-detail-page">
    <button class="detail-back" type="button" @click="goBack">← 返回</button>

    <p v-if="loading" class="loading-copy">正在加载菜谱详情…</p>
    <p v-else-if="errorMessage && !recipe" class="error-copy">{{ errorMessage }}</p>

    <template v-else-if="recipe">
      <section class="detail-hero">
        <div class="detail-cover" :class="{ 'has-image': !!coverUrl }">
          <img v-if="coverUrl" :src="coverUrl" :alt="recipe.name" />
          <div v-else class="detail-cover-fallback" aria-hidden="true">🍳</div>
        </div>
        <div class="detail-intro">
          <p class="overline">RECIPE DETAIL</p>
          <h1>{{ recipe.name }}</h1>
          <p v-if="metaLine" class="detail-meta">{{ metaLine }}</p>
          <p v-if="recipe.description" class="detail-desc">{{ recipe.description }}</p>
          <button class="cta-primary detail-cta" type="button" @click="goToPlan">
            ＋ 加入备餐计划
          </button>
        </div>
      </section>

      <section v-if="ingredientGroups.length" class="detail-section">
        <h2>食材清单</h2>
        <div class="detail-ingredient-groups">
          <div v-for="group in ingredientGroups" :key="group.role" class="detail-ingredient-group">
            <h3>{{ group.label }}</h3>
            <ul>
              <li v-for="item in group.items" :key="item.id">
                <span class="ingredient-name">{{ item.name }}</span>
                <span v-if="formatQuantity(item)" class="ingredient-qty">{{ formatQuantity(item) }}</span>
                <span v-if="item.isOptional" class="ingredient-optional">可选</span>
              </li>
            </ul>
          </div>
        </div>
      </section>

      <section v-if="recipe.steps?.length" class="detail-section">
        <h2>烹饪步骤</h2>
        <ol class="detail-steps">
          <li v-for="step in recipe.steps" :key="step.stepNo">
            <span class="step-no">{{ step.stepNo }}</span>
            <p>{{ step.content }}</p>
          </li>
        </ol>
      </section>

      <section v-if="sourceLabel" class="detail-section detail-source">
        <h2>来源</h2>
        <p>
          本菜谱来自
          <a
            v-if="recipe.source?.repository"
            :href="`https://github.com/${recipe.source.repository}`"
            target="_blank"
            rel="noopener noreferrer"
          >HowToCook</a>
          <template v-else>HowToCook</template>
          · {{ sourceLabel }}
        </p>
      </section>
    </template>
  </main>
</template>

<style scoped>
.recipe-detail-page {
  max-width: 920px;
}

.detail-back {
  margin: 0 0 20px;
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--sage);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}

.detail-back:hover {
  color: var(--deep-green);
}

.detail-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(0, 1fr);
  gap: 28px;
  margin-bottom: 36px;
}

.detail-cover {
  aspect-ratio: 4 / 3;
  border-radius: var(--radius-lg);
  overflow: hidden;
  background: linear-gradient(145deg, var(--sage-light), #f6efe8);
  display: flex;
  align-items: center;
  justify-content: center;
}

.detail-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.detail-cover-fallback {
  font-size: 72px;
}

.detail-intro h1 {
  margin: 8px 0 10px;
  font-size: clamp(28px, 4vw, 40px);
  line-height: 1.15;
}

.detail-meta {
  margin: 0 0 12px;
  color: var(--gray-text);
  font-size: 14px;
}

.detail-desc {
  margin: 0 0 20px;
  color: var(--gray-text);
  line-height: 1.65;
}

.detail-cta {
  width: fit-content;
}

.detail-section {
  margin-bottom: 32px;
  padding-top: 8px;
  border-top: 1px solid var(--sage-border);
}

.detail-section h2 {
  margin: 0 0 16px;
  font-size: 22px;
}

.detail-ingredient-groups {
  display: grid;
  gap: 20px;
}

.detail-ingredient-group h3 {
  margin: 0 0 10px;
  font-size: 14px;
  color: var(--sage);
  letter-spacing: 0.04em;
}

.detail-ingredient-group ul {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: 8px;
}

.detail-ingredient-group li {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 8px;
  padding: 10px 14px;
  border: 1px solid var(--sage-border);
  border-radius: var(--radius-md);
}

.ingredient-name {
  font-weight: 600;
  color: var(--deep-green);
}

.ingredient-qty {
  color: var(--gray-text);
  font-size: 14px;
}

.ingredient-optional {
  font-size: 11px;
  font-weight: 700;
  color: var(--gray-muted);
  background: var(--cream);
  padding: 2px 8px;
  border-radius: 10px;
}

.detail-steps {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: 14px;
}

.detail-steps li {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 14px;
  align-items: start;
  padding: 14px 16px;
  border: 1px solid var(--sage-border);
  border-radius: var(--radius-md);
}

.step-no {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--sage-light);
  color: var(--sage);
  font-size: 13px;
  font-weight: 700;
}

.detail-steps p {
  margin: 4px 0 0;
  line-height: 1.65;
  color: var(--gray-text);
}

.detail-source p {
  margin: 0;
  color: var(--gray-text);
  font-size: 14px;
  line-height: 1.6;
}

.detail-source a {
  color: var(--light-orange);
  text-decoration: none;
  font-weight: 600;
}

.detail-source a:hover {
  text-decoration: underline;
}

@media (max-width: 768px) {
  .detail-hero {
    grid-template-columns: 1fr;
  }
}
</style>
