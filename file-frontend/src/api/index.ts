import http, { API_BASE_URL, ApiResponse } from './http'

/* ------------------------- 领域类型 ------------------------- */
export interface LoginResponse {
  accessToken: string
  tokenType: string
  expiresIn: number
  userId: number
  username: string
  role: string
}

export interface FileInfo {
  id: number
  fileName: string
  fileSize: number
  filePath?: string
  mimeType?: string
  uploadUserId?: number
  uploadTime: string
}

export interface FileShareResponse {
  id: number
  fileId: number
  shareToken: string
  fileName: string
  fileSize: number
  expireTime?: string
  maxDownloads?: number
  currentDownloads?: number
  shareUrl: string
  createTime: string
}

export interface SystemMonitorResponse {
  cpuUsage: number
  memoryUsed: number
  memoryTotal: number
  memoryUsagePercent: number
  diskTotal: number
  diskUsed: number
  diskFree: number
  diskUsagePercent: number
  loadAverage1m: number
  loadAverage5m: number
  loadAverage15m: number
  uptime: number
  timestamp: number
}

/* ------------------------- 普通 HTTP 接口 ------------------------- */
export const login = (username: string, password: string) =>
  http.post<LoginResponse>('/auth/login', { username, password })

export const getSystemStatus = () => http.get<SystemMonitorResponse>('/monitor/status')

export const checkFile = (fileHash: string, fileName: string, fileSize: number) =>
  http.post('/files/check', { fileHash, fileName, fileSize })

/** 获取文件详情 */
export const getFileInfo = (fileId: number) => http.get<FileInfo>(`/files/${fileId}`)

/** 分页获取文件列表 */
export const getFileList = (page = 1, size = 50) => http.get<FileInfo[]>('/files', { params: { page, size } })

export const deleteFile = (fileId: number) => http.delete(`/files/${fileId}`)

/** expireTime 使用 ISO 8601（LocalDateTime 反序列化要求） */
export const createShare = (fileId: number, expireTime?: string, maxDownloads?: number) =>
  http.post<FileShareResponse>('/shares', { fileId, expireTime, maxDownloads })

export const getShareInfo = (shareToken: string) =>
  http.get<FileShareResponse>(`/shares/info/${shareToken}`)

export const deleteShare = (shareId: number) => http.delete(`/shares/${shareId}`)

export const getUserShares = (page = 1, size = 50) =>
  http.get<FileShareResponse[]>('/shares', { params: { page, size } })

/* ------------------------- 跨端文件传输 ------------------------- */

/**
 * 通过 uni.uploadFile 上传本地文件。
 * 说明：App/小程序端没有标准 File/FormData，必须走 uni 的原生上传通道，
 * 并且它能提供真实进度回调，避免大文件一次性载入内存。
 */
export function uploadFileByPath(
  filePath: string,
  onProgress?: (percent: number) => void
): Promise<ApiResponse<FileInfo>> {
  return new Promise((resolve, reject) => {
    const token = uni.getStorageSync('access_token')
    const task = uni.uploadFile({
      url: `${API_BASE_URL}/files/upload`,
      filePath,
      name: 'file',
      header: token ? { Authorization: `Bearer ${token}` } : {},
      success: res => {
        let data: any
        try {
          data = JSON.parse(res.statusCode === 200 ? res.data : '{}')
        } catch {
          data = null
        }
        if (res.statusCode === 200 && data && data.code === 0) {
          resolve(data as ApiResponse<FileInfo>)
        } else if (res.statusCode === 401 || data?.code === 401) {
          uni.removeStorageSync('access_token')
          reject(new Error('登录已过期，请重新登录'))
        } else {
          reject(new Error(data?.message || `上传失败（HTTP ${res.statusCode}）`))
        }
      },
      fail: err => reject(new Error(err.errMsg || '上传失败'))
    })

    task.onProgressUpdate?.(res => onProgress?.(res.progress))
  })
}

/** 下载分享文件（公开接口），返回本地临时文件路径 */
export function downloadShareFile(shareToken: string): Promise<string> {
  return new Promise((resolve, reject) => {
    uni.downloadFile({
      url: `${API_BASE_URL}/shares/download/${shareToken}`,
      success: res => {
        if (res.statusCode === 200) {
          resolve(res.tempFilePath)
        } else {
          reject(new Error(res.statusCode === 410 ? '链接已过期或下载次数已用完' : '下载失败'))
        }
      },
      fail: err => reject(new Error(err.errMsg || '下载失败'))
    })
  })
}

/** 下载自己上传的文件（需要认证） */
export function downloadOwnFile(fileId: number): Promise<string> {
  return new Promise((resolve, reject) => {
    const token = uni.getStorageSync('access_token')
    uni.downloadFile({
      url: `${API_BASE_URL}/files/download/${fileId}`,
      header: token ? { Authorization: `Bearer ${token}` } : {},
      success: res => {
        if (res.statusCode === 200) {
          resolve(res.tempFilePath)
        } else {
          reject(new Error(res.statusCode === 403 ? '无权限下载此文件' : '下载失败'))
        }
      },
      fail: err => reject(new Error(err.errMsg || '下载失败'))
    })
  })
}
