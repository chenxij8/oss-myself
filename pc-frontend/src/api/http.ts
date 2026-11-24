import axios, { type AxiosInstance, type InternalAxiosRequestConfig } from 'axios'
import router from '@/router'

export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
  timestamp: number
}

const BASE_URL = (import.meta.env.VITE_API_BASE_URL || '/api').replace(/\/$/, '')
const TOKEN_KEY = 'access_token'

const http: AxiosInstance = axios.create({
  baseURL: BASE_URL,
  timeout: 30000,
  headers: { 'Content-Type': 'application/json;charset=utf-8' }
})

http.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

let redirecting = false
function handleUnauthorized() {
  if (redirecting) return
  redirecting = true
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem('user_info')
  if (router.currentRoute.value.name !== 'login') {
    router.push({ name: 'login', query: { redirect: router.currentRoute.value.fullPath } })
  }
  setTimeout(() => { redirecting = false }, 1000)
}

http.interceptors.response.use(
  response => {
    const data = response.data as ApiResponse
    if (!data || typeof data !== 'object' || !('code' in data)) {
      return Promise.reject(new Error('服务器返回数据格式异常'))
    }
    if (data.code === 0) return data as any
    if (data.code === 401) handleUnauthorized()
    return Promise.reject(new Error(data.message || '请求失败'))
  },
  error => {
    const status = error.response?.status
    const data = error.response?.data as ApiResponse | undefined
    if (status === 401 || data?.code === 401) {
      handleUnauthorized()
      return Promise.reject(new Error('登录已过期，请重新登录'))
    }
    let msg = '网络异常，请检查网络或后端服务'
    if (status === 403) msg = '无权限访问此资源'
    else if (data?.message) msg = data.message
    else if (status === 413) msg = '文件过大，请压缩后重试'
    else if (status === 500) msg = '服务器内部错误，请稍后重试'
    return Promise.reject(new Error(msg))
  }
)

export { BASE_URL, TOKEN_KEY }
export default http
