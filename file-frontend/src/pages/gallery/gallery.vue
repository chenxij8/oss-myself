<template>
  <view class="gallery-container">
    <view class="top-bar">
      <text class="title">图床</text>
      <text class="subtitle">图片/视频独立存储 · 在线预览</text>
    </view>

    <view class="stats-row">
      <view class="stat-box"><text class="stat-num">{{ stats.total }}</text><text class="stat-label">全部</text></view>
      <view class="stat-box"><text class="stat-num">{{ stats.imageCount }}</text><text class="stat-label">图片</text></view>
      <view class="stat-box"><text class="stat-num">{{ stats.videoCount }}</text><text class="stat-label">视频</text></view>
      <view class="stat-box"><text class="stat-num">{{ formatSize(stats.totalSize) }}</text><text class="stat-label">占用</text></view>
    </view>

    <view class="toolbar">
      <button class="upload-btn" :disabled="uploading" @click="chooseMedia">
        {{ uploading ? `上传中 ${current}/${queue.length}` : '+ 上传图片/视频' }}
      </button>
      <button class="filter-btn" :class="{ on: filter === 'ALL' }" @click="setFilter('ALL')">全部</button>
      <button class="filter-btn" :class="{ on: filter === 'IMAGE' }" @click="setFilter('IMAGE')">图片</button>
      <button class="filter-btn" :class="{ on: filter === 'VIDEO' }" @click="setFilter('VIDEO')">视频</button>
    </view>
    <view v-if="uploading" class="progress-track"><view class="progress-fill" :style="{ width: progress + '%' }"></view></view>

    <view class="grid">
      <view v-for="asset in assets" :key="asset.id" class="card" @click="preview(asset)">
        <image v-if="asset.mediaType === 'IMAGE'" class="thumb" :src="galleryPreviewUrl(asset.id)" mode="aspectFill" />
        <view v-else class="video-thumb">
          <text class="play-icon">▶</text>
          <text class="video-label">视频</text>
        </view>
        <view class="meta">
          <text class="name">{{ asset.fileName }}</text>
          <view class="meta-row">
            <text class="size">{{ formatSize(asset.fileSize) }}</text>
            <text class="del" @click.stop="remove(asset)">删除</text>
          </view>
        </view>
      </view>
      <view v-if="!assets.length && !loading" class="empty">
        <text class="empty-icon">🖼</text>
        <text>暂无素材，点击上方按钮上传</text>
      </view>
    </view>

    <view class="pager">
      <button size="mini" :disabled="page <= 1 || loading" @click="changePage(-1)">上一页</button>
      <text>第 {{ page }} 页</text>
      <button size="mini" :disabled="!hasMore || loading" @click="changePage(1)">下一页</button>
    </view>

    <view v-if="previewAsset" class="modal-mask" @click.self="previewAsset = null">
      <view class="modal">
        <view class="modal-head">
          <text class="modal-title">{{ previewAsset.fileName }}</text>
          <text class="modal-close" @click="previewAsset = null">✕</text>
        </view>
        <image v-if="previewAsset.mediaType === 'IMAGE'" class="preview-img" :src="galleryPreviewUrl(previewAsset.id)" mode="widthFix" />
        <video v-else class="preview-video" :src="galleryPreviewUrl(previewAsset.id)" controls autoplay />
        <view class="link-box">
          <text class="link-text">{{ galleryPreviewUrl(previewAsset.id) }}</text>
          <button size="mini" @click="copyLink(galleryPreviewUrl(previewAsset.id))">复制外链</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import {
  getGalleryList, getGalleryStats, uploadGalleryFile, deleteGalleryAsset,
  galleryPreviewUrl, formatFileSize, type GalleryAsset, type GalleryStats
} from '@/api/index'
import { pickFiles, type PickedFile } from '@/utils/pickFiles'

const PAGE_SIZE = 20
const assets = ref<GalleryAsset[]>([])
const stats = reactive<GalleryStats>({ total: 0, imageCount: 0, videoCount: 0, totalSize: 0 })
const loading = ref(false)
const uploading = ref(false)
const progress = ref(0)
const current = ref(0)
const queue = ref<PickedFile[]>([])
const filter = ref<'ALL' | 'IMAGE' | 'VIDEO'>('ALL')
const page = ref(1)
const hasMore = ref(true)
const previewAsset = ref<GalleryAsset | null>(null)

onMounted(() => { loadAll(); refreshStats() })

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

function setFilter(value: 'ALL' | 'IMAGE' | 'VIDEO') { filter.value = value; loadAll(true) }
function changePage(delta: number) {
  const next = page.value + delta
  if (next < 1) return
  page.value = next
  loadAll(false)
}

async function refreshStats() {
  try { Object.assign(stats, (await getGalleryStats()).data) } catch { /* ignore */ }
}

