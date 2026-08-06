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
  await http.post('/telemetry/access', payload, { timeout: 10000 })
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
