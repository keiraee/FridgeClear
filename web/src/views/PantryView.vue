<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { storeToRefs } from 'pinia'
import { usePantryStore } from '../stores/pantry'
import type { PantryItem } from '../types'
import type { PantryItemPayload } from '../api/pantry'

defineOptions({ name: 'Pantry' })

const pantryStore = usePantryStore()
const { availableItems, loading, error: errorMessage } = storeToRefs(pantryStore)

const submitting = ref(false)
const showForm = ref(false)
const form = reactive<PantryItemPayload>({ rawName: '', quantity: 1, unit: '个', expireDate: '', note: '' })
const customUnit = ref('')
const unitOptions = ['个', '只', '条', '根', '把', '块', '片', '瓣', '盒', '袋', '瓶', '罐', '包', '份', '克', '千克', '毫升', '升', '斤']

const expiringItems = computed(() =>
  availableItems.value.filter((item) => item.expiringSoon ?? item.isExpiringSoon),
)

/** 临期食材置顶，其余按到期日 */
const sortedItems = computed(() => {
  return [...availableItems.value].sort((a, b) => {
    const aExpiring = a.expiringSoon ?? a.isExpiringSoon ? 0 : 1
    const bExpiring = b.expiringSoon ?? b.isExpiringSoon ? 0 : 1
    if (aExpiring !== bExpiring) return aExpiring - bExpiring
    const aDate = a.expireDate ?? '9999-12-31'
    const bDate = b.expireDate ?? '9999-12-31'
    return aDate.localeCompare(bDate)
  })
})

async function loadItems() {
  await pantryStore.fetchAvailable()
}

function openAddForm() {
  showForm.value = true
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

async function submit() {
  const unit = form.unit === '其他' ? customUnit.value.trim() : form.unit.trim()
  if (!form.rawName.trim() || !Number.isFinite(form.quantity) || form.quantity <= 0 || !unit) {
    errorMessage.value = '请填写食材名称、数量和单位'
    return
  }
  submitting.value = true
  errorMessage.value = ''
  try {
    await pantryStore.addItem({ ...form, rawName: form.rawName.trim(), unit })
    Object.assign(form, { rawName: '', quantity: 1, unit: '个', expireDate: '', note: '' })
    customUnit.value = ''
    showForm.value = false
  } catch (error) {
    errorMessage.value = (error as { response?: { data?: { message?: string } } }).response?.data?.message ?? '新增库存失败'
  } finally {
    submitting.value = false
  }
}

async function markUsed(item: PantryItem) {
  try {
    await pantryStore.markUsed(item.id)
  } catch {
    errorMessage.value = '更新库存状态失败'
  }
}

async function remove(item: PantryItem) {
  if (!window.confirm(`确定删除「${item.rawName}」吗？`)) return
  try {
    await pantryStore.removeItem(item.id)
  } catch {
    errorMessage.value = '删除库存失败'
  }
}

function displayName(item: PantryItem) {
  return item.ingredientName || item.canonicalName || item.rawName
}

function formatQuantity(item: PantryItem) {
  return `${item.quantity ?? '-'} ${item.unit ?? ''}`.trim()
}

function isExpiring(item: PantryItem) {
  return !!(item.expiringSoon ?? item.isExpiringSoon)
}

onMounted(loadItems)
</script>

<template>
  <main class="page-main pantry-page">
    <section class="page-heading-row">
      <div>
        <p class="overline">MY PANTRY</p>
        <h1>我的冰箱</h1>
        <p class="page-desc">管理库存食材，优先消耗即将过期的食材。</p>
      </div>
      <button class="cta-primary" type="button" @click="showForm = !showForm">
        {{ showForm ? '收起表单' : '＋ 添加食材' }}
      </button>
    </section>

    <p v-if="errorMessage" class="error-copy">{{ errorMessage }}</p>

    <section v-if="showForm" class="pantry-form-card">
      <div class="section-head compact">
        <div>
          <p class="overline">ADD TO PANTRY</p>
          <h2>添加库存食材</h2>
        </div>
      </div>
      <form class="pantry-form" @submit.prevent="submit">
        <label>食材名称<input v-model="form.rawName" placeholder="例如：西红柿" /></label>
        <label>数量<input v-model.number="form.quantity" type="number" min="0.001" step="any" inputmode="decimal" /></label>
        <label>
          单位
          <select v-model="form.unit">
            <option v-for="unit in unitOptions" :key="unit" :value="unit">{{ unit }}</option>
            <option value="其他">其他</option>
          </select>
        </label>
        <label v-if="form.unit === '其他'">自定义单位<input v-model="customUnit" maxlength="32" placeholder="例如：托、盘" /></label>
        <label>到期日期<input v-model="form.expireDate" type="date" /></label>
        <label class="wide">备注<input v-model="form.note" placeholder="可选，例如：冷冻保存" /></label>
        <div class="form-actions wide">
          <button class="secondary-btn" type="button" @click="showForm = false">取消</button>
          <button class="cta-primary" type="submit" :disabled="submitting">
            {{ submitting ? '保存中…' : '保存食材' }}
          </button>
        </div>
      </form>
    </section>

    <section class="pantry-summary-row">
      <div class="mini-stat">
        <span>当前库存</span>
        <strong>{{ availableItems.length }}</strong>
        <small>种食材</small>
      </div>
      <div class="mini-stat urgent-stat">
        <span>临期提醒</span>
        <strong>{{ expiringItems.length }}</strong>
        <small>种食材</small>
      </div>
    </section>

    <section class="pantry-list-card">
      <div class="section-head compact">
        <div>
          <p class="overline">YOUR INGREDIENTS</p>
          <h2>库存清单</h2>
        </div>
        <span class="list-count">{{ availableItems.length }} 项</span>
      </div>

      <p v-if="loading" class="loading-copy">正在加载库存…</p>

      <div v-else-if="!availableItems.length" class="pantry-empty">
        <div class="pantry-empty-icon" aria-hidden="true">🧊</div>
        <h3>冰箱还是空的</h3>
        <p>先添加一些食材，首页就能根据库存为你推荐可做的菜。</p>
        <button class="cta-primary" type="button" @click="openAddForm">＋ 添加第一件食材</button>
      </div>

      <div v-else class="pantry-cards">
        <article
          v-for="item in sortedItems"
          :key="item.id"
          class="pantry-card"
          :class="{ expiring: isExpiring(item) }"
        >
          <div v-if="isExpiring(item)" class="pantry-card-accent" aria-hidden="true" />
          <div class="pantry-card-body">
            <div class="pantry-card-top">
              <div class="pantry-name">
                <strong>{{ displayName(item) }}</strong>
                <span v-if="item.rawName !== displayName(item)">{{ item.rawName }}</span>
              </div>
              <span v-if="isExpiring(item)" class="pantry-expiring-badge">临期</span>
            </div>

            <div class="pantry-card-meta">
              <div class="pantry-meta-item">
                <span class="pantry-meta-label">数量</span>
                <span class="pantry-quantity">{{ formatQuantity(item) }}</span>
              </div>
              <div class="pantry-meta-item">
                <span class="pantry-meta-label">到期</span>
                <span class="pantry-expire">{{ item.expireDate || '未设置' }}</span>
              </div>
            </div>

            <p v-if="item.note" class="pantry-note">{{ item.note }}</p>

            <div class="pantry-actions">
              <button type="button" class="pantry-action-primary" @click="markUsed(item)">已用完</button>
              <button type="button" class="pantry-action-danger" @click="remove(item)">删除</button>
            </div>
          </div>
        </article>
      </div>
    </section>
  </main>
</template>
