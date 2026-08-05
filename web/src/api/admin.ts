import http from './http'

export interface SystemAiConfig {
  providerName: string
  protocol: string
  baseUrl: string
  modelName: string
  enabled: boolean
  apiKeyConfigured: boolean
}

export interface SystemAiConfigUpdate {
  providerName: string
  protocol: string
  baseUrl: string
  apiKey?: string
  modelName: string
  enabled: boolean
}

export interface ConnectionTestResult {
  success: boolean
  message: string
  modelCount: number
}

export interface ModelItem {
  id: string
  name: string
}

export async function getSystemAiConfig() {
  const response = await http.get<{ data: SystemAiConfig }>('/admin/ai/config')
  return response.data.data
}

export async function updateSystemAiConfig(payload: SystemAiConfigUpdate) {
  const response = await http.put<{ data: SystemAiConfig }>('/admin/ai/config', payload)
  return response.data.data
}

export async function fetchSystemAiConfigModels(payload: { protocol: string; baseUrl: string; apiKey?: string }) {
  const response = await http.post<{ data: ModelItem[] }>('/admin/ai/config/models', payload)
  return response.data.data
}

export async function testSystemAiConfig() {
  const response = await http.post<{ data: ConnectionTestResult }>('/admin/ai/config/test')
  return response.data.data
}
