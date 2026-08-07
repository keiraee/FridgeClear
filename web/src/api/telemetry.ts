import http from './http'

export interface AccessLogPayload {
  clientId: string
  deviceType?: string
  accessType?: string
  pagePath?: string
  referrer?: string
  locale?: string
  timezone?: string
  screenWidth?: number
  screenHeight?: number
  viewportWidth?: number
  viewportHeight?: number
  connectionType?: string
  latitude?: number
  longitude?: number
  gpsAccuracy?: number
  gpsStatus?: string
  extraJson?: string
}

export interface AccessLogItem {
  id: number
  userId: number | null
  userEmail: string | null
  clientId: string
  ipAddress: string | null
  userAgent: string | null
  deviceType: string | null
  accessType: string | null
  pagePath: string | null
  referrer: string | null
  locale: string | null
  timezone: string | null
  screenWidth: number | null
  screenHeight: number | null
  viewportWidth: number | null
  viewportHeight: number | null
  connectionType: string | null
  latitude: number | null
  longitude: number | null
  gpsAccuracy: number | null
  gpsStatus: string | null
  extraJson: string | null
  createdAt: string
}

export interface CountItem {
  label: string
  count: number
}

export interface DailyTrendItem {
  date: string
  count: number
}

export interface AccessLogStats {
  total: number
  todayCount: number
  uniqueVisitorsToday: number
  deviceBreakdown: CountItem[]
  accessTypeBreakdown: CountItem[]
  gpsStatusBreakdown: CountItem[]
  topPages: CountItem[]
  dailyTrend: DailyTrendItem[]
}

export async function recordAccessLog(payload: AccessLogPayload) {
  const baseURL = import.meta.env.VITE_API_BASE_URL ?? '/api/v1'
  const response = await fetch(`${baseURL}/telemetry/access`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
    keepalive: true,
  })
  if (!response.ok) {
    throw new Error(`telemetry failed: ${response.status}`)
  }
}

export async function getAccessLogs(page = 0, size = 20) {
  const response = await http.get<{
    data: { items: AccessLogItem[]; page: number; size: number; total: number }
  }>('/admin/access-logs', { params: { page, size } })
  return response.data.data
}

export async function getAccessLogStats() {
  const response = await http.get<{ data: AccessLogStats }>('/admin/access-logs/stats')
  return response.data.data
}
