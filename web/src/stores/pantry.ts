import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import {
  createPantryItem,
  deletePantryItem,
  getPantryItems,
  updatePantryStatus,
  type PantryItemPayload,
} from '../api/pantry'
import type { PantryItem } from '../types'
import { isCacheFresh } from '../utils/cache'

const PANTRY_TTL_MS = 60_000

export const usePantryStore = defineStore('pantry', () => {
  const items = ref<PantryItem[]>([])
  const loading = ref(false)
  const error = ref('')
  const fetchedAt = ref<number | null>(null)

  const availableItems = computed(() => items.value.filter((item) => item.status === 'AVAILABLE'))
  const expiringCount = computed(() =>
    availableItems.value.filter((item) => item.expiringSoon ?? item.isExpiringSoon).length,
  )

  async function fetchAvailable(options: { force?: boolean } = {}) {
    const { force = false } = options
    if (!force && fetchedAt.value !== null && isCacheFresh(fetchedAt.value, PANTRY_TTL_MS)) {
      return
    }

    const showLoading = fetchedAt.value === null
    if (showLoading) loading.value = true
    error.value = ''
    try {
      const result = await getPantryItems({ status: 'AVAILABLE', page: 0, size: 100 })
      items.value = result.items
      fetchedAt.value = Date.now()
    } catch {
      error.value = '库存加载失败，请确认后端服务已启动'
      if (fetchedAt.value === null) items.value = []
    } finally {
      if (showLoading) loading.value = false
    }
  }

  function invalidate() {
    fetchedAt.value = null
  }

  function reset() {
    items.value = []
    loading.value = false
    error.value = ''
    fetchedAt.value = null
  }

  async function addItem(payload: PantryItemPayload) {
    const created = await createPantryItem(payload)
    items.value.unshift(created)
    fetchedAt.value = Date.now()
    return created
  }

  async function addItemsBatch(
    payloads: PantryItemPayload[],
    onProgress?: (current: number, total: number) => void,
  ) {
    const created: PantryItem[] = []
    for (let index = 0; index < payloads.length; index += 1) {
      const item = await createPantryItem(payloads[index]!)
      created.push(item)
      onProgress?.(index + 1, payloads.length)
    }
    items.value = [...created, ...items.value]
    fetchedAt.value = Date.now()
    return created
  }

  async function markUsed(id: number) {
    await updatePantryStatus(id, 'USED_UP')
    items.value = items.value.filter((item) => item.id !== id)
    fetchedAt.value = Date.now()
  }

  async function removeItem(id: number) {
    await deletePantryItem(id)
    items.value = items.value.filter((item) => item.id !== id)
    fetchedAt.value = Date.now()
  }

  return {
    items,
    availableItems,
    expiringCount,
    loading,
    error,
    fetchAvailable,
    invalidate,
    reset,
    addItem,
    addItemsBatch,
    markUsed,
    removeItem,
  }
})
