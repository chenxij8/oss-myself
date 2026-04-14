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
        <text class="upload-tip">支持任何类型文件，单个最大 2GB</text>
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
        <text class="empty-text">暂无文件</text>
      </view>

      <view v-for="file in fileList" :key="file.id" class="file-item">
        <view class="file-info">
          <text class="file-name">{{ file.fileName }}</text>
          <view class="file-meta">
            <text class="file-size">{{ formatFileSize(file.fileSize) }}</text>
            <text class="file-time">{{ formatTime(file.uploadTime) }}</text>
          </view>
        </view>

        <view class="file-actions">
          <button class="action-btn share-btn" @click="handleShare(file.id)">
            分享
          </button>
          <button class="action-btn delete-btn" @click="handleDelete(file.id)">
            删除
          </button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { uploadFile, deleteFile } from '@/api/index'
import dayjs from 'dayjs'

// 文件列表
const fileList = ref<any[]>([])
const uploadProgress = ref(0)

/**
 * 选择文件
 */
const chooseFile = () => {
  // uni-app 文件选择
  uni.chooseFile({
    count: 1,
    type: 'file',
    success: (res) => {
      if (res.tempFilePaths && res.tempFilePaths.length > 0) {
        const filePath = res.tempFilePaths[0]
        handleFileUpload(filePath)
      }
    }
  })
}

/**
 * 处理文件上传
 */
const handleFileUpload = async (filePath: string) => {
  try {
    uploadProgress.value = 0

    // 读取文件
    uni.getFileInfo({
      filePath,
      success: async (fileInfo) => {
        // 创建 File 对象（实际项目中需要处理）
        const file = new File([filePath], filePath, {})

        // 上传文件
        const response = await uploadFile(file)

        if (response.code === 0) {
          uploadProgress.value = 100
          uni.showToast({ title: '上传成功', icon: 'success' })

          // 清除进度
          setTimeout(() => {
            uploadProgress.value = 0
          }, 1000)

          // 添加到列表
          fileList.value.unshift(response.data)
        }
      }
    })
  } catch (error: any) {
    uni.showToast({ title: error.message || '上传失败', icon: 'error' })
    uploadProgress.value = 0
  }
}

/**
 * 处理分享
 */
const handleShare = (fileId: number) => {
  uni.navigateTo({
    url: `/pages/share/config?fileId=${fileId}`
  })
}

/**
 * 处理删除
 */
const handleDelete = async (fileId: number) => {
  uni.showModal({
    title: '确认删除',
    content: '删除文件后将无法恢复，是否继续？',
    confirmText: '删除',
    cancelText: '取消',
    success: async (res) => {
      if (res.confirm) {
        try {
          const response = await deleteFile(fileId)

          if (response.code === 0) {
            uni.showToast({ title: '删除成功', icon: 'success' })
            fileList.value = fileList.value.filter(f => f.id !== fileId)
          }
        } catch (error: any) {
          uni.showToast({ title: error.message || '删除失败', icon: 'error' })
        }
      }
    }
  })
}

/**
 * 格式化文件大小
 */
const formatFileSize = (bytes: number) => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return (bytes / Math.pow(k, i)).toFixed(2) + ' ' + sizes[i]
}

/**
 * 格式化时间
 */
const formatTime = (timestamp: string) => {
  return dayjs(timestamp).format('YYYY-MM-DD HH:mm:ss')
}

/**
 * 组件挂载
 */
onMounted(() => {
  // 这里实际应该调用 API 获取文件列表
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

.share-btn {
  background-color: #667eea;
  color: white;
}

.delete-btn {
  background-color: #ff6b6b;
  color: white;
}
</style>
