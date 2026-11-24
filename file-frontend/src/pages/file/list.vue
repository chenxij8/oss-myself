<template>
  <view class="file-container">
    <!-- 顶部导航 -->
    <view class="top-bar">
      <text class="title">文件管理</text>
    </view>

    <!-- 上传区域 -->
    <view class="upload-section">
      <view class="upload-card" @click="chooseFile">
        <text class="upload-icon">📤</text>
        <text class="upload-text">点击选择文件上传</text>
        <text class="upload-tip">支持常见文件类型（可执行/脚本类禁止），单个最大 2GB</text>
      </view>

      <!-- 上传进度 -->
      <view v-if="uploadProgress > 0 && uploadProgress < 100" class="upload-progress">
        <view class="progress-label">
          <text>上传中...</text>
          <text>{{ uploadProgress }}%</text>
        </view>
        <view class="progress-bar">
          <view class="progress-fill" :style="{ width: uploadProgress + '%' }"></view>
        </view>
      </view>
    </view>

    <!-- 文件列表 -->
    <view class="file-list">
      <view class="list-header">
        <text class="header-title">已上传文件</text>
        <text class="header-count">共 {{ fileList.length }} 个</text>
      </view>

      <view v-if="fileList.length === 0" class="empty-state">
        <text class="empty-icon">📭</text>
        <text class="empty-text">{{ listLoading ? '加载中...' : '暂无文件' }}</text>
      </view>

      <!-- 分页：触底加载更多 -->
      <view v-if="fileList.length > 0" class="load-more" @click="loadMore">
        <text>{{ hasMore ? '点击加载更多' : `已加载全部 ${fileList.length} 个文件` }}</text>
      </view>

      <view v-for="file in fileList" :key="file.id" class="file-item">
        <!-- 文件列表项 -->
        <view class="file-info">
          <text class="file-name">{{ file.fileName }}</text>
          <view class="file-meta">
            <text class="file-size">{{ formatFileSize(file.fileSize) }}</text>
            <text class="file-time">{{ formatTime(file.uploadTime) }}</text>
          </view>
        </view>

        <view class="file-actions">
          <button class="action-btn download-btn" @click="handleDownload(file)">下载</button>
          <button class="action-btn share-btn" @click="handleShare(file.id)">分享</button>
          <button class="action-btn delete-btn" @click="handleDelete(file.id)">删除</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onShow, onHide } from '@dcloudio/uni-app'
import { uploadFileByPath, downloadOwnFile, deleteFile, getFileList } from '@/api/index'
import type { FileInfo } from '@/api/index'
import dayjs from 'dayjs'

const PAGE_SIZE = 20
const fileList = ref<FileInfo[]>([])
const uploadProgress = ref(0)
const listLoading = ref(false)
const currentPage = ref(1)
const hasMore = ref(true)

/** 选择文件：使用 uni.chooseFile 拿到本地路径，交给原生上传通道 */
const chooseFile = () => {
  uni.chooseFile({
    count: 1,
    type: 'file',
    success: res => {
      const filePath = res.tempFilePaths?.[0]
      if (filePath) {
        handleFileUpload(filePath)
      }
    }
  })
}

/** 真实上传：直接上传本地文件路径，并展示真实进度 */
const handleFileUpload = async (filePath: string) => {
  try {
    uploadProgress.value = 0
    const response = await uploadFileByPath(filePath, percent => {
      uploadProgress.value = percent
    })
    uploadProgress.value = 100
    uni.showToast({ title: '上传成功', icon: 'success' })
    fileList.value.unshift(response.data)
    // 缓存失效后重新计数
    currentPage.value = 1
  } catch (error: any) {
    uni.showToast({ title: error?.message || '上传失败', icon: 'none' })
  } finally {
    setTimeout(() => { uploadProgress.value = 0 }, 800)
  }
}

/** 下载自己的文件 */
const handleDownload = async (file: FileInfo) => {
  uni.showLoading({ title: '下载中...' })
  try {
    const tempPath = await downloadOwnFile(file.id)
    uni.hideLoading()
    // App 端可保存到相册/打开文件；H5 端下载到临时路径后提示
    // #ifdef APP-PLUS
    plus.runtime.openFile(tempPath)
    // #endif
    // #ifndef APP-PLUS
    uni.showToast({ title: '已开始下载', icon: 'success' })
    // #endif
  } catch (error: any) {
    uni.hideLoading()
    uni.showToast({ title: error?.message || '下载失败', icon: 'none' })
  }
}

