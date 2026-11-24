import http, { API_BASE_URL, ApiResponse } from './http'
import type { PickedFile } from '@/utils/pickFiles'

/**
 * H5 端用原生 XHR + FormData 直传：
 * uni.uploadFile 在 H5 端无法可靠携带 File 的真实文件名与进度，
 * 这里按平台分流，原生端仍走 uni 原生通道。
 */
function uploadByXhr<T>(
  picked: PickedFile,
  endpoint: string,
  onProgress?: (percent: number) => void
): Promise<ApiResponse<T>> {
  return new Promise((resolve, reject) => {
    if (!picked.raw) {
      reject(new Error('当前环境不支持此上传方式'))
      return
    }
    const token = uni.getStorageSync('access_token')
    const formData = new FormData()
    formData.append('file', picked.raw, picked.name)

    const xhr = new XMLHttpRequest()
    xhr.open('POST', `${API_BASE_URL}${endpoint}`)
    if (token) xhr.setRequestHeader('Authorization', `Bearer ${token}`)
    xhr.upload.onprogress = event => {
      if (event.lengthComputable) onProgress?.(Math.round(event.loaded * 100 / event.total))
    }
    xhr.onload = () => {
      releaseBlobUrl(picked)
      let data: any
      try { data = JSON.parse(xhr.responseText) } catch { data = null }
      if (xhr.status === 200 && data?.code === 0) resolve(data as ApiResponse<T>)
      else if (xhr.status === 401 || data?.code === 401) {
        uni.removeStorageSync('access_token')
        reject(new Error('登录已过期，请重新登录'))
      } else {
        reject(new Error(data?.message || `上传失败（HTTP ${xhr.status}）`))
      }
    }
    xhr.onerror = () => { releaseBlobUrl(picked); reject(new Error('网络错误，上传失败')) }
    xhr.send(formData)
  })
}

function releaseBlobUrl(picked: PickedFile) {
  if (picked.raw && picked.path.startsWith('blob:')) URL.revokeObjectURL(picked.path)
}

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

export interface GalleryAsset {
  id: number
  fileName: string
  fileHash: string
  mimeType: string
  mediaType: 'IMAGE' | 'VIDEO'
  fileSize: number
  width?: number
  height?: number
  duration?: number
  previewUrl: string
  rawUrl: string
  createTime: string
}

export interface GalleryStats {
  total: number
  imageCount: number
  videoCount: number
  totalSize: number
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

/** 注册：角色由服务端固定为 USER，注册后进入用户端 */
export const register = (username: string, password: string, email?: string) =>
  http.post('/auth/register', { username, password, email })

export const getSystemStatus = () => http.get<SystemMonitorResponse>('/monitor/status')

/* ---------------- 图床接口（与 PC 端共用同一后端） ---------------- */
export const getGalleryList = (page = 1, size = 50, mediaType?: string) =>
  http.get<GalleryAsset[]>('/gallery', { params: { page, size, mediaType } })
export const getGalleryStats = () => http.get<GalleryStats>('/gallery/stats')
export const deleteGalleryAsset = (id: number) => http.delete(`/gallery/${id}`)
export const galleryPreviewUrl = (id: number) => `${API_BASE_URL}/gallery/preview/${id}`

/**
 * 图床上传：走 uni.uploadFile 原生通道，支持真实进度与 App/小程序跨端
 */
export function uploadGalleryFile(
  file: string | PickedFile,
  onProgress?: (percent: number) => void
): Promise<ApiResponse<GalleryAsset>> {
  if (typeof file !== 'string') return uploadByXhr<GalleryAsset>(file, '/gallery/upload', onProgress)
  return new Promise((resolve, reject) => {
    const token = uni.getStorageSync('access_token')
    const task = uni.uploadFile({
      url: `${API_BASE_URL}/gallery/upload`,
      filePath: file,
      name: 'file',
      header: token ? { Authorization: `Bearer ${token}` } : {},
      success: res => {
        let data: any
        try { data = JSON.parse(res.statusCode === 200 ? res.data : '{}') } catch { data = null }
        if (res.statusCode === 200 && data && data.code === 0) resolve(data as ApiResponse<GalleryAsset>)
        else reject(new Error(data?.message || `上传失败（HTTP ${res.statusCode}）`))
      },
      fail: err => reject(new Error(err.errMsg || '上传失败'))
    })
    task.onProgressUpdate?.(res => onProgress?.(res.progress))
  })
}

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
  file: string | PickedFile,
  onProgress?: (percent: number) => void
): Promise<ApiResponse<FileInfo>> {
  if (typeof file !== 'string') return uploadByXhr<FileInfo>(file, '/files/upload', onProgress)
  return new Promise((resolve, reject) => {
    const token = uni.getStorageSync('access_token')
    const task = uni.uploadFile({
      url: `${API_BASE_URL}/files/upload`,
      filePath: file,
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

/* ---------------- 工具 ---------------- */
export function formatFileSize(bytes: number | undefined): string {
  if (!bytes) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.min(Math.floor(Math.log(bytes) / Math.log(1024)), units.length - 1)
  return `${(bytes / Math.pow(1024, i)).toFixed(2)} ${units[i]}`
}
