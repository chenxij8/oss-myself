import http from './http'

/**
 * 登录 API
 */
export const login = (username: string, password: string) => {
  return http.post('/auth/login', { username, password })
}

/**
 * 获取系统监控数据
 */
export const getSystemStatus = () => {
  return http.get('/monitor/status')
}

/**
 * 检查文件是否存在（秒传）
 */
export const checkFile = (fileHash: string, fileName: string, fileSize: number) => {
  return http.post('/files/check', { fileHash, fileName, fileSize })
}

/**
 * 上传文件
 */
export const uploadFile = (file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  
  return http.post('/files/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

/**
 * 获取文件详情
 */
export const getFileInfo = (fileId: number) => {
  return http.get(`/files/${fileId}`)
}

/**
 * 删除文件
 */
export const deleteFile = (fileId: number) => {
  return http.delete(`/files/${fileId}`)
}

/**
 * 创建分享链接
 */
export const createShare = (fileId: number, expireTime?: string, maxDownloads?: number) => {
  return http.post('/shares', { fileId, expireTime, maxDownloads })
}

/**
 * 获取分享信息（公开，无需认证）
 */
export const getShareInfo = (shareToken: string) => {
  return http.get(`/shares/info/${shareToken}`)
}

/**
 * 删除分享链接
 */
export const deleteShare = (shareId: number) => {
  return http.delete(`/shares/${shareId}`)
}

/**
 * 获取用户分享列表
 */
export const getUserShares = () => {
  return http.get('/shares')
}
