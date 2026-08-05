import axios from 'axios'

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '/api/v1',
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' },
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('fridgeclear_access_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

http.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status
    const url = typeof error.config?.url === 'string' ? error.config.url : ''

    if (status === 401) {
      localStorage.removeItem('fridgeclear_access_token')
      localStorage.removeItem('fridgeclear_user')
      window.dispatchEvent(new Event('fridgeclear:unauthorized'))
    } else if (status === 403 && !url.includes('/admin/')) {
      // 非管理接口的 403 多半是登录态失效，按未登录处理
      localStorage.removeItem('fridgeclear_access_token')
      localStorage.removeItem('fridgeclear_user')
      window.dispatchEvent(new Event('fridgeclear:unauthorized'))
    } else if (status === 403) {
      window.dispatchEvent(new CustomEvent('fridgeclear:forbidden', {
        detail: error.response?.data?.message ?? '无权访问该资源',
      }))
    }
    return Promise.reject(error)
  },
)

export default http
