<template>
  <view class="dashboard-container">
    <!-- 顶部导航 -->
    <view class="top-bar">
      <view class="title">服务器监控仪表板</view>
      <view class="refresh-btn" @click="refreshData">
        <text>{{ loading ? '刷新中...' : '刷新' }}</text>
      </view>
    </view>

    <!-- 监控数据卡片 -->
    <view class="content">
      <!-- CPU 监控 -->
      <view class="card">
        <view class="card-header">
          <text class="card-title">CPU 使用率</text>
          <text class="card-value" :class="{ critical: monitorData.cpuUsage > 80 }">
            {{ monitorData.cpuUsage.toFixed(2) }}%
          </text>
        </view>
        <view class="progress-bar">
          <view class="progress-fill" :style="{ width: monitorData.cpuUsage + '%' }"></view>
        </view>
      </view>

      <!-- 内存监控 -->
      <view class="card">
        <view class="card-header">
          <text class="card-title">内存使用</text>
          <text class="card-value" :class="{ critical: monitorData.memoryUsagePercent > 80 }">
            {{ monitorData.memoryUsagePercent.toFixed(2) }}%
          </text>
        </view>
        <view class="progress-bar">
          <view class="progress-fill" :style="{ width: monitorData.memoryUsagePercent + '%' }"></view>
        </view>
        <view class="card-detail">
          {{ (monitorData.memoryUsed / 1024).toFixed(2) }} GB / {{ (monitorData.memoryTotal / 1024).toFixed(2) }} GB
        </view>
      </view>

      <!-- 磁盘监控 -->
      <view class="card">
        <view class="card-header">
          <text class="card-title">磁盘使用</text>
          <text class="card-value" :class="{ critical: monitorData.diskUsagePercent > 80 }">
            {{ monitorData.diskUsagePercent.toFixed(2) }}%
          </text>
        </view>
        <view class="progress-bar">
          <view class="progress-fill" :style="{ width: monitorData.diskUsagePercent + '%' }"></view>
        </view>
        <view class="card-detail">
          {{ monitorData.diskUsed }} GB / {{ monitorData.diskTotal }} GB
        </view>
      </view>

      <!-- 系统负载 -->
      <view class="card">
        <view class="card-title">系统平均负载</view>
        <view class="load-info">
          <view class="load-item">
            <text class="load-label">1分钟</text>
            <text class="load-value">{{ monitorData.loadAverage1m.toFixed(2) }}</text>
          </view>
          <view class="load-item">
            <text class="load-label">5分钟</text>
            <text class="load-value">{{ monitorData.loadAverage5m.toFixed(2) }}</text>
          </view>
          <view class="load-item">
            <text class="load-label">15分钟</text>
            <text class="load-value">{{ monitorData.loadAverage15m.toFixed(2) }}</text>
          </view>
        </view>
      </view>

      <!-- 系统信息 -->
      <view class="card">
        <view class="card-title">系统信息</view>
        <view class="info-list">
          <view class="info-item">
            <text class="info-label">运行时间</text>
            <text class="info-value">{{ formatUptime(monitorData.uptime) }}</text>
          </view>
          <view class="info-item">
            <text class="info-label">最后更新</text>
            <text class="info-value">{{ formaTime(monitorData.timestamp) }}</text>
          </view>
        </view>
      </view>

      <!-- 快捷导航 -->
      <view class="quick-nav">
        <view class="nav-item" @click="navigateTo('/pages/file/list')">
          <text class="nav-icon">📁</text>
          <text class="nav-text">文件管理</text>
        </view>
        <view class="nav-item" @click="navigateTo('/pages/share/config')">
          <text class="nav-icon">🔗</text>
          <text class="nav-text">分享配置</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { getSystemStatus } from '@/api/index'
import { useUserStore } from '@/stores/user'
import dayjs from 'dayjs'

// 用户状态
const userStore = useUserStore()

