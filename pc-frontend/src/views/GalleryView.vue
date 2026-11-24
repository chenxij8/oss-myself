<template>
  <div>
    <div class="page-head">
      <h1 class="page-title">图床</h1>
      <p class="page-desc">图片与视频独立存储，支持在线预览与外链引用（与文件托管分离）</p>
    </div>

    <div class="metrics">
      <div class="card metric"><div class="metric-label">全部素材</div><div class="metric-value">{{ stats.total }}</div></div>
      <div class="card metric"><div class="metric-label">图片</div><div class="metric-value">{{ stats.imageCount }}</div></div>
      <div class="card metric"><div class="metric-label">视频</div><div class="metric-value">{{ stats.videoCount }}</div></div>
      <div class="card metric"><div class="metric-label">占用空间</div><div class="metric-value" style="font-size:22px">{{ formatFileSize(stats.totalSize) }}</div></div>
    </div>

    <div class="card" style="padding:18px">
      <div class="toolbar">
        <input ref="fileInput" type="file" accept="image/*,video/*" multiple style="display:none" @change="onFilesSelected" />
        <button class="btn btn-primary" @click="pickFiles" :disabled="uploading">
          {{ uploading ? `上传中 ${current}/${queue.length}` : '+ 上传图片/视频' }}
        </button>
        <button class="btn" @click="loadAll()" :disabled="loading">刷新</button>
        <div class="spacer"></div>
        <div class="filter-group">
          <button class="btn btn-sm" :class="{ 'btn-primary': filter === 'ALL' }" @click="setFilter('ALL')">全部</button>
          <button class="btn btn-sm" :class="{ 'btn-primary': filter === 'IMAGE' }" @click="setFilter('IMAGE')">图片</button>
          <button class="btn btn-sm" :class="{ 'btn-primary': filter === 'VIDEO' }" @click="setFilter('VIDEO')">视频</button>
        </div>
      </div>
      <div v-if="uploading" class="progress-track"><div class="progress-fill" :style="{ width: progress + '%' }"></div></div>
      <div v-if="uploadError" class="error-text" style="margin-top:8px">{{ uploadError }}</div>

      <div class="gallery-grid" style="margin-top:16px">
        <div v-for="asset in assets" :key="asset.id" class="card gallery-card" @click="preview(asset)">
          <img v-if="asset.mediaType === 'IMAGE'" class="gallery-thumb" :src="galleryPreviewUrl(asset.id)" :alt="asset.fileName" loading="lazy" />
          <video v-else class="gallery-video" :src="galleryPreviewUrl(asset.id)" muted preload="metadata" playsinline></video>
          <div class="gallery-meta">
            <div class="gallery-name" :title="asset.fileName">{{ asset.fileName }}</div>
            <div class="gallery-sub">
              <span>{{ formatFileSize(asset.fileSize) }}</span>
              <span>{{ formatTime(asset.createTime) }}</span>
            </div>
            <div class="cell-actions" style="margin-top:8px" @click.stop>
              <button class="btn btn-sm btn-text" @click="copy(galleryPreviewUrl(asset.id))">复制外链</button>
              <button class="btn btn-sm btn-text btn-danger" @click="remove(asset)">删除</button>
            </div>
          </div>
        </div>
        <div v-if="!assets.length && !loading" class="empty" style="grid-column:1/-1">
          <span class="empty-icon">🖼</span>暂无素材，上传图片或视频开始使用图床
        </div>
      </div>

      <div class="pagination">
        <button class="btn btn-sm" :disabled="page <= 1 || loading" @click="changePage(-1)">上一页</button>
        <span>第 {{ page }} 页</span>
        <button class="btn btn-sm" :disabled="!hasMore || loading" @click="changePage(1)">下一页</button>
      </div>
    </div>

    <div v-if="previewAsset" class="modal-mask" @click.self="previewAsset = null">
      <div class="modal" style="max-width:900px">
        <div class="modal-head">
          <span class="modal-title">{{ previewAsset.fileName }}</span>
          <button class="btn btn-sm" @click="previewAsset = null">关闭</button>
        </div>
        <div class="modal-body">
          <div class="preview-wrap">
            <img v-if="previewAsset.mediaType === 'IMAGE'" class="preview-img" :src="galleryPreviewUrl(previewAsset.id)" />
            <video v-else class="preview-video" :src="galleryPreviewUrl(previewAsset.id)" controls autoplay></video>
          </div>
          <div style="margin-top:14px;font-size:12px;color:var(--text-sub);word-break:break-all">
            外链：<span class="tag">{{ galleryPreviewUrl(previewAsset.id) }}</span>
            <button class="btn btn-sm btn-text" @click="copy(galleryPreviewUrl(previewAsset.id))">复制</button>
          </div>
          <div v-if="previewAsset.width && previewAsset.height" style="margin-top:6px;font-size:12px;color:var(--text-faint)">
            尺寸 {{ previewAsset.width }} × {{ previewAsset.height }}
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import dayjs from 'dayjs'
import {
  getGalleryList, getGalleryStats, uploadGalleryFile, deleteGalleryAsset,
  galleryPreviewUrl, formatFileSize, type GalleryAsset, type GalleryStats
} from '@/api/index'

