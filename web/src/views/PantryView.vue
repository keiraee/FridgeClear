<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { storeToRefs } from 'pinia'
import { usePantryStore } from '../stores/pantry'
import type { PantryItem } from '../types'
import type { PantryItemPayload } from '../api/pantry'
import { formatExpireDetail } from '../utils/pantry'
import FcIcon from '../components/FcIcon.vue'
import LoadingWait from '../components/LoadingWait.vue'
import { PANTRY_LOADING_STAGES } from '../composables/useElapsedTimer'
import { confirmDiscardDraft, useUnsavedDraftGuard } from '../composables/useUnsavedDraftGuard'

defineOptions({ name: 'Pantry' })

type DraftPantryRow = {
  key: string
  rawName: string
  quantity: number
  unit: string
  customUnit: string
  expireDate: string
  note: string
}

const pantryStore = usePantryStore()
const { availableItems, loading, error: storeError } = storeToRefs(pantryStore)

const submitting = ref(false)
const showForm = ref(false)
const formError = ref('')
const saveProgress = ref({ current: 0, total: 0 })

const unitOptions = ['个', '只', '条', '根', '把', '块', '片', '瓣', '盒', '袋', '瓶', '罐', '包', '份', '克', '千克', '毫升', '升', '斤']

let draftKeySeq = 0

function createDraftRow(): DraftPantryRow {
  draftKeySeq += 1
  return {
    key: `draft-${draftKeySeq}`,
    rawName: '',
    quantity: 1,
    unit: '个',
    customUnit: '',
    expireDate: '',
    note: '',
  }
}

const draftRows = ref<DraftPantryRow[]>([createDraftRow()])

const expiringItems = computed(() =>
  availableItems.value.filter((item) => item.expiringSoon ?? item.isExpiringSoon),
)

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

function rowIsDirty(row: DraftPantryRow): boolean {
  return !!(
    row.rawName.trim()
    || row.expireDate
    || row.note.trim()
    || row.customUnit.trim()
    || row.unit !== '个'
    || row.quantity !== 1
  )
}

const hasDraftChanges = computed(() => draftRows.value.some(rowIsDirty))

const pendingSaveCount = computed(() => {
  let count = 0
  for (const row of draftRows.value) {
    if (!rowIsDirty(row)) continue
    const rawName = row.rawName.trim()
    const unit = row.unit === '其他' ? row.customUnit.trim() : row.unit.trim()
    if (rawName && Number.isFinite(row.quantity) && row.quantity > 0 && unit) count += 1
  }
  return count
})

useUnsavedDraftGuard(() => showForm.value && hasDraftChanges.value)

function resetDraftRows() {
  draftRows.value = [createDraftRow()]
  formError.value = ''
  saveProgress.value = { current: 0, total: 0 }
}

