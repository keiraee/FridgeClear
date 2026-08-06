import type { Router } from 'vue-router'
import { recordAccessLog, type AccessLogPayload } from '../api/telemetry'

const CLIENT_ID_KEY = 'fridgeclear_client_id'

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
      { enableHighAccuracy: false, timeout: 8000, maximumAge: 300_000 },
    )
  })
}

async function resolveGps(): Promise<GpsResult> {
  if (!gpsPromise) gpsPromise = attemptGps()
  return gpsPromise
}

function buildPayload(pagePath: string, gps: GpsResult): AccessLogPayload {
  const deviceType = detectDeviceType()
  const connection = (navigator as Navigator & { connection?: { effectiveType?: string } }).connection

  return {
    clientId: getClientId(),
    deviceType,
    accessType: detectAccessType(deviceType),
    pagePath,
    referrer: document.referrer || undefined,
    locale: navigator.language,
    timezone: Intl.DateTimeFormat().resolvedOptions().timeZone,
    screenWidth: window.screen.width,
    screenHeight: window.screen.height,
    viewportWidth: window.innerWidth,
    viewportHeight: window.innerHeight,
    connectionType: connection?.effectiveType,
    ...gps,
    extraJson: JSON.stringify({
      online: navigator.onLine,
      platform: navigator.platform,
      userAgent: navigator.userAgent,
      cookieEnabled: navigator.cookieEnabled,
      touchPoints: navigator.maxTouchPoints,
      pixelRatio: window.devicePixelRatio,
    }),
  }
}

export async function reportAccessTelemetry(pagePath: string) {
  const gps = await resolveGps()
  await recordAccessLog(buildPayload(pagePath, gps))
}

export function initAccessTelemetry(router: Router) {
  let lastSentPath = ''
  let debounceTimer: ReturnType<typeof setTimeout> | undefined

  const send = (path: string) => {
    if (!path || path === lastSentPath) return
    lastSentPath = path
    void reportAccessTelemetry(path).catch(() => {
      lastSentPath = ''
    })
  }

  void router.isReady().then(() => {
    send(router.currentRoute.value.fullPath)
  })

  router.afterEach((to) => {
    clearTimeout(debounceTimer)
    debounceTimer = setTimeout(() => send(to.fullPath), 400)
  })
}
