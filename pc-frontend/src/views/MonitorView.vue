<template>
  <div>
    <div class="page-head">
      <h1 class="page-title">系统监控</h1>
      <p class="page-desc">服务器资源实时状态（每 15 秒自动刷新，仅管理员可见）</p>
    </div>

    <div class="metrics">
      <div class="card metric"><div class="metric-label">CPU 使用率</div><div class="metric-value">{{ fmt(data.cpuUsage) }}%</div></div>
      <div class="card metric"><div class="metric-label">内存使用率</div><div class="metric-value">{{ fmt(data.memoryUsagePercent) }}%</div></div>
      <div class="card metric"><div class="metric-label">磁盘使用率</div><div class="metric-value">{{ fmt(data.diskUsagePercent) }}%</div></div>
      <div class="card metric"><div class="metric-label">运行时间</div><div class="metric-value" style="font-size:20px">{{ formatUptime(data.uptime) }}</div></div>
    </div>

    <div style="display:grid;grid-template-columns:repeat(auto-fit,minmax(280px,1fr));gap:16px">
      <div class="card" style="padding:18px">
        <h3 style="margin:0 0 14px;font-size:15px">内存</h3>
        <div class="info-row"><span>已用</span><strong>{{ data.memoryUsed }} MB</strong></div>
        <div class="info-row"><span>总计</span><strong>{{ data.memoryTotal }} MB</strong></div>
        <div class="bar"><div class="bar-fill" :style="{ width: Math.min(data.memoryUsagePercent || 0, 100) + '%' }"></div></div>
      </div>
      <div class="card" style="padding:18px">
        <h3 style="margin:0 0 14px;font-size:15px">磁盘</h3>
        <div class="info-row"><span>已用</span><strong>{{ data.diskUsed }} GB</strong></div>
        <div class="info-row"><span>可用</span><strong>{{ data.diskFree }} GB</strong></div>
        <div class="info-row"><span>总计</span><strong>{{ data.diskTotal }} GB</strong></div>
        <div class="bar"><div class="bar-fill" :style="{ width: Math.min(data.diskUsagePercent || 0, 100) + '%' }"></div></div>
      </div>
      <div class="card" style="padding:18px">
        <h3 style="margin:0 0 14px;font-size:15px">系统负载</h3>
        <div class="info-row"><span>1 分钟</span><strong>{{ fmt(data.loadAverage1m) }}</strong></div>
        <div class="info-row"><span>5 分钟</span><strong>{{ fmt(data.loadAverage5m) }}</strong></div>
        <div class="info-row"><span>15 分钟</span><strong>{{ fmt(data.loadAverage15m) }}</strong></div>
      </div>
      <div class="card" style="padding:18px">
        <h3 style="margin:0 0 14px;font-size:15px">采样时间</h3>
        <div style="font-size:13px;color:var(--text-sub)">{{ formatTime(data.timestamp) }}</div>
        <button class="btn btn-sm" style="margin-top:14px" @click="fetchData" :disabled="loading">立即刷新</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import dayjs from 'dayjs'
import { getSystemStatus, type SystemMonitor } from '@/api/index'

const data = ref<SystemMonitor>({
  cpuUsage: 0, memoryUsed: 0, memoryTotal: 0, memoryUsagePercent: 0,
  diskTotal: 0, diskUsed: 0, diskFree: 0, diskUsagePercent: 0,
  loadAverage1m: 0, loadAverage5m: 0, loadAverage15m: 0, uptime: 0, timestamp: 0
})
const loading = ref(false)
let inFlight = false
let timer: ReturnType<typeof setInterval> | null = null

async function fetchData() {
  if (inFlight) return
  inFlight = true
  loading.value = true
  try {
    const res = await getSystemStatus()
    data.value = res.data
  } catch { /* ignore */ }
  finally {
    loading.value = false
    inFlight = false
  }
}

onMounted(() => {
  fetchData()
  timer = setInterval(fetchData, 15000)
})
onUnmounted(() => { if (timer) clearInterval(timer) })

function fmt(v: number | undefined) { return Number(v || 0).toFixed(1) }
function formatTime(ts: number) { return ts ? dayjs(ts).format('YYYY-MM-DD HH:mm:ss') : '--' }
function formatUptime(seconds: number) {
  if (!seconds) return '--'
  const d = Math.floor(seconds / 86400)
  const h = Math.floor((seconds % 86400) / 3600)
  return `${d}天 ${h}小时`
}
</script>

<style scoped>
.info-row { display: flex; justify-content: space-between; padding: 6px 0; font-size: 13px; color: var(--text-sub); }
.info-row strong { color: var(--text); font-weight: 500; }
.bar { height: 6px; background: var(--border); border-radius: 3px; margin-top: 12px; overflow: hidden; }
.bar-fill { height: 100%; background: var(--primary); }
</style>