// 监控数据
const monitorData = ref({
  cpuUsage: 0,
  memoryUsed: 0,
  memoryTotal: 0,
  memoryUsagePercent: 0,
  diskTotal: 0,
  diskUsed: 0,
  diskFree: 0,
  diskUsagePercent: 0,
  loadAverage1m: 0,
  loadAverage5m: 0,
  loadAverage15m: 0,
  uptime: 0,
  timestamp: 0
})

const loading = ref(false)
let refreshInterval: any = null

/**
 * 获取监控数据
 */
const fetchMonitorData = async () => {
  // 未登录时不请求数据
  if (!userStore.isLoggedIn()) {
    return
  }
  
  try {
    loading.value = true
    const response = await getSystemStatus()
    
    if (response.code === 0) {
      monitorData.value = response.data
    } else {
      uni.showToast({ title: '获取数据失败', icon: 'error' })
    }
  } catch (error) {
    console.error('获取监控数据异常:', error)
  } finally {
    loading.value = false
  }
}

/**
 * 刷新数据
 */
const refreshData = () => {
  fetchMonitorData()
}

/**
 * 格式化运行时间
 */
const formatUptime = (seconds: number) => {
  const days = Math.floor(seconds / 86400)
  const hours = Math.floor((seconds % 86400) / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  return `${days}天 ${hours}小时 ${minutes}分钟`
}

/**
 * 格式化时间
 */
const formaTime = (timestamp: number) => {
  return dayjs(timestamp).format('YYYY-MM-DD HH:mm:ss')
}

/**
 * 导航
 */
const navigateTo = (url: string) => {
  uni.navigateTo({ url })
}

/**
 * 组件挂载
 */
onMounted(() => {
  // 已登录时才获取数据和启动定时刷新
  if (userStore.isLoggedIn()) {
    fetchMonitorData()
    
    // 每30秒自动刷新一次
    refreshInterval = setInterval(() => {
      fetchMonitorData()
    }, 30000)
  }
})

/**
 * 组件卸载
 */
onUnmounted(() => {
  if (refreshInterval) {
    clearInterval(refreshInterval)
  }
})
</script>

<style scoped>
.dashboard-container {
  width: 100%;
  background-color: #f5f7fa;
  min-height: 100vh;
  padding-bottom: 20px;
}

.top-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 15px 20px;
  font-size: 18px;
  font-weight: bold;
}

.title {
  flex: 1;
}

.refresh-btn {
  padding: 8px 16px;
  background-color: rgba(255, 255, 255, 0.3);
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
}

.content {
  padding: 20px;
}

.card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.card-title {
  font-size: 16px;
  font-weight: bold;
  color: #333;
}

.card-value {
  font-size: 24px;
  font-weight: bold;
  color: #667eea;
}

.card-value.critical {
  color: #ff6b6b;
}

.progress-bar {
  width: 100%;
  height: 8px;
  background-color: #e9ecef;
  border-radius: 4px;
  overflow: hidden;
  margin-bottom: 8px;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
  transition: width 0.3s ease;
}

.card-detail {
  font-size: 12px;
  color: #999;
  margin-top: 8px;
}

.load-info {
  display: flex;
  justify-content: space-around;
  margin-top: 16px;
}

.load-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.load-label {
  font-size: 12px;
  color: #999;
  margin-bottom: 4px;
}

.load-value {
  font-size: 18px;
  font-weight: bold;
  color: #333;
}

.info-list {
  margin-top: 12px;
}

.info-item {
  display: flex;
  justify-content: space-between;
  margin: 8px 0;
  font-size: 14px;
}

.info-label {
  color: #999;
}

.info-value {
  color: #333;
  font-weight: 500;
}

.quick-nav {
  display: flex;
  gap: 12px;
  margin-top: 20px;
}

.nav-item {
  flex: 1;
  background: white;
  border-radius: 12px;
  padding: 20px;
  text-align: center;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.nav-item:active {
  transform: scale(0.98);
}

.nav-icon {
  display: block;
  font-size: 32px;
  margin-bottom: 8px;
}

.nav-text {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}
</style>
