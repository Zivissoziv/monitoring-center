import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import zhCn from 'element-plus/es/locale/lang/zh-cn'

// Global fetch interceptor to add Authorization header
const originalFetch = window.fetch
window.fetch = function(url, options = {}) {
  const token = localStorage.getItem('token')
  
  // Only add auth header for API calls
  if (url.startsWith('/api') || url.includes('/api/')) {
    options.headers = {
      'Content-Type': 'application/json',
      ...options.headers
    }
    
    if (token) {
      options.headers['Authorization'] = `Bearer ${token}`
    }
  }
  
  return originalFetch(url, options).then(response => {
    // Handle 401 Unauthorized
    if (response.status === 401 && (url.startsWith('/api') || url.includes('/api/'))) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      window.location.reload()
    }
    return response
  })
}

const app = createApp(App)
const pinia = createPinia()

// Register all icons
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(pinia)
app.use(ElementPlus, {
  locale: zhCn,
})
app.mount('#app')