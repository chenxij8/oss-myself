import axios, { AxiosInstance, AxiosRequestConfig } from 'axios'

/**
 * API 响应格式
 */
export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
  timestamp: number
  requestId: string
}

class HttpClient {
  private instance: AxiosInstance

  constructor() {
    this.instance = axios.create({
      baseURL: '/api',
      timeout: 30000,
      headers: {
        'Content-Type': 'application/json;charset=utf-8'
      }
    })

    // 请求拦截器
    this.instance.interceptors.request.use(
      config => {
        // 从本地存储获取 token
        const token = uni.getStorageSync('access_token')
        if (token) {
          config.headers.Authorization = `Bearer ${token}`
        }
        return config
      },
      error => Promise.reject(error)
    )

    // 响应拦截器
    this.instance.interceptors.response.use(
      response => {
        const data = response.data as ApiResponse
        
        // 根据业务码判断
        if (data.code === 0) {
          return data
        } else if (data.code === 401) {
          // 未认证，跳转登录
          uni.removeStorageSync('access_token')
          uni.navigateTo({ url: '/pages/login/login' })
          return Promise.reject(data)
        } else if (data.code === 403) {
          // 无权限
          uni.showToast({ title: '无权限访问', icon: 'error' })
          return Promise.reject(data)
        } else {
          // 其他错误
          return Promise.reject(data)
        }
      },
      error => {
        uni.showToast({ title: '网络请求异常', icon: 'error' })
        return Promise.reject(error)
      }
    )
  }

  /**
   * GET 请求
   */
  async get<T = any>(url: string, config?: AxiosRequestConfig): Promise<ApiResponse<T>> {
    return this.instance.get(url, config)
  }

  /**
   * POST 请求
   */
  async post<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<ApiResponse<T>> {
    return this.instance.post(url, data, config)
  }

  /**
   * PUT 请求
   */
  async put<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<ApiResponse<T>> {
    return this.instance.put(url, data, config)
  }

  /**
   * DELETE 请求
   */
  async delete<T = any>(url: string, config?: AxiosRequestConfig): Promise<ApiResponse<T>> {
    return this.instance.delete(url, config)
  }

  /**
   * 获取原始 axios 实例
   */
  getAxios(): AxiosInstance {
    return this.instance
  }
}

export default new HttpClient()