function openAddForm() {
  showForm.value = true
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function toggleForm() {
  if (showForm.value) {
    if (hasDraftChanges.value && !confirmDiscardDraft()) return
    resetDraftRows()
    showForm.value = false
    return
  }
  showForm.value = true
}

function addDraftRow() {
  draftRows.value.push(createDraftRow())
}

function removeDraftRow(index: number) {
  if (draftRows.value.length === 1) {
    draftRows.value[0] = createDraftRow()
    return
  }
  draftRows.value.splice(index, 1)
}

function validateDraftRows(): { valid: PantryItemPayload[]; error?: string } {
  const valid: PantryItemPayload[] = []

  for (let index = 0; index < draftRows.value.length; index += 1) {
    const row = draftRows.value[index]!
    if (!rowIsDirty(row)) continue

    const rawName = row.rawName.trim()
    const unit = row.unit === '其他' ? row.customUnit.trim() : row.unit.trim()

    if (!rawName) {
      return { valid: [], error: `第 ${index + 1} 行：请填写食材名称，或删除该行` }
    }
    if (!Number.isFinite(row.quantity) || row.quantity <= 0) {
      return { valid: [], error: `第 ${index + 1} 行：请填写有效数量` }
    }
    if (!unit) {
      return { valid: [], error: `第 ${index + 1} 行：请选择或填写单位` }
    }

    valid.push({
      rawName,
      quantity: row.quantity,
      unit,
      expireDate: row.expireDate || undefined,
      note: row.note.trim() || undefined,
    })
  }

  if (!valid.length) {
    return { valid: [], error: '请至少填写一项食材' }
  }

  return { valid }
}

function showToast(message: string) {
  window.dispatchEvent(new CustomEvent('fridgeclear:toast', { detail: message }))
}

async function loadItems() {
  await pantryStore.fetchAvailable()
}

async function submitDraft() {
  const { valid, error } = validateDraftRows()
  if (error) {
    formError.value = error
    return
  }

  submitting.value = true
  formError.value = ''
  saveProgress.value = { current: 0, total: valid.length }

  try {
    await pantryStore.addItemsBatch(valid, (current, total) => {
      saveProgress.value = { current, total }
    })
    showToast(`已保存 ${valid.length} 项食材`)
    resetDraftRows()
    showForm.value = false
  } catch (error) {
    formError.value = (error as { response?: { data?: { message?: string } } }).response?.data?.message
      ?? '保存失败，请稍后重试'
  } finally {
    submitting.value = false
    saveProgress.value = { current: 0, total: 0 }
  }
}

async function markUsed(item: PantryItem) {
  try {
    await pantryStore.markUsed(item.id)
  } catch {
    formError.value = '更新库存状态失败'
  }
}

async function remove(item: PantryItem) {
  if (!window.confirm(`确定删除「${item.rawName}」吗？`)) return
  try {
    await pantryStore.removeItem(item.id)
  } catch {
    formError.value = '删除库存失败'
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
        <h1>我的冰箱</h1>
        <p class="page-desc">可一次填写多项食材，确认后再统一保存。</p>
      </div>
      <button class="cta-primary" type="button" @click="toggleForm">
        <FcIcon name="plus" :size="16" />
        {{ showForm ? '收起表单' : '添加食材' }}
      </button>
    </section>

    <p v-if="formError" class="error-copy">{{ formError }}</p>
    <p v-else-if="storeError" class="error-copy">{{ storeError }}</p>

    <section v-if="showForm" class="pantry-form-card">
      <div class="section-head compact pantry-draft-head">
        <div>
          <h2>批量添加食材</h2>
          <p class="pantry-draft-subtitle">填写多行后点击保存，会依次提交到服务器。</p>
        </div>
      </div>

      <form class="pantry-draft" @submit.prevent="submitDraft">
        <div class="pantry-draft-table" role="table" aria-label="待添加食材">
          <div class="pantry-draft-row pantry-draft-row--head" role="row">
            <span role="columnheader">食材名称</span>
            <span role="columnheader">数量</span>
            <span role="columnheader">单位</span>
            <span role="columnheader">到期</span>
            <span role="columnheader">备注</span>
            <span role="columnheader" class="pantry-draft-action-col" aria-label="操作" />
          </div>

          <div
            v-for="(row, index) in draftRows"
            :key="row.key"
            class="pantry-draft-row"
            role="row"
          >
            <label class="pantry-draft-field pantry-draft-field--name">
              <span class="pantry-draft-label">食材名称</span>
              <input v-model="row.rawName" placeholder="例如：西红柿" />
            </label>

            <label class="pantry-draft-field pantry-draft-field--qty">
              <span class="pantry-draft-label">数量</span>
              <input v-model.number="row.quantity" type="number" min="0.001" step="any" inputmode="decimal" />
            </label>

            <label class="pantry-draft-field pantry-draft-field--unit">
              <span class="pantry-draft-label">单位</span>
              <select v-model="row.unit">
                <option v-for="unit in unitOptions" :key="unit" :value="unit">{{ unit }}</option>
                <option value="其他">其他</option>
              </select>
              <input
                v-if="row.unit === '其他'"
                v-model="row.customUnit"
                class="pantry-draft-custom-unit"
                maxlength="32"
                placeholder="自定义单位"
              />
            </label>

            <label class="pantry-draft-field pantry-draft-field--expire">
              <span class="pantry-draft-label">到期</span>
              <input v-model="row.expireDate" type="date" />
            </label>

            <label class="pantry-draft-field pantry-draft-field--note">
              <span class="pantry-draft-label">备注</span>
              <input v-model="row.note" placeholder="可选" />
            </label>

            <div class="pantry-draft-field pantry-draft-field--remove">
              <button
                type="button"
                class="pantry-draft-remove"
                :aria-label="`删除第 ${index + 1} 行`"
                @click="removeDraftRow(index)"
              >
                <FcIcon name="trash" :size="16" />
              </button>
            </div>
          </div>
        </div>

        <button type="button" class="pantry-draft-add" @click="addDraftRow">
          <FcIcon name="plus" :size="16" />
          再加一行
        </button>

        <p v-if="hasDraftChanges" class="pantry-draft-hint">未保存内容在刷新或离开页面时会丢失</p>

        <div class="pantry-draft-actions">
          <button class="secondary-btn" type="button" :disabled="submitting" @click="toggleForm">取消</button>
          <button class="cta-primary" type="submit" :disabled="submitting || pendingSaveCount === 0">
            <template v-if="submitting">
              保存中 {{ saveProgress.current }}/{{ saveProgress.total }}…
            </template>
            <template v-else>
              保存{{ pendingSaveCount > 0 ? ` ${pendingSaveCount} 项` : '' }}
            </template>
          </button>
        </div>
      </form>
    </section>

    <p v-if="availableItems.length" class="pantry-inline-summary">
      当前 {{ availableItems.length }} 种食材
      <template v-if="expiringItems.length"> · {{ expiringItems.length }} 种临期</template>
    </p>

    <section class="pantry-list-card">
      <div class="section-head compact">
        <h2>库存清单</h2>
        <span class="list-count">{{ availableItems.length }} 项</span>
      </div>

      <LoadingWait
        v-if="loading"
        :active="loading"
        :stages="PANTRY_LOADING_STAGES"
        hint="通常 1–3 秒"
      />

      <div v-else-if="!availableItems.length" class="pantry-empty">
        <div class="pantry-empty-icon" aria-hidden="true">
          <FcIcon name="pantry" :size="40" />
        </div>
        <h3>还没有登记库存</h3>
        <p>添加食材和到期日后，首页就能推荐可做的菜。</p>
        <button class="cta-primary" type="button" @click="openAddForm">
          <FcIcon name="plus" :size="16" />
          开始添加
        </button>
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
                <span class="pantry-expire">{{ formatExpireDetail(item.expireDate) }}</span>
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
