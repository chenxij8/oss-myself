import { createApp } from 'vue'
import App from './App.vue'
import { createPinia } from 'pinia'
import persistedstate from 'pinia-plugin-persistedstate'

// 创建应用实例
const app = createApp(App)

// 创建 Pinia store
const pinia = createPinia()
pinia.use(persistedstate)

// 使用插件
app.use(pinia)

// 导出应用
export default app
