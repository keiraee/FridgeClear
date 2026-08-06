import { computed, onUnmounted, ref } from 'vue'

export function formatElapsedLabel(totalSeconds: number): string {
  const minutes = Math.floor(totalSeconds / 60)
  const seconds = totalSeconds % 60
  return minutes ? `${minutes} 分 ${String(seconds).padStart(2, '0')} 秒` : `${seconds} 秒`
}

export function useElapsedTimer() {
  const elapsedSeconds = ref(0)
  let timer: ReturnType<typeof setInterval> | undefined

  const elapsedLabel = computed(() => formatElapsedLabel(elapsedSeconds.value))

  function start() {
    stop()
    elapsedSeconds.value = 0
    timer = setInterval(() => {
      elapsedSeconds.value += 1
    }, 1000)
  }

  function stop() {
    if (timer) clearInterval(timer)
    timer = undefined
  }

  function reset() {
    elapsedSeconds.value = 0
  }

  onUnmounted(stop)

  return { elapsedSeconds, elapsedLabel, start, stop, reset }
}

export type LoadingStage = {
  until: number
  text: string
}

export function resolveLoadingStage(stages: LoadingStage[], elapsedSeconds: number): string {
  for (const stage of stages) {
    if (elapsedSeconds < stage.until) return stage.text
  }
  return stages[stages.length - 1]?.text ?? '加载中…'
}

export const DEFAULT_LOADING_STAGES: LoadingStage[] = [
  { until: 3, text: '正在连接服务…' },
  { until: 8, text: '正在读取数据…' },
  { until: Infinity, text: '仍在处理，请稍候…' },
]

export const PANTRY_LOADING_STAGES: LoadingStage[] = [
  { until: 2, text: '正在加载库存…' },
  { until: 6, text: '正在整理到期信息…' },
  { until: Infinity, text: '库存较多，仍在加载…' },
]

export const RECOMMEND_LOADING_STAGES: LoadingStage[] = [
  { until: 4, text: '正在读取库存食材…' },
  { until: 12, text: '正在匹配可做菜谱…' },
  { until: Infinity, text: '匹配较慢，请稍候…' },
]

export const RECIPE_LIST_LOADING_STAGES: LoadingStage[] = [
  { until: 3, text: '正在加载菜谱列表…' },
  { until: 8, text: '正在获取封面和摘要…' },
  { until: Infinity, text: '列表较大，仍在加载…' },
]

export const RECIPE_DETAIL_LOADING_STAGES: LoadingStage[] = [
  { until: 3, text: '正在加载菜谱…' },
  { until: 8, text: '正在加载食材和步骤…' },
  { until: Infinity, text: '仍在加载详情…' },
]

export const MEAL_PLAN_LOADING_STAGES: LoadingStage[] = [
  { until: 10, text: '正在读取你的库存和临期食材…' },
  { until: 30, text: '正在分析菜谱和你的饮食条件…' },
  { until: Infinity, text: '正在等待 AI 模型返回规划结果…' },
]
