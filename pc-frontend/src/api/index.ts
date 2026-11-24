import http, { BASE_URL, TOKEN_KEY } from './http'

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

export interface SystemMonitor {
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

export interface SysUser {
  id: number
  username: string
  email?: string
  role: string
  status: number
  createTime: string
  lastLogin?: string
}

/* ---------------- 认证 ---------------- */
export const login = (username: string, password: string) =>
  http.post<LoginResponse>('/auth/login', { username, password })
export const register = (username: string, password: string, email?: string) =>
  http.post('/auth/register', { username, password, email })

/* ---------------- 文件托管 ---------------- */
export const getFileList = (page = 1, size = 50) =>
  http.get<FileInfo[]>('/files', { params: { page, size } })
export const checkFile = (fileHash: string, fileName: string, fileSize: number) =>
  http.post('/files/check', { fileHash, fileName, fileSize })
export const deleteFile = (id: number) => http.delete(`/files/${id}`)
export const getFileInfo = (id: number) => http.get<FileInfo>(`/files/${id}`)

/** 带进度的文件上传（XHR），与 uniapp 端行为对齐 */
export function uploadFile(
  file: File,
  onProgress?: (percent: number) => void
): Promise<{ data: FileInfo }> {
  return new Promise((resolve, reject) => {
    const formData = new FormData()
    formData.append('file', file)
    const token = localStorage.getItem(TOKEN_KEY)
    const xhr = new XMLHttpRequest()
    xhr.open('POST', `${BASE_URL}/files/upload`)
    if (token) xhr.setRequestHeader('Authorization', `Bearer ${token}`)
    xhr.upload.onprogress = e => {
      if (e.lengthComputable) onProgress?.(Math.round((e.loaded / e.total) * 100))
    }
    xhr.onload = () => {
      if (xhr.status === 200) {
        try {
          const data = JSON.parse(xhr.responseText)
          if (data.code === 0) resolve({ data: data.data })
          else reject(new Error(data.message || '上传失败'))
        } catch {
          reject(new Error('服务器响应格式异常'))
        }
      } else if (xhr.status === 401) {
        localStorage.removeItem(TOKEN_KEY)
        reject(new Error('登录已过期，请重新登录'))
      } else {
        reject(new Error(`上传失败（HTTP ${xhr.status}）`))
      }
    }
    xhr.onerror = () => reject(new Error('网络异常，上传失败'))
    xhr.send(formData)
  })
}

/** 带鉴权的文件下载：用 fetch 携带 Bearer 头，再转为 blob 触发浏览器保存 */
export async function downloadFile(fileId: number, fileName: string): Promise<void> {
  const token = localStorage.getItem(TOKEN_KEY) || ''
  const response = await fetch(`${BASE_URL}/files/download/${fileId}`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {}
  })
  if (!response.ok) throw new Error(response.status === 403 ? '无权限下载此文件' : '下载失败')
  const blob = await response.blob()
  const url = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = fileName
  document.body.appendChild(anchor)
  anchor.click()
  anchor.remove()
  URL.revokeObjectURL(url)
}

/* ---------------- 分享 ---------------- */
export const createShare = (fileId: number, expireTime?: string, maxDownloads?: number) =>
  http.post<FileShareResponse>('/shares', { fileId, expireTime, maxDownloads })
export const getShareList = (page = 1, size = 50) =>
  http.get<FileShareResponse[]>('/shares', { params: { page, size } })
export const getShareInfo = (token: string) => http.get<FileShareResponse>(`/shares/info/${token}`)
export const deleteShare = (id: number) => http.delete(`/shares/${id}`)
export const shareDownloadUrl = (token: string) => `${BASE_URL}/shares/download/${token}`
export const sharePageUrl = (token: string) =>
  `${location.origin}/#/share/access?token=${encodeURIComponent(token)}`

/* ---------------- 图床 ---------------- */
export const getGalleryList = (page = 1, size = 50, mediaType?: string) =>
  http.get<GalleryAsset[]>('/gallery', { params: { page, size, mediaType } })
export const getGalleryStats = () => http.get<GalleryStats>('/gallery/stats')
export const deleteGalleryAsset = (id: number) => http.delete(`/gallery/${id}`)
export const galleryPreviewUrl = (id: number) => `${BASE_URL}/gallery/preview/${id}`
export const galleryRawUrl = (id: number) => `${BASE_URL}/gallery/raw/${id}`

export function uploadGalleryFile(
  file: File,
  onProgress?: (percent: number) => void
): Promise<{ data: GalleryAsset }> {
  return new Promise((resolve, reject) => {
    const formData = new FormData()
    formData.append('file', file)
    const token = localStorage.getItem(TOKEN_KEY)
    const xhr = new XMLHttpRequest()
    xhr.open('POST', `${BASE_URL}/gallery/upload`)
    if (token) xhr.setRequestHeader('Authorization', `Bearer ${token}`)
    xhr.upload.onprogress = e => {
      if (e.lengthComputable) onProgress?.(Math.round((e.loaded / e.total) * 100))
    }
    xhr.onload = () => {
      if (xhr.status === 200) {
        try {
          const data = JSON.parse(xhr.responseText)
          if (data.code === 0) resolve({ data: data.data })
          else reject(new Error(data.message || '上传失败'))
        } catch {
          reject(new Error('服务器响应格式异常'))
        }
      } else {
        reject(new Error(`上传失败（HTTP ${xhr.status}）`))
      }
    }
    xhr.onerror = () => reject(new Error('网络异常，上传失败'))
    xhr.send(formData)
  })
}

/* ---------------- 监控 ---------------- */
export const getSystemStatus = () => http.get<SystemMonitor>('/monitor/status')

/* ---------------- 管理端 ---------------- */
export const adminListUsers = (page = 1, size = 50, keyword?: string) =>
  http.get<SysUser[]>('/admin/users', { params: { page, size, keyword } })
export const adminStats = () => http.get<{ userCount: number }>('/admin/stats')
export const adminUpdateUserStatus = (id: number, status: number) =>
  http.put(`/admin/users/${id}/status`, null, { params: { status } })
export const adminUpdateUserRole = (id: number, role: string) =>
  http.put(`/admin/users/${id}/role`, null, { params: { role } })
export const adminResetPassword = (id: number, password: string) =>
  http.put(`/admin/users/${id}/password`, null, { params: { password } })

/* ---------------- 工具 ---------------- */
export function formatFileSize(bytes: number | undefined): string {
  if (!bytes) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.min(Math.floor(Math.log(bytes) / Math.log(1024)), units.length - 1)
  return `${(bytes / Math.pow(1024, i)).toFixed(2)} ${units[i]}`
}
