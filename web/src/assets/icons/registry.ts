/**
 * FridgeClear 统一图标清单 — Remix Icon（线性 / -line）
 * https://github.com/Remix-Design/RemixIcon
 *
 * 通过 unplugin-icons + @iconify-json/ri 按需打包，避免整包引入。
 */

import type { Component } from 'vue'
import RiAddLine from '~icons/ri/add-line'
import RiArrowLeftLine from '~icons/ri/arrow-left-line'
import RiArrowRightLine from '~icons/ri/arrow-right-line'
import RiBowlLine from '~icons/ri/bowl-line'
import RiCakeLine from '~icons/ri/cake-line'
import RiCalendarScheduleLine from '~icons/ri/calendar-schedule-line'
import RiCheckLine from '~icons/ri/check-line'
import RiClipboardLine from '~icons/ri/clipboard-line'
import RiCupLine from '~icons/ri/cup-line'
import RiDeleteBinLine from '~icons/ri/delete-bin-line'
import RiErrorWarningLine from '~icons/ri/error-warning-line'
import RiFireLine from '~icons/ri/fire-line'
import RiFridgeLine from '~icons/ri/fridge-line'
import RiHeartLine from '~icons/ri/heart-line'
import RiHomeLine from '~icons/ri/home-line'
import RiLightbulbLine from '~icons/ri/lightbulb-line'
import RiLogoutBoxLine from '~icons/ri/logout-box-line'
import RiRefreshLine from '~icons/ri/refresh-line'
import RiRestaurantLine from '~icons/ri/restaurant-line'
import RiRobotLine from '~icons/ri/robot-line'
import RiSearchLine from '~icons/ri/search-line'
import RiShoppingCartLine from '~icons/ri/shopping-cart-line'
import RiSkipForwardLine from '~icons/ri/skip-forward-line'
import RiSparklingLine from '~icons/ri/sparkling-line'
import RiTimeLine from '~icons/ri/time-line'
import RiUserLine from '~icons/ri/user-line'

export const ICON_NAMES = [
  'home',
  'recipe',
  'plan',
  'pantry',
  'search',
  'back',
  'heart',
  'plus',
  'check',
  'trash',
  'warning',
  'spark',
  'chef',
  'user',
  'logout',
  'arrow-right',
  'clock',
  'fire',
  'cart',
  'refresh',
  'lightbulb',
  'robot',
  'bowl',
  'cup',
  'cake',
  'clipboard',
  'skip',
] as const

export type IconName = (typeof ICON_NAMES)[number]

/** 语义名 → Remix Icon 组件（统一使用 -line 线性版） */
export const ICON_COMPONENTS: Record<IconName, Component> = {
  home: RiHomeLine,
  recipe: RiRestaurantLine,
  plan: RiCalendarScheduleLine,
  pantry: RiFridgeLine,
  search: RiSearchLine,
  back: RiArrowLeftLine,
  heart: RiHeartLine,
  plus: RiAddLine,
  check: RiCheckLine,
  trash: RiDeleteBinLine,
  warning: RiErrorWarningLine,
  spark: RiSparklingLine,
  chef: RiRestaurantLine,
  user: RiUserLine,
  logout: RiLogoutBoxLine,
  'arrow-right': RiArrowRightLine,
  clock: RiTimeLine,
  fire: RiFireLine,
  cart: RiShoppingCartLine,
  refresh: RiRefreshLine,
  lightbulb: RiLightbulbLine,
  robot: RiRobotLine,
  bowl: RiBowlLine,
  cup: RiCupLine,
  cake: RiCakeLine,
  clipboard: RiClipboardLine,
  skip: RiSkipForwardLine,
}

/** 菜谱封面占位轮换 */
export const RECIPE_FALLBACK_ICONS: IconName[] = ['bowl', 'chef', 'cup', 'cake']
