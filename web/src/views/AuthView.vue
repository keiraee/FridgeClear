<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const isRegister = computed(() => route.name === 'register')

const submitButtonLabel = computed(() => {
  if (loading.value) return isRegister.value ? '注册中…' : '登录中…'
  return isRegister.value ? '注册' : '登录'
})
const email = ref('')
const password = ref('')
const nickname = ref('')
const loading = ref(false)
const errorMessage = ref('')

function extractError(error: unknown) {
  const response = (error as { response?: { data?: { message?: string } } }).response
  if (response?.data?.message) return response.data.message
  return isRegister.value
    ? '注册失败，请检查网络或稍后重试'
    : '登录失败，请检查网络或稍后重试'
}

async function submit() {
  errorMessage.value = ''
  if (!email.value || password.value.length < 6 || (isRegister.value && !nickname.value)) {
    errorMessage.value = isRegister.value ? '请填写昵称、邮箱和至少 6 位密码' : '请输入邮箱和至少 6 位密码'
    return
  }
  loading.value = true
  try {
    if (isRegister.value) await auth.signUp({ email: email.value, password: password.value, nickname: nickname.value })
    else await auth.signIn(email.value, password.value)
    await router.push('/')
  } catch (error) {
    errorMessage.value = extractError(error)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="auth-page">
    <section class="auth-card">
      <h1>{{ isRegister ? '创建账户' : '登录' }}</h1>
      <p class="auth-intro">{{ isRegister ? '注册后即可管理库存和备餐计划。' : '登录后继续管理库存和备餐计划。' }}</p>
      <form @submit.prevent="submit">
        <label v-if="isRegister">昵称<input v-model.trim="nickname" autocomplete="nickname" placeholder="例如：小厨师" /></label>
        <label>邮箱<input v-model.trim="email" type="email" autocomplete="email" placeholder="you@example.com" /></label>
        <label>密码<input v-model="password" type="password" :autocomplete="isRegister ? 'new-password' : 'current-password'" placeholder="至少 6 位密码" /></label>
        <p v-if="errorMessage" class="form-error">{{ errorMessage }}</p>
        <button class="cta-primary auth-submit" type="submit" :disabled="loading">{{ submitButtonLabel }}</button>
      </form>
      <button class="auth-switch" type="button" @click="router.push(isRegister ? '/login' : '/register')">
        {{ isRegister ? '已有账户？去登录' : '还没有账户？立即注册' }}
      </button>
    </section>
  </main>
</template>
