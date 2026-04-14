<template>
  <view class="share-container">
    <!-- 顶部导航 -->
    <view class="top-bar">
      <text class="title">分享配置</text>
    </view>

    <!-- 文件选择弹窗 -->
    <view v-if="showFileModal" class="file-modal">
      <view class="modal-mask" @click="showFileModal = false"></view>
      <view class="modal-content">
        <view class="modal-header">
          <text class="modal-title">选择文件</text>
          <text class="modal-close" @click="showFileModal = false">×</text>
        </view>
        
        <view class="modal-body">
          <view v-if="fileListLoading" class="loading-state">
            <text>加载中...</text>
          </view>
          
          <view v-else-if="fileList.length === 0" class="empty-state">
            <text class="empty-icon">📭</text>
            <text class="empty-text">暂无已上传文件</text>
            <text class="empty-tip">请先上传文件</text>
          </view>
          
          <scroll-view v-else scroll-y class="file-scroll">
            <view 
              v-for="file in fileList" 
              :key="file.id"
              :class="['file-item', { selected: selectedFileId === file.id }]"
              @click="selectFile(file)"
            >
              <view class="file-icon">📄</view>
              <view class="file-info">
                <text class="file-name">{{ file.fileName }}</text>
                <text class="file-meta">{{ formatFileSize(file.fileSize) }} · {{ formatTime(file.uploadTime) }}</text>
              </view>
              <view v-if="selectedFileId === file.id" class="check-icon">✓</view>
            </view>
          </scroll-view>
        </view>
      </view>
    </view>

    <!-- 配置表单 -->
    <view class="form-section">
      <!-- 文件选择 -->
      <view class="form-group">
        <view class="form-label">选择文件</view>
        <view class="select-file" @click="chooseFile">
          <text>{{ selectedFileName || '点击选择文件' }}</text>
          <text class="arrow">></text>
        </view>
      </view>

      <!-- 过期时间设置 -->
      <view class="form-group">
        <view class="form-label">过期时间</view>
        <view class="radio-group">
          <button
            v-for="option in expireOptions"
            :key="option.value"
            :class="['radio-btn', { active: expireTime === option.value }]"
            @click="expireTime = option.value"
          >
            {{ option.label }}
          </button>
        </view>
      </view>

      <!-- 下载次数限制 -->
      <view class="form-group">
        <view class="form-label-row">
          <text class="form-label">下载次数限制</text>
          <text class="form-check">
            <input
              type="checkbox"
              :checked="enableDownloadLimit"
              @change="enableDownloadLimit = !enableDownloadLimit"
            />
            启用
          </text>
        </view>
        
        <view v-if="enableDownloadLimit" class="form-input-group">
          <input
            v-model.number="maxDownloads"
            type="number"
            class="form-input"
            placeholder="请输入最大下载次数"
            min="1"
          />
        </view>
      </view>

      <!-- 生成分享链接按钮 -->
      <button
        class="generate-btn"
        :disabled="!selectedFileId || loading"
        @click="generateShare"
      >
        {{ loading ? '生成中...' : '生成分享链接' }}
      </button>
    </view>

    <!-- 分享结果 -->
    <view v-if="shareUrl" class="result-section">
      <view class="result-card">
        <view class="result-title">分享链接已生成</view>
        
        <!-- 分享链接 -->
        <view class="link-box">
          <text class="link-label">分享链接</text>
          <view class="link-content">
            <text class="link-text">{{ shareUrl }}</text>
            <button class="copy-btn" @click="copyToClipboard(shareUrl)">
              复制
            </button>
          </view>
        </view>

        <!-- 分享令牌 -->
        <view class="link-box">
          <text class="link-label">分享令牌</text>
          <view class="link-content">
            <text class="link-text">{{ shareToken }}</text>
            <button class="copy-btn" @click="copyToClipboard(shareToken)">
              复制
            </button>
          </view>
        </view>

        <!-- 分享信息 -->
        <view class="share-info">
          <view class="info-item">
            <text class="info-label">有效期</text>
            <text class="info-value">{{ shareExpiryInfo }}</text>
          </view>
          <view class="info-item">
            <text class="info-label">下载限制</text>
            <text class="info-value">{{ shareDownloadInfo }}</text>
          </view>
          <view class="info-item">
            <text class="info-label">创建时间</text>
            <text class="info-value">{{ formatTime(shareCreateTime) }}</text>
          </view>
        </view>

        <!-- 操作按钮 -->
        <view class="action-buttons">
          <button class="btn-primary" @click="copyShareUrl">
            复制完整链接
          </button>
          <button class="btn-secondary" @click="shareAgain">
            重新生成
          </button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { createShare, getFileList, getFileInfo } from '@/api/index'
