import type { Router } from 'vue-router'
import { recordAccessLog, type AccessLogPayload } from '../api/telemetry'

const CLIENT_ID_KEY = 'fridgeclear_client_id'
/** 当前标签页会话内是否已上报（关标签后清空，下次打开再记一条） */
const SESSION_LOG_KEY = 'fridgeclear_access_logged'

const GPS_WAIT_MS = 1500
const RETRY_DELAY_MS = 2500
const MAX_EXTRA_JSON = 1900
const MAX_PATH = 480
const MAX_REFERRER = 480

type GpsResult = Pick<AccessLogPayload, 'latitude' | 'longitude' | 'gpsAccuracy' | 'gpsStatus'>

let gpsPromise: Promise<GpsResult> | null = null

export function getClientId(): string {
  let id = localStorage.getItem(CLIENT_ID_KEY)
  if (!id) {
    id = crypto.randomUUID()
    localStorage.setItem(CLIENT_ID_KEY, id)
  }
  return id
}

function truncate(value: string | undefined, max: number): string | undefined {
  if (!value) return undefined
  const trimmed = value.trim()
  if (!trimmed) return undefined
  return trimmed.length <= max ? trimmed : trimmed.slice(0, max)
}

function detectDeviceType(): 'MOBILE' | 'TABLET' | 'DESKTOP' {
  const ua = navigator.userAgent
  const width = window.innerWidth
  if (/iPad|Tablet|Android(?!.*Mobile)/i.test(ua) || (width >= 768 && width < 1024 && 'ontouchstart' in window)) {
    return 'TABLET'
  }
  if (/Mobi|Android|iPhone|iPod/i.test(ua) || width < 768) return 'MOBILE'
  return 'DESKTOP'
}

function detectAccessType(deviceType: ReturnType<typeof detectDeviceType>): string {
  if (window.matchMedia('(display-mode: standalone)').matches) return 'PWA'
  if (deviceType === 'MOBILE' || deviceType === 'TABLET') return 'MOBILE_WEB'
  return 'WEB'
}

function attemptGps(): Promise<GpsResult> {
  return new Promise((resolve) => {
    if (!navigator.geolocation) {
      resolve({ gpsStatus: 'UNAVAILABLE' })
      return
    }

    navigator.geolocation.getCurrentPosition(
      (position) => {
        resolve({
          latitude: position.coords.latitude,
          longitude: position.coords.longitude,
          gpsAccuracy: position.coords.accuracy,
          gpsStatus: 'GRANTED',
        })
      },
      (error) => {
        resolve({ gpsStatus: error.code === error.PERMISSION_DENIED ? 'DENIED' : 'UNAVAILABLE' })
      },
      { enableHighAccuracy: false, timeout: GPS_WAIT_MS, maximumAge: 300_000 },
    )
  })
}

function resolveGps(): Promise<GpsResult> {
  if (!gpsPromise) gpsPromise = attemptGps()
  return gpsPromise
}

/** 不阻塞上报：最多等 GPS_WAIT_MS，超时仍发送记录 */
async function resolveGpsQuick(): Promise<GpsResult> {
  return Promise.race([
    resolveGps(),
    new Promise<GpsResult>((resolve) => {
      window.setTimeout(() => resolve({ gpsStatus: 'TIMED_OUT' }), GPS_WAIT_MS)
    }),
  ])
}

function buildExtraJson(): string {
  const payload = {
    online: navigator.onLine,
    platform: navigator.platform,
    userAgent: navigator.userAgent.slice(0, 320),
    cookieEnabled: navigator.cookieEnabled,
    touchPoints: navigator.maxTouchPoints,
    pixelRatio: window.devicePixelRatio,
  }
  let json = JSON.stringify(payload)
  if (json.length > MAX_EXTRA_JSON) json = json.slice(0, MAX_EXTRA_JSON)
  return json
}

function buildPayload(pagePath: string, gps: GpsResult): AccessLogPayload {
  const deviceType = detectDeviceType()
  const connection = (navigator as Navigator & { connection?: { effectiveType?: string } }).connection

  return {
    clientId: getClientId(),
    deviceType,
    accessType: detectAccessType(deviceType),
    pagePath: truncate(pagePath, MAX_PATH) ?? '/',
    referrer: truncate(document.referrer, MAX_REFERRER),
    locale: navigator.language,
    timezone: Intl.DateTimeFormat().resolvedOptions().timeZone,
    screenWidth: window.screen.width,
    screenHeight: window.screen.height,
    viewportWidth: window.innerWidth,
    viewportHeight: window.innerHeight,
    connectionType: connection?.effectiveType,
    ...gps,
    extraJson: buildExtraJson(),
  }
}

export async function reportAccessTelemetry(pagePath: string) {
  const gps = await resolveGpsQuick()
  await recordAccessLog(buildPayload(pagePath, gps))
}

function markSessionLogged() {
  sessionStorage.setItem(SESSION_LOG_KEY, '1')
}

function scheduleSessionReport(landingPath: string, allowRetry: boolean) {
  void reportAccessTelemetry(landingPath)
    .then(() => {
      markSessionLogged()
    })
    .catch(() => {
      if (!allowRetry) return
      window.setTimeout(() => {
        if (sessionStorage.getItem(SESSION_LOG_KEY)) return
        scheduleSessionReport(landingPath, false)
      }, RETRY_DELAY_MS)
    })
}

export function initAccessTelemetry(router: Router) {
  void router.isReady().then(() => {
    if (sessionStorage.getItem(SESSION_LOG_KEY)) return
    const landingPath = router.currentRoute.value.fullPath || '/'
    scheduleSessionReport(landingPath, true)
  })
}
