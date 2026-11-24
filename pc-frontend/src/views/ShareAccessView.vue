<template>
  <div class="login-wrap">
    <div class="card login-card" style="max-width:480px">
      <div v-if="loading" class="empty">链接校验中...</div>
      <template v-else-if="error">
        <div class="empty">
          <span class="empty-icon">⚠️</span>
          <div style="font-size:16px;color:var(--text);margin-bottom:8px">链接不可用</div>
          <div>{{ error }}</div>
        </div>
      </template>
      <template v-else-if="share">
        <div class="login-logo">F</div>
        <h1 class="login-title">{{ share.fileName }}</h1>
        <div class="info-row"><span>文件大小</span><strong>{{ formatFileSize(share.fileSize) }}</strong></div>
        <div class="info-row"><span>过期时间</span><strong>{{ share.expireTime ? formatTime(share.expireTime) : '永不过期' }}</strong></div>
        <div class="info-row"><span>下载限制</span><strong>{{ share.maxDownloads ? share.maxDownloads + ' 次' : '不限' }}</strong></div>
        <div class="info-row"><span>已下载</span><strong>{{ share.currentDownloads || 0 }} 次</strong></div>
        <button class="btn btn-primary" style="width:100%;margin-top:20px" @click="download" :disabled="downloading">
          {{ downloading ? '下载中...' : '下载文件' }}
        </button>
        <div class="login-tip">
          这是一个公开分享链接，无需登录即可下载。<br />
          <RouterLink to="/login">前往文件中心</RouterLink>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import dayjs from 'dayjs'
import { getShareInfo, shareDownloadUrl, formatFileSize, type FileShareResponse } from '@/api/index'

const route = useRoute()
const share = ref<FileShareResponse | null>(null)
const loading = ref(true)
const downloading = ref(false)
const error = ref('')
const token = String(route.query.token || '')

onMounted(async () => {
  if (!token) {
    error.value = '链接缺少令牌参数'
    loading.value = false
    return
  }
  try {
    const res = await getShareInfo(token)
    share.value = res.data
  } catch (e: any) {
    error.value = e?.message || '链接已失效'
  } finally {
    loading.value = false
  }
})

function download() {
  if (!token) return
  downloading.value = true
  const anchor = document.createElement('a')
  anchor.href = shareDownloadUrl(token)
  anchor.download = share.value?.fileName || 'download'
  document.body.appendChild(anchor)
  anchor.click()
  anchor.remove()
  setTimeout(() => { downloading.value = false }, 1000)
}

function formatTime(value: string) { return dayjs(value).format('YYYY-MM-DD HH:mm') }
</script>

<style scoped>
.info-row { display: flex; justify-content: space-between; padding: 8px 0; font-size: 13px; border-bottom: 1px solid var(--border); }
.info-row strong { font-weight: 500; }
</style>