import dayjs from 'dayjs'

// 表单数据
const selectedFileId = ref<number | null>(null)
const selectedFileName = ref('')
const loading = ref(false)

// 文件列表弹窗
const showFileModal = ref(false)
const fileList = ref<any[]>([])
const fileListLoading = ref(false)

/**
 * 选择文件
 */
const chooseFile = () => {
  uni.showActionSheet({
    itemList: ['从最近文件选择', '从文件系统选择'],
    success: (res) => {
      if (res.tapIndex === 0) {
        // 从最近文件选择
        loadFileList()
      } else if (res.tapIndex === 1) {
        // 从文件系统选择
        uni.chooseFile({
          count: 1,
          type: 'file',
          success: (chooseRes) => {
            if (chooseRes.tempFilePaths && chooseRes.tempFilePaths.length > 0) {
              uni.showToast({ title: '请先上传此文件', icon: 'warning' })
            }
          }
        })
      }
    }
  })
}

/**
 * 加载文件列表
 */
const loadFileList = async () => {
  try {
    fileListLoading.value = true
    const response = await getFileList()
    
    if (response.code === 0) {
      fileList.value = response.data || []
      showFileModal.value = true
    } else {
      uni.showToast({ title: response.message || '获取文件列表失败', icon: 'error' })
    }
  } catch (error: any) {
    uni.showToast({ title: error.message || '获取文件列表失败', icon: 'error' })
  } finally {
    fileListLoading.value = false
  }
}

/**
 * 选择文件
 */
const selectFile = (file: any) => {
  selectedFileId.value = file.id
  selectedFileName.value = file.fileName
  showFileModal.value = false
}

// 过期时间选项
const expireOptions = [
  { label: '不设置', value: null },
  { label: '1小时', value: 1 },
  { label: '1天', value: 24 },
  { label: '7天', value: 7 * 24 },
  { label: '30天', value: 30 * 24 }
]
const expireTime = ref<number | null>(null)

// 下载限制
const enableDownloadLimit = ref(false)
const maxDownloads = ref(10)

// 分享结果
const shareUrl = ref('')
const shareToken = ref('')
const shareExpiryInfo = ref('')
const shareDownloadInfo = ref('')
const shareCreateTime = ref('')

/**
 * 生成分享链接
 */
const generateShare = async () => {
  if (!selectedFileId.value) {
    uni.showToast({ title: '请先选择文件', icon: 'error' })
    return
  }

  try {
    loading.value = true

    // 计算过期时间
    let expireAt: string | undefined = undefined
    if (expireTime.value !== null) {
      expireAt = dayjs().add(expireTime.value, 'hour').format('YYYY-MM-DD HH:mm:ss')
    }

    // 调用 API
    const response = await createShare(
      selectedFileId.value,
      expireAt,
      enableDownloadLimit.value ? maxDownloads.value : undefined
    )

    if (response.code === 0) {
      // 保存分享信息
      const shareData = response.data
      shareToken.value = shareData.shareToken
      shareUrl.value = shareData.shareUrl
      shareCreateTime.value = shareData.createTime

      // 设置有效期信息
      if (shareData.expireTime) {
        const expireDays = dayjs(shareData.expireTime).diff(dayjs(), 'day')
        shareExpiryInfo.value = `${expireDays}天后过期`
      } else {
        shareExpiryInfo.value = '永不过期'
      }

      // 设置下载限制信息
      if (shareData.maxDownloads) {
        shareDownloadInfo.value = `最多 ${shareData.maxDownloads} 次`
      } else {
        shareDownloadInfo.value = '无限制'
      }

      uni.showToast({ title: '分享链接生成成功', icon: 'success' })
    } else {
      uni.showToast({ title: response.message || '生成失败', icon: 'error' })
    }
  } catch (error: any) {
    uni.showToast({ title: error.message || '生成失败', icon: 'error' })
  } finally {
    loading.value = false
  }
}

/**
 * 复制到剪贴板
 */
const copyToClipboard = (text: string) => {
  uni.setClipboardData({
    data: text,
    success: () => {
      uni.showToast({ title: '已复制到剪贴板', icon: 'success' })
    }
  })
}

/**
 * 复制完整分享URL
 */
const copyShareUrl = () => {
  const fullUrl = `${shareUrl.value}?token=${shareToken.value}`
  copyToClipboard(fullUrl)
}

/**
 * 重新生成
 */
const shareAgain = () => {
  shareUrl.value = ''
  shareToken.value = ''
  generateShare()
}
</script>

<style scoped>
.share-container {
  width: 100%;
  background-color: #f5f7fa;
  min-height: 100vh;
  padding-bottom: 20px;
}

