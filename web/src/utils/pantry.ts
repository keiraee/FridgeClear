import type { PantryItem } from '../types'

export function daysUntilExpire(expireDate: string | null | undefined): number | null {
  if (!expireDate) return null
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const exp = new Date(`${expireDate}T00:00:00`)
  if (Number.isNaN(exp.getTime())) return null
  return Math.ceil((exp.getTime() - today.getTime()) / 86_400_000)
}

export function pantryItemDisplayName(item: PantryItem): string {
  return item.ingredientName ?? item.canonicalName ?? item.rawName
}

export function formatExpireDetail(expireDate: string | null | undefined): string {
  const days = daysUntilExpire(expireDate)
  if (!expireDate || days === null) return '未设置'
  if (days < 0) return `${expireDate}（已过期）`
  if (days === 0) return `${expireDate}（今天到期）`
  if (days === 1) return `${expireDate}（明天到期）`
  return `${expireDate}（还剩 ${days} 天）`
}

export function isExpired(expireDate: string | null | undefined): boolean {
  const days = daysUntilExpire(expireDate)
  return days !== null && days < 0
}

export function isExpiringSoon(item: PantryItem): boolean {
  return !!(item.expiringSoon ?? item.isExpiringSoon)
}

export function formatExpireLabel(item: PantryItem): string {
  const name = pantryItemDisplayName(item)
  const days = daysUntilExpire(item.expireDate)
  if (days === null) return name
  if (days < 0) return `${name} 已过期`
  if (days === 0) return `${name} 今天到期`
  if (days === 1) return `${name} 明天到期`
  return `${name} 还剩 ${days} 天`
}