const PAGE_SIZE = 24
const assets = ref<GalleryAsset[]>([])
const stats = reactive<GalleryStats>({ total: 0, imageCount: 0, videoCount: 0, totalSize: 0 })
const loading = ref(false)
const uploading = ref(false)
const progress = ref(0)
const current = ref(0)
const queue = ref<File[]>([])
const uploadError = ref('')
const filter = ref<'ALL' | 'IMAGE' | 'VIDEO'>('ALL')
const page = ref(1)
const hasMore = ref(true)
const fileInput = ref<HTMLInputElement | null>(null)
const previewAsset = ref<GalleryAsset | null>(null)

onMounted(() => loadAll())

async function loadAll(reset = true) {
  if (loading.value) return
  if (reset) { page.value = 1; hasMore.value = true }
  loading.value = true
  try {
    const mediaType = filter.value === 'ALL' ? undefined : filter.value
    const res = await getGalleryList(page.value, PAGE_SIZE, mediaType)
    const list = res.data || []
    assets.value = reset ? list : [...assets.value, ...list]
    hasMore.value = list.length === PAGE_SIZE
    if (hasMore.value) page.value++
  } catch { /* 拦截器统一提示 */ }
  finally { loading.value = false }
}

function setFilter(value: 'ALL' | 'IMAGE' | 'VIDEO') {
  filter.value = value
  loadAll(true)
}

function changePage(delta: number) {
  const next = page.value + delta
  if (next < 1) return
  page.value = next
  loadAll(false)
}

async function refreshStats() {
  try {
    const res = await getGalleryStats()
    Object.assign(stats, res.data)
  } catch { /* ignore */ }
}

function pickFiles() { fileInput.value?.click() }

async function onFilesSelected(event: Event) {
  const input = event.target as HTMLInputElement
  const selected = Array.from(input.files || [])
  if (!selected.length) return
  queue.value = selected
  uploadError.value = ''
  uploading.value = true
  current.value = 0
  let success = 0
  for (const file of selected) {
    current.value++
    progress.value = 0
    try {
      await uploadGalleryFile(file, p => { progress.value = p })
      success++
    } catch (e: any) {
      uploadError.value = `${file.name}: ${e?.message || '上传失败'}`
    }
  }
  uploading.value = false
  input.value = ''
  if (success > 0) {
    await loadAll(true)
    await refreshStats()
  }
}

function preview(asset: GalleryAsset) { previewAsset.value = asset }

async function remove(asset: GalleryAsset) {
  if (!confirm(`确定删除「${asset.fileName}」吗？`)) return
  try {
    await deleteGalleryAsset(asset.id)
    assets.value = assets.value.filter(a => a.id !== asset.id)
    await refreshStats()
  } catch (e: any) { alert(e?.message || '删除失败') }
}

function copy(text: string) {
  navigator.clipboard?.writeText(text).then(() => alert('外链已复制到剪贴板'))
}

function formatTime(value: string) {
  return value ? dayjs(value).format('MM-DD HH:mm') : ''
}
</script>

<style scoped>
.filter-group { display: flex; gap: 4px; }
</style>
