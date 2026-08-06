<script setup lang="ts">
import { computed } from 'vue'
import type { AccessLogStats, CountItem } from '../api/telemetry'

const props = defineProps<{
  stats: AccessLogStats | null
}>()

const LABEL_MAP: Record<string, string> = {
  MOBILE: '手机',
  TABLET: '平板',
  DESKTOP: '电脑',
  WEB: '网页',
  MOBILE_WEB: '移动网页',
  PWA: 'PWA',
  GRANTED: '已授权',
  DENIED: '已拒绝',
  UNAVAILABLE: '不可用',
}

function labelOf(value: string) {
  return LABEL_MAP[value] ?? value
}

function maxCount(items: CountItem[]) {
  return items.reduce((max, item) => Math.max(max, item.count), 0) || 1
}

const trendMax = computed(() => {
  if (!props.stats) return 1
  return props.stats.dailyTrend.reduce((max, item) => Math.max(max, item.count), 0) || 1
})

function formatDay(date: string) {
  const parts = date.split('-')
  return parts.length === 3 ? `${parts[1]}/${parts[2]}` : date
}

function barWidth(count: number, max: number) {
  return `${Math.max(8, Math.round((count / max) * 100))}%`
}

function formatPage(path: string) {
  if (path === '/') return '首页'
  if (path.startsWith('/recipes/')) return '菜谱详情'
  if (path === '/recipes') return '菜谱列表'
  if (path === '/pantry') return '冰箱'
  if (path === '/meal-plan') return '备餐'
  if (path === '/login') return '登录'
  if (path === '/register') return '注册'
  return path
}
</script>

<template>
  <section v-if="stats" class="access-dashboard">
    <div class="access-stat-cards">
      <article class="access-stat-card">
        <span class="access-stat-label">总访问</span>
        <strong class="access-stat-value">{{ stats.total }}</strong>
      </article>
      <article class="access-stat-card">
        <span class="access-stat-label">今日访问</span>
        <strong class="access-stat-value">{{ stats.todayCount }}</strong>
      </article>
      <article class="access-stat-card">
        <span class="access-stat-label">今日独立访客</span>
        <strong class="access-stat-value">{{ stats.uniqueVisitorsToday }}</strong>
      </article>
    </div>

    <div class="access-chart-grid">
      <article class="access-chart-card access-chart-card--wide">
        <h3>近 7 日访问趋势</h3>
        <div class="access-trend-chart">
          <div
            v-for="item in stats.dailyTrend"
            :key="item.date"
            class="access-trend-col"
          >
            <span class="access-trend-value">{{ item.count }}</span>
            <div class="access-trend-bar-wrap">
              <div
                class="access-trend-bar"
                :style="{ height: barWidth(item.count, trendMax) }"
              />
            </div>
            <span class="access-trend-day">{{ formatDay(item.date) }}</span>
          </div>
        </div>
      </article>

      <article class="access-chart-card">
        <h3>设备分布</h3>
        <ul class="access-bar-list">
          <li v-for="item in stats.deviceBreakdown" :key="item.label">
            <span class="access-bar-label">{{ labelOf(item.label) }}</span>
            <div class="access-bar-track">
              <div
                class="access-bar-fill access-bar-fill--sage"
                :style="{ width: barWidth(item.count, maxCount(stats.deviceBreakdown)) }"
              />
            </div>
            <span class="access-bar-count">{{ item.count }}</span>
          </li>
        </ul>
      </article>

      <article class="access-chart-card">
        <h3>访问类型</h3>
        <ul class="access-bar-list">
          <li v-for="item in stats.accessTypeBreakdown" :key="item.label">
            <span class="access-bar-label">{{ labelOf(item.label) }}</span>
            <div class="access-bar-track">
              <div
                class="access-bar-fill access-bar-fill--orange"
                :style="{ width: barWidth(item.count, maxCount(stats.accessTypeBreakdown)) }"
              />
            </div>
            <span class="access-bar-count">{{ item.count }}</span>
          </li>
        </ul>
      </article>

      <article class="access-chart-card">
        <h3>GPS 状态</h3>
        <ul class="access-bar-list">
          <li v-for="item in stats.gpsStatusBreakdown" :key="item.label">
            <span class="access-bar-label">{{ labelOf(item.label) }}</span>
            <div class="access-bar-track">
              <div
                class="access-bar-fill access-bar-fill--muted"
                :style="{ width: barWidth(item.count, maxCount(stats.gpsStatusBreakdown)) }"
              />
            </div>
            <span class="access-bar-count">{{ item.count }}</span>
          </li>
        </ul>
      </article>

      <article class="access-chart-card access-chart-card--wide">
        <h3>热门页面</h3>
        <ul class="access-bar-list access-bar-list--pages">
          <li v-for="item in stats.topPages" :key="item.label">
            <span class="access-bar-label access-bar-label--page" :title="item.label">
              {{ formatPage(item.label) }}
            </span>
            <div class="access-bar-track">
              <div
                class="access-bar-fill access-bar-fill--sage"
                :style="{ width: barWidth(item.count, maxCount(stats.topPages)) }"
              />
            </div>
            <span class="access-bar-count">{{ item.count }}</span>
          </li>
        </ul>
      </article>
    </div>
  </section>