async function chooseMedia() {
  try {
    // H5 无 uni.chooseMedia，统一走跨端选择器（H5 用 input，App/小程序用 chooseMedia）
    const picked = await pickFiles({
      count: 9,
      accept: 'image/*,video/*',
      nativeApi: 'media',
      mediaType: ['image', 'video']
    })
    if (!picked.length) return
    queue.value = picked
    uploading.value = true
    let success = 0
    for (const item of picked) {
      current.value++
      progress.value = 0
      try {
        await uploadGalleryFile(item, p => { progress.value = p })
        success++
      } catch (e: any) {
        uni.showToast({ title: e?.message || '上传失败', icon: 'none' })
      }
    }
    uploading.value = false
    if (success > 0) { await loadAll(true); await refreshStats() }
  } catch (e: any) {
    uploading.value = false
    uni.showToast({ title: e?.message || '选择媒体失败', icon: 'none' })
  }
}

function preview(asset: GalleryAsset) { previewAsset.value = asset }

async function remove(asset: GalleryAsset) {
  const confirmRes = await new Promise<boolean>(resolve => {
    uni.showModal({
      title: '确认删除',
      content: `确定删除「${asset.fileName}」吗？`,
      success: r => resolve(r.confirm)
    })
  })
  if (!confirmRes) return
  try {
    await deleteGalleryAsset(asset.id)
    assets.value = assets.value.filter(a => a.id !== asset.id)
    await refreshStats()
  } catch (e: any) { uni.showToast({ title: e?.message || '删除失败', icon: 'none' }) }
}

function copyLink(url: string) {
  uni.setClipboardData({ data: url, success: () => uni.showToast({ title: '外链已复制', icon: 'success' }) })
}

function formatSize(bytes: number) { return formatFileSize(bytes) }
</script>

<style scoped>
.gallery-container { min-height: 100vh; background: #f5f7fa; padding-bottom: 30px; }
.top-bar { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: #fff; padding: 16px 20px; }
.title { font-size: 18px; font-weight: bold; display: block; }
.subtitle { font-size: 11px; opacity: .8; display: block; margin-top: 4px; }
.stats-row { display: flex; gap: 8px; padding: 14px 16px 4px; }
.stat-box { flex: 1; background: #fff; border-radius: 10px; padding: 10px 4px; text-align: center; }
.stat-num { display: block; font-size: 16px; font-weight: bold; color: #333; }
.stat-label { display: block; font-size: 10px; color: #999; margin-top: 2px; }
.toolbar { display: flex; flex-wrap: wrap; gap: 8px; padding: 12px 16px; align-items: center; }
.upload-btn { background: #667eea; color: #fff; border-radius: 8px; font-size: 12px; padding: 0 14px; height: 34px; line-height: 34px; margin: 0; }
.filter-btn { background: #fff; color: #666; border: 1px solid #e5e7eb; border-radius: 8px; font-size: 12px; padding: 0 12px; height: 34px; line-height: 34px; margin: 0; }
.filter-btn.on { background: #eef2ff; color: #667eea; border-color: #667eea; }
.progress-track { height: 5px; background: #e5e7eb; margin: 0 16px; border-radius: 3px; overflow: hidden; }
.progress-fill { height: 100%; background: #667eea; }
.grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 10px; padding: 12px 16px; }
.card { background: #fff; border-radius: 10px; overflow: hidden; }
.thumb, .video-thumb { width: 100%; height: 110px; display: block; }
.video-thumb { background: #1f2937; display: flex; align-items: center; justify-content: center; position: relative; }
.play-icon { color: #fff; font-size: 28px; }
.video-label { position: absolute; bottom: 6px; right: 8px; color: #fff; font-size: 10px; background: rgba(0,0,0,.5); padding: 2px 6px; border-radius: 4px; }
.meta { padding: 8px; }
.name { font-size: 12px; color: #333; display: block; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.meta-row { display: flex; justify-content: space-between; margin-top: 4px; }
.size { font-size: 10px; color: #999; }
.del { font-size: 10px; color: #ef4444; }
.empty { grid-column: 1 / -1; text-align: center; padding: 50px 0; color: #999; font-size: 12px; }
.empty-icon { display: block; font-size: 36px; margin-bottom: 10px; }
.pager { display: flex; justify-content: center; align-items: center; gap: 14px; font-size: 12px; color: #666; }
.modal-mask { position: fixed; inset: 0; background: rgba(0,0,0,.8); z-index: 999; display: flex; align-items: center; justify-content: center; padding: 20px; }
.modal { width: 100%; max-width: 600px; background: #fff; border-radius: 12px; padding: 14px; max-height: 90vh; }
.modal-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.modal-title { font-size: 14px; font-weight: bold; }
.modal-close { font-size: 18px; color: #999; padding: 0 8px; }
.preview-img, .preview-video { width: 100%; max-height: 60vh; }
.link-box { display: flex; align-items: center; gap: 8px; margin-top: 10px; padding: 8px; background: #f5f7fa; border-radius: 6px; }
.link-text { flex: 1; font-size: 10px; color: #666; word-break: break-all; }
</style>
