import { createApp } from 'vue'
import './style.css'
import App from './App.vue'
import router from './router'
import { createPinia } from 'pinia'
import { useAuthStore } from './stores/auth'
import { initAccessTelemetry } from './services/accessTelemetry'

const app = createApp(App)
const pinia = createPinia()
app.use(pinia).use(router)
useAuthStore(pinia).restore()
initAccessTelemetry(router)
app.mount('#app')