</template>

<style scoped>
.access-dashboard {
  display: grid;
  gap: 20px;
  margin-bottom: 20px;
}

.access-stat-cards {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.access-stat-card {
  padding: 18px 20px;
  border: 1px solid var(--sage-border);
  border-radius: var(--radius-md);
  background: var(--white);
}

.access-stat-label {
  display: block;
  margin-bottom: 6px;
  color: var(--gray-text);
  font-size: 13px;
}

.access-stat-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--deep-green);
  letter-spacing: -0.02em;
}

.access-chart-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.access-chart-card {
  padding: 18px 20px;
  border: 1px solid var(--sage-border);
  border-radius: var(--radius-md);
  background: var(--white);
}

.access-chart-card--wide {
  grid-column: 1 / -1;
}

.access-chart-card h3 {
  margin: 0 0 14px;
  font-size: 15px;
  font-weight: 600;
  color: var(--deep-green);
}

.access-trend-chart {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 10px;
  align-items: end;
  min-height: 180px;
}

.access-trend-col {
  display: grid;
  gap: 8px;
  justify-items: center;
  height: 100%;
  align-content: end;
}

.access-trend-value {
  font-size: 12px;
  font-weight: 600;
  color: var(--deep-green);
}

.access-trend-bar-wrap {
  display: flex;
  align-items: flex-end;
  width: 100%;
  height: 120px;
}

.access-trend-bar {
  width: 100%;
  min-height: 8px;
  border-radius: 6px 6px 2px 2px;
  background: linear-gradient(180deg, var(--sage), #5f7f63);
}

.access-trend-day {
  font-size: 11px;
  color: var(--gray-text);
}

.access-bar-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: 10px;
}

.access-bar-list li {
  display: grid;
  grid-template-columns: 72px 1fr 36px;
  gap: 10px;
  align-items: center;
}

.access-bar-list--pages li {
  grid-template-columns: 88px 1fr 36px;
}

.access-bar-label {
  font-size: 12px;
  color: var(--gray-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.access-bar-label--page {
  color: var(--deep-green);
  font-weight: 500;
}

.access-bar-track {
  height: 10px;
  border-radius: 999px;
  background: var(--sage-light);
  overflow: hidden;
}

.access-bar-fill {
  height: 100%;
  border-radius: 999px;
  min-width: 8px;
}

.access-bar-fill--sage { background: var(--sage); }
.access-bar-fill--orange { background: var(--light-orange); }
.access-bar-fill--muted { background: #9bb39f; }

.access-bar-count {
  font-size: 12px;
  font-weight: 600;
  color: var(--deep-green);
  text-align: right;
}

@media (max-width: 960px) {
  .access-chart-grid {
    grid-template-columns: 1fr;
  }

  .access-stat-cards {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .access-trend-chart {
    gap: 6px;
  }

  .access-bar-list li,
  .access-bar-list--pages li {
    grid-template-columns: 64px 1fr 28px;
    gap: 8px;
  }
}
</style>