const handleShare = (fileId: number) => {
  uni.navigateTo({ url: `/pages/share/share?fileId=${fileId}` })
}

const handleDelete = async (fileId: number) => {
  uni.showModal({
    title: '确认删除',
    content: '删除后不可恢复，且相关分享链接会立即失效。是否继续？',
    confirmText: '删除',
    cancelText: '取消',
    success: async res => {
      if (!res.confirm) return
      try {
        await deleteFile(fileId)
        uni.showToast({ title: '删除成功', icon: 'success' })
        fileList.value = fileList.value.filter(f => f.id !== fileId)
      } catch (error: any) {
        uni.showToast({ title: error?.message || '删除失败', icon: 'none' })
      }
    }
  })
}

/** 分页加载；已到最后一页则不再请求 */
const loadFileList = async (reset = false) => {
  if (listLoading.value) return
  if (!reset && !hasMore.value) return
  if (reset) {
    currentPage.value = 1
    hasMore.value = true
  }
  try {
    listLoading.value = true
    const response = await getFileList(currentPage.value, PAGE_SIZE)
    const list = response.data || []
    fileList.value = reset ? list : fileList.value.concat(list)
    hasMore.value = list.length === PAGE_SIZE
    if (hasMore.value) currentPage.value++
  } catch {
    // 错误提示由 http 拦截器统一处理
  } finally {
    listLoading.value = false
  }
}

const loadMore = () => loadFileList(false)

const formatFileSize = (bytes: number) => {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.min(Math.floor(Math.log(bytes) / Math.log(k)), sizes.length - 1)
  return (bytes / Math.pow(k, i)).toFixed(2) + ' ' + sizes[i]
}

const formatTime = (value: string) => {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm:ss') : '--'
}

let progressTimer: ReturnType<typeof setTimeout> | null = null
onShow(() => {
  // 从分享页返回时刷新，保证列表与服务端一致
  loadFileList(true)
})
onHide(() => {
  if (progressTimer) clearTimeout(progressTimer)
})
</script>

<style scoped>
.file-container {
  width: 100%;
  background-color: #f5f7fa;
  min-height: 100vh;
}

.top-bar {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 15px 20px;
  font-size: 18px;
  font-weight: bold;
}

.upload-section {
  padding: 20px;
}

.upload-card {
  background: white;
  border-radius: 12px;
  padding: 40px 20px;
  text-align: center;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transition: transform 0.2s, box-shadow 0.2s;
}

.upload-card:active {
  transform: scale(0.98);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.upload-icon {
  display: block;
  font-size: 48px;
  margin-bottom: 12px;
}

.upload-text {
  display: block;
  font-size: 16px;
  color: #333;
  font-weight: bold;
  margin-bottom: 8px;
}

.upload-tip {
  display: block;
  font-size: 12px;
  color: #999;
}

.upload-progress {
  margin-top: 20px;
}

.progress-label {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #999;
  margin-bottom: 8px;
}

.progress-bar {
  width: 100%;
  height: 6px;
  background-color: #e9ecef;
  border-radius: 3px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
  transition: width 0.3s ease;
}

.file-list {
  padding: 20px;
}

.list-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
}

.header-title {
  font-size: 16px;
  font-weight: bold;
  color: #333;
}

.header-count {
  font-size: 14px;
  color: #999;
}

.empty-state {
  text-align: center;
  padding: 40px 20px;
  background: white;
  border-radius: 12px;
}

.empty-icon {
  display: block;
  font-size: 48px;
  margin-bottom: 12px;
}

.empty-text {
  display: block;
  font-size: 14px;
  color: #999;
}

.file-item {
  background: white;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.file-info {
  flex: 1;
}

.file-name {
  display: block;
  font-size: 14px;
  font-weight: bold;
  color: #333;
  margin-bottom: 8px;
  word-break: break-all;
}

.file-meta {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #999;
}

.file-size {
  /* */
}

.file-time {
  /* */
}

.file-actions {
  display: flex;
  gap: 8px;
  margin-left: 12px;
}

.action-btn {
  padding: 8px 12px;
  border-radius: 6px;
  font-size: 12px;
  border: none;
  cursor: pointer;
  transition: transform 0.2s;
}

.action-btn:active {
  transform: scale(0.95);
}

.download-btn {
  background-color: #51cf66;
  color: white;
}

.share-btn {
  background-color: #667eea;
  color: white;
}

.delete-btn {
  background-color: #ff6b6b;
  color: white;
}

.load-more {
  text-align: center;
  padding: 16px;
  color: #667eea;
  font-size: 13px;
}
</style>
