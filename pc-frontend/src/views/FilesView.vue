<template>
  <div>
    <div class="page-head">
      <h1 class="page-title">文件管理</h1>
      <p class="page-desc">管理你上传的所有文件，支持秒传、在线下载与分享</p>
    </div>

    <div class="metrics">
      <div class="card metric"><div class="metric-label">当前页文件</div><div class="metric-value">{{ files.length }}</div></div>
      <div class="card metric"><div class="metric-label">已加载记录</div><div class="metric-value">{{ totalLoaded }}</div></div>
      <div class="card metric"><div class="metric-label">本页占用</div><div class="metric-value">{{ formatFileSize(pageSizeTotal) }}</div></div>
      <div class="card metric"><div class="metric-label">支持单文件</div><div class="metric-value" style="font-size:20px">2 GB</div></div>
    </div>

    <div class="card" style="padding:18px">
      <div class="toolbar">
        <input ref="fileInput" type="file" style="display:none" @change="onFileSelected" />
        <button class="btn btn-primary" @click="pickFile" :disabled="uploading">
          {{ uploading ? '上传中...' : '+ 上传文件' }}
        </button>
        <button class="btn" @click="loadFiles(true)" :disabled="loading">刷新</button>
        <span class="tag" v-if="uploading">上传进度 {{ progress }}%</span>
        <div class="spacer"></div>
        <input v-model="keyword" class="input search" placeholder="筛选文件名" />
      </div>
      <div v-if="uploading" class="progress-track"><div class="progress-fill" :style="{ width: progress + '%' }"></div></div>

      <table class="data-table">
        <thead>
          <tr>
            <th style="width:38%">文件名</th>
            <th style="width:12%">大小</th>
            <th style="width:18%">上传时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="file in filteredFiles" :key="file.id">
            <td>
              <span class="file-ico">{{ iconFor(file.fileName) }}</span>
              {{ file.fileName }}
            </td>
            <td>{{ formatFileSize(file.fileSize) }}</td>
            <td>{{ formatTime(file.uploadTime) }}</td>
            <td>
              <div class="cell-actions">
                <button class="btn btn-sm btn-text" @click="handleDownload(file)">下载</button>
                <button class="btn btn-sm btn-text" @click="openShare(file)">分享</button>
                <button class="btn btn-sm btn-text btn-danger" @click="handleDelete(file)">删除</button>
              </div>
            </td>
          </tr>
          <tr v-if="!filteredFiles.length && !loading">
            <td colspan="4"><div class="empty"><span class="empty-icon">📭</span>暂无文件，点击「上传文件」开始</div></td>
          </tr>
        </tbody>
      </table>

      <div class="pagination">
        <button class="btn btn-sm" :disabled="page <= 1 || loading" @click="changePage(-1)">上一页</button>
        <span>第 {{ page }} 页</span>
        <button class="btn btn-sm" :disabled="!hasMore || loading" @click="changePage(1)">下一页</button>
      </div>
    </div>

    <div v-if="shareDialog" class="modal-mask" @click.self="shareDialog = false">
      <div class="modal" style="max-width:480px">
        <div class="modal-head">
          <span class="modal-title">创建分享链接</span>
          <button class="btn btn-sm" @click="shareDialog = false">关闭</button>
        </div>
        <div class="modal-body">
          <p style="margin-top:0;color:var(--text-sub)">文件：{{ shareTarget?.fileName }}</p>
          <div class="field">
            <label>有效期</label>
            <select v-model="shareExpire" class="select">
              <option :value="0">永不过期</option>
              <option :value="1">1 小时</option>
              <option :value="24">1 天</option>
              <option :value="168">7 天</option>
              <option :value="720">30 天</option>
            </select>
          </div>
          <div class="field">
            <label>下载次数限制（0 表示不限）</label>
            <input v-model.number="shareMax" class="input" type="number" min="0" />
          </div>
          <div v-if="shareResult" style="margin-top:16px">
            <label style="font-size:13px;color:var(--text-sub)">分享链接</label>
            <div style="display:flex;gap:8px;margin-top:6px">
              <input class="input" :value="shareLink" readonly />
              <button class="btn" @click="copy(shareLink)">复制</button>
            </div>
            <div style="font-size:12px;color:var(--text-faint);margin-top:6px">令牌：{{ shareResult.shareToken }}</div>
          </div>
          <div v-if="shareError" class="error-text">{{ shareError }}</div>
        </div>
        <div class="modal-foot">
          <button class="btn" @click="shareDialog = false">取消</button>
          <button class="btn btn-primary" :disabled="shareLoading" @click="submitShare">
            {{ shareLoading ? '生成中...' : (shareResult ? '重新生成' : '生成链接') }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import dayjs from 'dayjs'
import {
  getFileList, uploadFile, downloadFile, deleteFile, createShare,
  sharePageUrl, formatFileSize, type FileInfo, type FileShareResponse
} from '@/api/index'

const PAGE_SIZE = 20
const files = ref<FileInfo[]>([])
const loading = ref(false)
const uploading = ref(false)
const progress = ref(0)
const page = ref(1)
const hasMore = ref(true)
const keyword = ref('')
const fileInput = ref<HTMLInputElement | null>(null)

const filteredFiles = computed(() =>
  keyword.value ? files.value.filter(f => f.fileName.toLowerCase().includes(keyword.value.toLowerCase())) : files.value
)
const totalLoaded = computed(() => files.value.length)
const pageSizeTotal = computed(() => files.value.reduce((s, f) => s + (f.fileSize || 0), 0))

onMounted(() => loadFiles(true))

async function loadFiles(reset = false) {
  if (loading.value) return
  if (reset) { page.value = 1; hasMore.value = true }
  loading.value = true
  try {
    const res = await getFileList(page.value, PAGE_SIZE)
    const list = res.data || []
    files.value = reset ? list : [...files.value, ...list]
    hasMore.value = list.length === PAGE_SIZE
    if (hasMore.value) page.value++
  } catch { /* 错误由拦截器提示 */ }
  finally { loading.value = false }
}

function changePage(delta: number) {
  const next = page.value + delta
  if (next < 1) return
  page.value = next
  loadFiles(false)
}

function pickFile() { fileInput.value?.click() }

async function onFileSelected(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  uploading.value = true
  progress.value = 0
  try {
    const res = await uploadFile(file, p => { progress.value = p })
    progress.value = 100
    files.value.unshift(res.data)
  } catch (e: any) {
    alert(e?.message || '上传失败')
  } finally {
    uploading.value = false
    input.value = ''
    setTimeout(() => { progress.value = 0 }, 800)
  }
}

async function handleDownload(file: FileInfo) {
  try { await downloadFile(file.id, file.fileName) }
  catch (e: any) { alert(e?.message || '下载失败') }
}

async function handleDelete(file: FileInfo) {
  if (!confirm(`确定删除「${file.fileName}」吗？相关分享链接会同时失效。`)) return
  try {
    await deleteFile(file.id)
    files.value = files.value.filter(f => f.id !== file.id)
  } catch (e: any) { alert(e?.message || '删除失败') }
}

const shareDialog = ref(false)
const shareTarget = ref<FileInfo | null>(null)
const shareExpire = ref(24)
const shareMax = ref(0)
const shareResult = ref<FileShareResponse | null>(null)
const shareError = ref('')
const shareLoading = ref(false)
const shareLink = computed(() => shareResult.value ? sharePageUrl(shareResult.value.shareToken) : '')

function openShare(file: FileInfo) {
  shareTarget.value = file
  shareResult.value = null
  shareError.value = ''
  shareExpire.value = 24
  shareMax.value = 0
  shareDialog.value = true
}

async function submitShare() {
  if (!shareTarget.value) return
  shareLoading.value = true
  shareError.value = ''
  try {
    const expireTime = shareExpire.value > 0
      ? dayjs().add(shareExpire.value, 'hour').format('YYYY-MM-DDTHH:mm:ss')
      : undefined
    const res = await createShare(
      shareTarget.value.id,
      expireTime,
      shareMax.value > 0 ? shareMax.value : undefined
    )
    shareResult.value = res.data
  } catch (e: any) {
    shareError.value = e?.message || '生成失败'
  } finally {
    shareLoading.value = false
  }
}

function copy(text: string) {
  navigator.clipboard?.writeText(text).then(() => alert('已复制到剪贴板'))
}

function iconFor(name: string) {
  const ext = name.split('.').pop()?.toLowerCase()
  if (['png','jpg','jpeg','gif','webp','bmp'].includes(ext || '')) return '🖼'
  if (['mp4','webm','mov','mkv','avi'].includes(ext || '')) return '🎬'
  if (['pdf'].includes(ext || '')) return '📕'
  if (['zip','rar','7z'].includes(ext || '')) return '🗜'
  return '📄'
}

function formatTime(value: string) {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '--'
}
</script>

<style scoped>
.file-ico { margin-right: 6px; }
</style>