/* 文件选择弹窗样式 */
.file-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 999;
  display: flex;
  align-items: flex-end;
}

.modal-mask {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
}

.modal-content {
  position: relative;
  width: 100%;
  max-height: 70vh;
  background-color: white;
  border-radius: 16px 16px 0 0;
  overflow: hidden;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #eee;
}

.modal-title {
  font-size: 16px;
  font-weight: bold;
  color: #333;
}

.modal-close {
  font-size: 24px;
  color: #999;
  padding: 0 8px;
}

.modal-body {
  max-height: 60vh;
  overflow: hidden;
}

.loading-state,
.empty-state {
  padding: 60px 20px;
  text-align: center;
}

.empty-icon {
  display: block;
  font-size: 48px;
  margin-bottom: 12px;
}

.empty-text {
  display: block;
  font-size: 14px;
  color: #666;
  margin-bottom: 8px;
}

.empty-tip {
  display: block;
  font-size: 12px;
  color: #999;
}

.file-scroll {
  max-height: 60vh;
  padding: 8px 0;
}

.file-item {
  display: flex;
  align-items: center;
  padding: 12px 20px;
  border-bottom: 1px solid #f5f5f5;
  transition: background-color 0.2s;
}

.file-item:active {
  background-color: #f5f5f5;
}

.file-item.selected {
  background-color: #f0f4ff;
}

.file-icon {
  font-size: 32px;
  margin-right: 12px;
}

.file-info {
  flex: 1;
  overflow: hidden;
}

.file-name {
  display: block;
  font-size: 14px;
  color: #333;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-meta {
  display: block;
  font-size: 12px;
  color: #999;
}

.check-icon {
  font-size: 18px;
  color: #667eea;
  font-weight: bold;
  margin-left: 12px;
}

.top-bar {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 15px 20px;
  font-size: 18px;
  font-weight: bold;
}

.form-section {
  padding: 20px;
}

.form-group {
  background: white;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 12px;
}

.form-label {
  font-size: 14px;
  font-weight: bold;
  color: #333;
  margin-bottom: 8px;
  display: block;
}

.form-label-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
  margin-bottom: 8px;
}

.form-check {
  font-size: 12px;
  color: #666;
  display: flex;
  align-items: center;
  gap: 4px;
}

.select-file {
  height: 44px;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  padding: 0 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #f9f9f9;
  cursor: pointer;
}

.arrow {
  color: #999;
}

.radio-group {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.radio-btn {
  flex: 1;
  min-width: 80px;
  padding: 10px 12px;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  background-color: white;
  font-size: 12px;
  color: #666;
  cursor: pointer;
  transition: all 0.2s;
}

.radio-btn.active {
  border-color: #667eea;
  background-color: #667eea;
  color: white;
}

.form-input-group {
  margin-top: 8px;
}

.form-input {
  width: 100%;
  height: 40px;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  padding: 0 12px;
  font-size: 14px;
  box-sizing: border-box;
}

.form-input:focus {
  outline: none;
  border-color: #667eea;
}

.generate-btn {
  width: 100%;
  height: 48px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 16px;
  font-weight: bold;
  cursor: pointer;
  margin-top: 20px;
}

.generate-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.result-section {
  padding: 20px;
}

.result-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.result-title {
  font-size: 16px;
  font-weight: bold;
  color: #333;
  margin-bottom: 16px;
  padding-left: 10px;
  border-left: 4px solid #667eea;
}

.link-box {
  margin-bottom: 16px;
}

.link-label {
  display: block;
  font-size: 12px;
  font-weight: bold;
  color: #666;
  margin-bottom: 8px;
}

.link-content {
  display: flex;
  align-items: center;
  background-color: #f5f5f5;
  border-radius: 6px;
  padding: 12px;
  gap: 8px;
}

.link-text {
  flex: 1;
  font-size: 12px;
  color: #333;
  word-break: break-all;
  font-family: monospace;
}

.copy-btn {
  padding: 6px 12px;
  background-color: #667eea;
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;
  white-space: nowrap;
}

.share-info {
  margin: 20px 0;
  padding: 16px;
  background-color: #f5f7fa;
  border-radius: 6px;
}

.info-item {
  display: flex;
  justify-content: space-between;
  margin: 8px 0;
  font-size: 14px;
}

.info-label {
  color: #666;
}

.info-value {
  color: #333;
  font-weight: 500;
}

.action-buttons {
  display: flex;
  gap: 12px;
  margin-top: 20px;
}

.btn-primary,
.btn-secondary {
  flex: 1;
  height: 44px;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: bold;
  cursor: pointer;
}

.btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.btn-secondary {
  background-color: #e9ecef;
  color: #333;
}
</style>
