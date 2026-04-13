import { createSSRApp } from 'vue' // 必须用 createSSRApp
import App from './App.vue'
import { createPinia } from 'pinia'
import persistedstate from 'pinia-plugin-persistedstate'

// 必须导出一个名为 createApp 的函数
export function createApp() {
  const app = createSSRApp(App)
  
  const pinia = createPinia()
  pinia.use(persistedstate)
  
  app.use(pinia)

  // 返回一个对象，供 uni-app 底层调用
  return {
    app,
    pinia
  }
}