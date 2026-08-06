<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import AccessLogDashboard from '../components/AccessLogDashboard.vue'
import {
  getAccessLogs,
  getAccessLogStats,
  type AccessLogItem,
  type AccessLogStats,
} from '../api/telemetry'
import LoadingWait from '../components/LoadingWait.vue'
import { DEFAULT_LOADING_STAGES } from '../composables/useElapsedTimer'

const router = useRouter()
const logs = ref<AccessLogItem[]>([])
const stats = ref<AccessLogStats | null>(null)
const page = ref(0)
const total = ref(0)
const loading = ref(false)
const errorMessage = ref('')

const pageSize = 20

const GPS_LABELS: Record<string, string> = {
  GRANTED: '已授权',
  DENIED: '已拒绝',
  UNAVAILABLE: '不可用',
}

async function loadData() {
  loading.value = true
  errorMessage.value = ''
  try {
    const [logResult, statsResult] = await Promise.all([
      getAccessLogs(page.value, pageSize),
      getAccessLogStats(),
    ])
    logs.value = logResult.items
    total.value = logResult.total
    stats.value = statsResult
  } catch {
    errorMessage.value = '访问日志加载失败'
    logs.value = []
    total.value = 0
    stats.value = null
  } finally {
    loading.value = false
  }
}

function formatCoords(item: AccessLogItem) {
  if (item.latitude == null || item.longitude == null) {
    return GPS_LABELS[item.gpsStatus ?? ''] ?? item.gpsStatus ?? '—'
  }
  return `${item.latitude.toFixed(5)}, ${item.longitude.toFixed(5)}`
}

function formatTime(value: string) {
  return value.replace('T', ' ').slice(0, 19)
}

function formatUser(item: AccessLogItem) {
  return item.userEmail ?? '访客'
}

function prevPage() {
  if (page.value <= 0) return
  page.value -= 1
  void loadLogsOnly()
}

function nextPage() {
  if ((page.value + 1) * pageSize >= total.value) return
  page.value += 1
  void loadLogsOnly()
}

async function loadLogsOnly() {
  loading.value = true
  errorMessage.value = ''
  try {
    const logResult = await getAccessLogs(page.value, pageSize)
    logs.value = logResult.items
    total.value = logResult.total
  } catch {
    errorMessage.value = '访问日志加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void loadData()
})
</script>

<template>
  <main class="page-main admin-access-logs-page">
    <section class="page-heading-row">
      <div>
        <h1>访问日志</h1>
        <p class="page-desc">访问趋势、设备分布与明细记录（GPS 需浏览器授权）。</p>
      </div>
      <div class="admin-page-links">
        <button class="secondary-btn" type="button" @click="router.push('/admin/ai-config')">AI 配置</button>
        <button class="secondary-btn" type="button" @click="loadData">刷新</button>
      </div>
    </section>

    <p v-if="errorMessage" class="error-copy">{{ errorMessage }}</p>

    <LoadingWait v-if="loading && !stats" :active="loading" :stages="DEFAULT_LOADING_STAGES" hint="通常 1–3 秒" compact />

    <template v-else>
      <AccessLogDashboard :stats="stats" />

      <section class="pantry-list-card access-log-card">
        <div class="section-head compact">
          <h2>访问明细</h2>
          <span class="list-count">共 {{ total }} 条</span>
        </div>

        <div v-if="loading" class="access-log-loading-hint">正在刷新列表…</div>

        <div v-else-if="!logs.length" class="empty-copy">暂无访问记录</div>

        <div v-else class="access-log-table-wrap">
          <table class="access-log-table">
            <thead>
              <tr>
                <th>时间</th>
                <th>用户</th>
                <th>IP</th>
                <th>设备</th>
                <th>访问类型</th>
                <th>页面</th>
                <th>定位</th>
                <th>网络</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in logs" :key="item.id">
                <td>{{ formatTime(item.createdAt) }}</td>
                <td class="access-log-user">{{ formatUser(item) }}</td>
                <td>{{ item.ipAddress || '—' }}</td>
                <td>{{ item.deviceType || '—' }}</td>
                <td>{{ item.accessType || '—' }}</td>
                <td class="access-log-path">{{ item.pagePath || '—' }}</td>
                <td>{{ formatCoords(item) }}</td>
                <td>{{ item.connectionType || '—' }}</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div v-if="total > pageSize" class="access-log-pagination">
          <button class="secondary-btn" type="button" :disabled="page <= 0 || loading" @click="prevPage">上一页</button>
          <span>第 {{ page + 1 }} / {{ Math.ceil(total / pageSize) }} 页</span>
          <button
            class="secondary-btn"
            type="button"
            :disabled="(page + 1) * pageSize >= total || loading"
            @click="nextPage"
          >
            下一页
          </button>
        </div>
      </section>
    </template>
  </main>
</template>

<style scoped>
.admin-page-links {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.access-log-loading-hint {
  padding: 12px 0;
  color: var(--gray-text);
  font-size: 13px;
}

.access-log-table-wrap {
  overflow-x: auto;
}

.access-log-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.access-log-table th,
.access-log-table td {
  padding: 10px 12px;
  border-bottom: 1px solid var(--sage-border);
  text-align: left;
  vertical-align: top;
}

.access-log-table th {
  color: var(--gray-text);
  font-weight: 600;
  white-space: nowrap;
}

.access-log-user {
  max-width: 200px;
  word-break: break-all;
  color: var(--deep-green);
  font-weight: 500;
}

.access-log-path {
  max-width: 180px;
  word-break: break-all;
}

.access-log-pagination {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 16px;
  font-size: 13px;
  color: var(--gray-text);
}
</style>
