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
    if (error.response?.status === 401) {
      localStorage.removeItem('fridgeclear_access_token')
      localStorage.removeItem('fridgeclear_user')
      window.dispatchEvent(new Event('fridgeclear:unauthorized'))
    }
    return Promise.reject(error)
  },
)

export default http
