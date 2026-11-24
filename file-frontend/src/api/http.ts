import axios, { AxiosInstance, AxiosRequestConfig, InternalAxiosRequestConfig } from 'axios'

/** API 响应格式 */
export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
  timestamp: number
  requestId?: string
}

/** API 基地址：H5 开发留空走 Vite 代理，App/生产通过 VITE_API_BASE_URL 指定完整域名 */
const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || '/api').replace(/\/$/, '')
const IS_PROD = import.meta.env.PROD

let redirecting = false

/** 统一处理 401：清理 token 并跳转登录，避免并发请求重复跳转 */
function handleUnauthorized() {
  if (redirecting) return
  redirecting = true
  try {
    uni.removeStorageSync('access_token')
    uni.reLaunch({ url: '/pages/login/login' })
  } finally {
    setTimeout(() => { redirecting = false }, 1000)
  }
}

class HttpClient {
  private instance: AxiosInstance

  constructor() {
    this.instance = axios.create({
      baseURL: API_BASE_URL,
      timeout: 30000,
      headers: { 'Content-Type': 'application/json;charset=utf-8' }
    })

    this.instance.interceptors.request.use(
      (config: InternalAxiosRequestConfig) => {
        const token = uni.getStorageSync('access_token')
        if (token) {
          config.headers.Authorization = `Bearer ${token}`
        }
        return config
      },
      error => Promise.reject(error)
    )

    this.instance.interceptors.response.use(
      response => {
        const data = response.data as ApiResponse
        if (!data || typeof data !== 'object' || !('code' in data)) {
          if (!IS_PROD) console.error('服务器返回非 API 格式数据:', response.status, response.data)
          uni.showToast({ title: '服务器数据格式异常', icon: 'none' })
          return Promise.reject(new Error('Invalid API response format'))
        }
        if (data.code === 0) {
          return data as any
        }
        if (data.code === 401) {
          handleUnauthorized()
          return Promise.reject(data)
        }
        if (data.code === 403) {
          uni.showToast({ title: '无权限访问', icon: 'none' })
          return Promise.reject(data)
        }
        return Promise.reject(data)
      },
      error => {
        const status = error.response?.status
        const responseData = error.response?.data as ApiResponse | undefined
        let msg = '网络请求异常，请检查网络'

        // 关键修复：后端 401 走 HTTP 状态码，进入 rejected 分支；
        // 原实现只检查 data.code，导致该分支永远走不到，登录过期后用户卡死。
        if (status === 401 || responseData?.code === 401) {
          handleUnauthorized()
          return Promise.reject(error)
        }
        if (status === 400 && responseData?.message) {
          msg = responseData.message
        } else if (status === 404) {
          msg = '接口不存在（后端可能未启动或代理配置有误）'
        } else if (status === 413) {
          msg = '文件过大，请压缩后重试'
        } else if (status === 500) {
          msg = '服务器内部错误，请稍后重试'
        }
        if (!IS_PROD) console.error('请求失败:', error.config?.url, status)
        uni.showToast({ title: msg, icon: 'none' })
        return Promise.reject(error)
      }
    )
  }

  async get<T = any>(url: string, config?: AxiosRequestConfig): Promise<ApiResponse<T>> {
    return this.instance.get(url, config)
  }

  async post<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<ApiResponse<T>> {
    return this.instance.post(url, data, config)
  }

  async put<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<ApiResponse<T>> {
    return this.instance.put(url, data, config)
  }

  async delete<T = any>(url: string, config?: AxiosRequestConfig): Promise<ApiResponse<T>> {
    return this.instance.delete(url, config)
  }

  getAxios(): AxiosInstance {
    return this.instance
  }
}

export { API_BASE_URL }
export default new HttpClient()
