<template>
  <div>
    <div class="page-head">
      <h1 class="page-title">我的分享</h1>
      <p class="page-desc">查看和管理你创建的分享链接</p>
    </div>
    <div class="card" style="padding:18px">
      <div class="toolbar">
        <button class="btn" @click="load" :disabled="loading">刷新</button>
        <div class="spacer"></div>
        <span class="tag-gray tag">共 {{ shares.length }} 条记录</span>
      </div>
      <table class="data-table">
        <thead>
          <tr>
            <th style="width:30%">文件名</th>
            <th>有效期</th>
            <th>下载次数</th>
            <th>创建时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="share in shares" :key="share.id">
            <td>{{ share.fileName }}</td>
            <td>{{ share.expireTime ? formatTime(share.expireTime) : '永不过期' }}</td>
            <td>
              {{ share.currentDownloads ?? 0 }}
              <span v-if="share.maxDownloads"> / {{ share.maxDownloads }}</span>
              <span v-else class="tag-gray tag" style="margin-left:4px">不限</span>
            </td>
            <td>{{ formatTime(share.createTime) }}</td>
            <td>
              <div class="cell-actions">
                <button class="btn btn-sm btn-text" @click="copy(sharePageUrl(share.shareToken))">复制链接</button>
                <button class="btn btn-sm btn-text" @click="openAccess(share.shareToken)">访问</button>
                <button class="btn btn-sm btn-text btn-danger" @click="remove(share)">删除</button>
              </div>
            </td>
          </tr>
          <tr v-if="!shares.length && !loading">
            <td colspan="5"><div class="empty"><span class="empty-icon">⌁</span>暂无分享，可在文件管理中对文件创建分享</div></td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import dayjs from 'dayjs'
import { getShareList, deleteShare, sharePageUrl, type FileShareResponse } from '@/api/index'

const shares = ref<FileShareResponse[]>([])
const loading = ref(false)

onMounted(() => load())

async function load() {
  loading.value = true
  try {
    const res = await getShareList(1, 200)
    shares.value = res.data || []
  } catch { /* ignore */ }
  finally { loading.value = false }
}

async function remove(share: FileShareResponse) {
  if (!confirm(`确定删除「${share.fileName}」的分享链接吗？`)) return
  try {
    await deleteShare(share.id)
    shares.value = shares.value.filter(s => s.id !== share.id)
  } catch (e: any) { alert(e?.message || '删除失败') }
}

function copy(text: string) {
  navigator.clipboard?.writeText(text).then(() => alert('分享链接已复制'))
}

function openAccess(token: string) {
  window.open(sharePageUrl(token), '_blank')
}

function formatTime(value: string) {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '--'
}
</script>
