<script setup lang="ts">
import { onMounted, ref } from 'vue'
import {
  fetchSystemAiConfigModels,
  getSystemAiConfig,
  testSystemAiConfig,
  updateSystemAiConfig,
  type ModelItem,
  type SystemAiConfigUpdate,
} from '../api/admin'
import LoadingWait from '../components/LoadingWait.vue'

const protocolOptions = [
  { value: 'OPENAI_CHAT', label: 'OpenAI Chat（兼容接口）' },
]

const form = ref<SystemAiConfigUpdate>({ providerName: '', protocol: 'OPENAI_CHAT', baseUrl: '', modelName: '', apiKey: '', enabled: true })
const apiKeyConfigured = ref(false)
const modelOptions = ref<ModelItem[]>([])
const loading = ref(false)
const fetching = ref(false)
const testing = ref(false)
const saving = ref(false)
const message = ref('')
const messageType = ref<'success' | 'error'>('success')

function showMessage(text: string, type: 'success' | 'error') {
  message.value = text
  messageType.value = type
}

async function load() {
  loading.value = true
  message.value = ''
  try {
    const config = await getSystemAiConfig()
    form.value = {
      providerName: config.providerName,
      protocol: config.protocol,
      baseUrl: config.baseUrl,
      modelName: config.modelName,
      apiKey: '',
      enabled: config.enabled,
    }
    apiKeyConfigured.value = config.apiKeyConfigured
  } catch {
    showMessage('加载全局 AI 配置失败', 'error')
  } finally {
    loading.value = false
  }
}

async function handleFetchModels() {
  if (!form.value.baseUrl.trim()) {
    showMessage('请先填写 Base URL', 'error')
    return
  }
  fetching.value = true
  message.value = ''
  try {
    modelOptions.value = await fetchSystemAiConfigModels({
      protocol: form.value.protocol,
      baseUrl: form.value.baseUrl,
      apiKey: form.value.apiKey || undefined,
    })
    showMessage(`连接成功，获取到 ${modelOptions.value.length} 个模型`, 'success')
  } catch (error) {
    modelOptions.value = []
    const axiosError = error as { response?: { data?: { message?: string } } }
    showMessage(axiosError.response?.data?.message ?? '获取模型失败，请检查地址和密钥', 'error')
  } finally {
    fetching.value = false
  }
}

async function handleTestConnection() {
  if (!apiKeyConfigured.value && !form.value.apiKey?.trim()) {
    showMessage('请先填写并保存 API Key，再测试连接', 'error')
    return
  }
  testing.value = true
  message.value = ''
  try {
    if (form.value.apiKey?.trim()) {
      await updateSystemAiConfig({ ...form.value })
      form.value.apiKey = ''
    }
    const result = await testSystemAiConfig()
    if (result.success) {
      showMessage(`${result.message}（${result.modelCount} 个模型）`, 'success')
    } else {
      showMessage(result.message, 'error')
    }
  } catch (error) {
    const axiosError = error as { response?: { data?: { message?: string } } }
    showMessage(axiosError.response?.data?.message ?? '连接测试失败', 'error')
  } finally {
    testing.value = false
  }
}

async function handleSave() {
  saving.value = true
  message.value = ''
  try {
    const saved = await updateSystemAiConfig({ ...form.value })
    form.value.apiKey = ''
    apiKeyConfigured.value = saved.apiKeyConfigured
    showMessage('配置已保存', 'success')
  } catch (error) {
    const axiosError = error as { response?: { data?: { message?: string } } }
    showMessage(axiosError.response?.data?.message ?? '保存失败，请稍后重试', 'error')
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  await load()
  if (apiKeyConfigured.value && form.value.baseUrl) await handleFetchModels()
})
</script>

<template>
  <main class="page-main admin-ai-config-page">
    <div class="page-heading-row">
      <div>
        <h1>全局 AI 配置</h1>
        <p class="page-desc">平台统一维护 AI 接口，普通用户无需自行配置密钥。</p>
      </div>
    </div>

    <LoadingWait
      v-if="loading"
      :active="loading"
      hint="通常 1–3 秒"
      compact
    />

    <section v-else class="pantry-form-card">
      <p class="protocol-note">备餐计划当前仅支持 <strong>OpenAI 兼容接口</strong>（如 OpenAI、DeepSeek、通义等）。请填写兼容 <code>/chat/completions</code> 的 Base URL。</p>

      <form class="pantry-form" @submit.prevent="handleSave">
        <label class="wide">
          服务名称
          <input v-model="form.providerName" type="text" maxlength="128" placeholder="例如：平台默认" required />
        </label>
        <label>
          协议
          <select v-model="form.protocol">
            <option v-for="option in protocolOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
          </select>
        </label>
        <label class="wide">
          Base URL
          <input v-model="form.baseUrl" type="text" placeholder="https://api.deepseek.com" required />
        </label>
        <label>
          API Key
          <input v-model="form.apiKey" type="password" autocomplete="new-password" :placeholder="apiKeyConfigured ? '已配置，留空则不修改' : '首次保存必填'" />
        </label>
        <label class="wide model-row">
          模型名称
          <div class="model-control">
            <select v-model="form.modelName">
              <option value="">（留空：保存时自动选择首个模型）</option>
              <option v-for="option in modelOptions" :key="option.id" :value="option.id">{{ option.name }}</option>
            </select>
            <button class="admin-btn fetch-btn" type="button" :disabled="fetching || saving || testing" @click="handleFetchModels">
              {{ fetching ? '获取中…' : '获取模型' }}
            </button>
          </div>
        </label>
        <label class="check-row">
          <input v-model="form.enabled" type="checkbox" />
          启用全局 AI 服务
        </label>

        <div class="form-actions">
          <button class="admin-btn save-btn" type="submit" :disabled="saving || fetching || testing">
            {{ saving ? '保存中…' : '保存配置' }}
          </button>
          <button class="admin-btn fetch-btn" type="button" :disabled="testing || saving || fetching" @click="handleTestConnection">
            {{ testing ? '测试中…' : '测试连接' }}
          </button>
        </div>
        <p v-if="message" class="form-message" :class="messageType">{{ message }}</p>
      </form>
    </section>
  </main>
</template>

<style scoped>
.admin-ai-config-page { padding-top: 48px; max-width: 960px; }
.protocol-note { margin: 0 0 20px; padding: 12px 16px; border-radius: var(--radius-md); background: rgba(122, 158, 126, 0.12); font-size: 14px; line-height: 1.6; color: var(--deep-green); }
.protocol-note code { font-size: 13px; }
.check-row { display: flex; flex-direction: row !important; align-items: center; gap: 8px !important; font-weight: 500; }
.check-row input { width: auto; padding: 0; }
.model-control { display: flex; gap: 10px; }
.model-control select { flex: 1; min-width: 0; }
.form-actions { display: flex; gap: 10px; flex-wrap: wrap; }
.form-message { margin: 14px 0 0; font-size: 13px; }
.form-message.success { color: var(--sage); }
.form-message.error { color: var(--light-orange); }
.admin-btn { padding: 11px 22px; border-radius: var(--radius-md); font-size: 14px; font-weight: 600; }
.admin-btn:disabled { cursor: wait; opacity: .65; }
.save-btn { border: 0; background: var(--light-orange); color: #fff; }
.fetch-btn { border: 1px solid var(--sage-border); background: var(--white); color: var(--deep-green); white-space: nowrap; }
</style>
