<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useRecipesStore } from '../stores/recipes'
import type { RecipeDetail, RecipeIngredient } from '../types'
import {
  difficultyStars,
  formatCalories,
  recipeMediaUrl,
  resolveCookingMinutes,
} from '../utils/recipe'
import FcIcon from '../components/FcIcon.vue'
import LoadingWait from '../components/LoadingWait.vue'
import { RECIPE_DETAIL_LOADING_STAGES } from '../composables/useElapsedTimer'

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
  return recipeMediaUrl(detail.id, first.sortOrder)
})

const stars = computed(() => difficultyStars(recipe.value?.difficultyLevel))
const caloriesLabel = computed(() => formatCalories(recipe.value?.calories ?? null))
const cookingMinutes = computed(() => (recipe.value ? resolveCookingMinutes(recipe.value) : null))

const stepImages = computed(() => {
  const detail = recipe.value
  if (!detail?.media?.length || !detail.steps?.length) return new Map<number, string>()
  const images = new Map<number, string>()
  const extraMedia = detail.media.length > 1 ? detail.media.slice(1) : detail.media
  extraMedia.forEach((media, index) => {
    const stepNo = detail.steps[index]?.stepNo ?? index + 1
    images.set(stepNo, recipeMediaUrl(detail.id, media.sortOrder))
  })
  return images
})

const galleryImages = computed(() => {
  const detail = recipe.value
  if (!detail?.media?.length) return []
  return detail.media.map((media) => ({
    url: recipeMediaUrl(detail.id, media.sortOrder),
    alt: media.altText || detail.name,
  }))
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
  if (recipe.value) {
    router.push({ path: '/meal-plan', query: { recipeId: String(recipe.value.id) } })
  } else {
    router.push('/meal-plan')
  }
}

onMounted(() => loadDetail())
watch(recipeId, () => loadDetail())
</script>

<template>
  <main class="page-main recipe-detail-page">
    <button class="detail-back" type="button" @click="goBack">
      <FcIcon name="back" :size="16" />
      返回
    </button>

    <LoadingWait
      v-if="loading"
      :active="loading"
      :stages="RECIPE_DETAIL_LOADING_STAGES"
      hint="通常 1–5 秒"
    />
    <p v-else-if="errorMessage && !recipe" class="error-copy">{{ errorMessage }}</p>

    <template v-else-if="recipe">
      <section class="detail-hero">
        <div class="detail-cover" :class="{ 'has-image': !!coverUrl }">
          <img v-if="coverUrl" :src="coverUrl" :alt="recipe.name" />
          <div v-else class="detail-cover-fallback" aria-hidden="true">
            <FcIcon name="chef" :size="48" />
          </div>
        </div>
        <div class="detail-intro">
          <h1>{{ recipe.name }}</h1>
          <div class="detail-stat-row">
            <span v-if="recipe.category" class="detail-stat">{{ recipe.category }}</span>
            <span v-if="stars" class="detail-stat detail-stars" :title="recipe.difficultyText ?? '难度'">{{ stars }}</span>
            <span v-if="caloriesLabel" class="detail-stat">{{ caloriesLabel }}</span>
            <span v-if="cookingMinutes" class="detail-stat">约 {{ cookingMinutes }} 分钟</span>
            <span v-if="recipe.ingredients?.length" class="detail-stat">{{ recipe.ingredients.length }} 种食材</span>
          </div>
          <p v-if="recipe.description" class="detail-desc">{{ recipe.description }}</p>
          <button class="cta-primary detail-cta" type="button" @click="goToPlan">
            <FcIcon name="plus" :size="16" />
            加入备餐计划
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
            <div class="step-content">
              <p>{{ step.content }}</p>
              <img
                v-if="stepImages.get(step.stepNo)"
                class="step-image"
                :src="stepImages.get(step.stepNo)"
                :alt="`${recipe.name} 步骤 ${step.stepNo}`"
                loading="lazy"
              />
            </div>
          </li>
        </ol>
      </section>

      <section v-if="galleryImages.length > 1" class="detail-section">
        <h2>步骤图解</h2>
        <div class="detail-gallery">
          <figure v-for="(image, index) in galleryImages" :key="`${image.url}-${index}`">
            <img :src="image.url" :alt="image.alt" loading="lazy" />
          </figure>
        </div>
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
  display: inline-flex;
  align-items: center;
  gap: 6px;
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
  width: 96px;
  height: 96px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.45);
  border: 3px solid rgba(255, 255, 255, 0.55);
  color: var(--sage);
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

.detail-stat-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 0 0 14px;
}

.detail-stat {
  font-size: 12px;
  font-weight: 600;
  color: var(--gray-text);
  padding: 4px 10px;
  border-radius: 12px;
  background: var(--cream);
  border: 1px solid var(--sage-border);
}

.detail-stars {
  color: var(--light-orange);
  letter-spacing: 0.04em;
}

.detail-desc {
  margin: 0 0 20px;
  color: var(--gray-text);
  line-height: 1.65;
}

.detail-cta {
  width: fit-content;
  gap: 8px;
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

.step-content {
  min-width: 0;
}

.step-image {
  display: block;
  width: 100%;
  max-width: 420px;
  margin-top: 12px;
  border-radius: var(--radius-md);
  border: 1px solid var(--sage-border);
}

.detail-gallery {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 12px;
}

.detail-gallery figure {
  margin: 0;
  border: 1px solid var(--sage-border);
  border-radius: var(--radius-md);
  overflow: hidden;
  background: var(--white);
}

.detail-gallery img {
  display: block;
  width: 100%;
  aspect-ratio: 4 / 3;
  object-fit: cover;
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

@media (max-width: 768px) {
  .detail-hero {
    grid-template-columns: 1fr;
    gap: 18px;
    margin-bottom: 28px;
  }

  .detail-cover {
    border-radius: var(--radius-md);
  }

  .detail-cover-fallback {
    width: 72px;
    height: 72px;
    font-size: 32px;
  }

  .detail-cta {
    width: 100%;
    justify-content: center;
  }

  .detail-steps li {
    padding: 12px;
    gap: 10px;
  }

  .step-image {
    max-width: 100%;
  }

  .detail-gallery {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 8px;
  }
}

@media (max-width: 480px) {
  .detail-gallery {
    grid-template-columns: 1fr;
  }
}
</style>
