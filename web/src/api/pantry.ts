import http from './http'
import type { ApiResponse, PantryItem, PantryItemStatus } from '../types'

interface BackendPage<T> {
  content: T[]
  totalElements: number
}

export async function getPantryItems(params: { status?: PantryItemStatus; page?: number; size?: number } = {}) {
  const response = await http.get<ApiResponse<BackendPage<PantryItem>>>('/pantry-items', { params })
  const data = response.data.data
  return { items: data.content, total: data.totalElements }
}

export interface PantryItemPayload {
  rawName: string
  quantity: number
  unit: string
  purchaseDate?: string
  expireDate?: string
  note?: string
}

export async function createPantryItem(payload: PantryItemPayload) {
  const response = await http.post<ApiResponse<PantryItem>>('/pantry-items', payload)
  return response.data.data
}

export async function updatePantryStatus(id: number, status: PantryItemStatus) {
  const response = await http.patch<ApiResponse<PantryItem>>(`/pantry-items/${id}/status`, { status })
  return response.data.data
}

export async function updatePantryItem(id: number, payload: PantryItemPayload) {
  const response = await http.put<ApiResponse<PantryItem>>(`/pantry-items/${id}`, payload)
  return response.data.data
}

export async function deletePantryItem(id: number) {
  await http.delete(`/pantry-items/${id}`)
}
