<template>
  <view class="access-container">
    <view v-if="loading" class="state-card">
      <text>链接校验中...</text>
    </view>

    <view v-else-if="errorMsg" class="state-card">
      <text class="state-icon">⚠️</text>
      <text class="state-title">链接不可用</text>
      <text class="state-desc">{{ errorMsg }}</text>
    </view>

    <view v-else class="access-card">
      <view class="file-head">
        <text class="file-icon">📄</text>
        <view class="file-meta">
          <text class="file-name">{{ shareInfo.fileName }}</text>
          <text class="file-size">{{ formatFileSize(shareInfo.fileSize) }}</text>
        </view>
      </view>

      <view class="info-list">
        <view class="info-item">
          <text>过期时间</text>
          <text>{{ shareInfo.expireTime ? formatTime(shareInfo.expireTime) : '永不过期' }}</text>
        </view>
        <view class="info-item">
          <text>下载限制</text>
          <text>{{ shareInfo.maxDownloads ? `${shareInfo.maxDownloads} 次` : '无限制' }}</text>
        </view>
        <view class="info-item">
          <text>已下载</text>
          <text>{{ shareInfo.currentDownloads || 0 }} 次</text>
        </view>
      </view>

      <button class="download-btn" :disabled="downloading" @click="handleDownload">
        {{ downloading ? '下载中...' : '下载文件' }}
      </button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getShareInfo, downloadShareFile } from '@/api/index'
import type { FileShareResponse } from '@/api/index'
import dayjs from 'dayjs'

const shareInfo = ref<FileShareResponse | null>(null)
const loading = ref(true)
const downloading = ref(false)
const errorMsg = ref('')
let token = ''

/** 公开访问页：从 URL query 中读取 token，无需登录 */
onLoad(options => {
  token = String(options?.token || '')
  if (!token) {
    loading.value = false
    errorMsg.value = '链接缺少令牌参数'
    return
  }
  loadShareInfo()
})

const loadShareInfo = async () => {
  try {
    const response = await getShareInfo(token)
    if (response.code === 0) {
      shareInfo.value = response.data
    } else {
      errorMsg.value = response.message || '链接已失效'
    }
  } catch (error: any) {
    errorMsg.value = error?.message || '链接已失效'
  } finally {
    loading.value = false
  }
}

const handleDownload = async () => {
  downloading.value = true
  try {
    const tempPath = await downloadShareFile(token)
    // #ifdef APP-PLUS
    plus.runtime.openFile(tempPath)
    // #endif
    // #ifndef APP-PLUS
    uni.showToast({ title: '已开始下载', icon: 'success' })
    // #endif
  } catch (error: any) {
    uni.showToast({ title: error?.message || '下载失败', icon: 'none' })
  } finally {
    downloading.value = false
  }
}

const formatFileSize = (bytes: number) => {
  if (!bytes) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  const i = Math.min(Math.floor(Math.log(bytes) / Math.log(1024)), units.length - 1)
  return (bytes / Math.pow(1024, i)).toFixed(2) + ' ' + units[i]
}

const formatTime = (value: string) => dayjs(value).format('YYYY-MM-DD HH:mm:ss')
</script>

<style scoped>
.access-container {
  min-height: 100vh;
  background: #f5f7fa;
  padding: 40px 20px;
}
.state-card,
.access-card {
  max-width: 480px;
  margin: 0 auto;
  background: #fff;
  border-radius: 14px;
  padding: 28px 22px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
}
.state-card {
  text-align: center;
  color: #666;
}
.state-icon {
  display: block;
  font-size: 44px;
  margin-bottom: 12px;
}
.state-title {
  display: block;
  font-size: 17px;
  font-weight: bold;
  color: #333;
  margin-bottom: 8px;
}
.state-desc {
  font-size: 13px;
}
.file-head {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
}
.file-icon {
  font-size: 40px;
  margin-right: 14px;
}
.file-meta {
  flex: 1;
  overflow: hidden;
}
.file-name {
  display: block;
  font-size: 16px;
  font-weight: bold;
  color: #333;
  word-break: break-all;
}
.file-size {
  display: block;
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}
.info-list {
  background: #f8f9fb;
  border-radius: 8px;
  padding: 12px 16px;
  margin-bottom: 24px;
}
.info-item {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: #666;
  padding: 6px 0;
}
.download-btn {
  width: 100%;
  height: 46px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: bold;
}
.download-btn:disabled {
  opacity: 0.6;
}
</style>
