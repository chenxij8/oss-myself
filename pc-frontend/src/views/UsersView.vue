<template>
  <div>
    <div class="page-head">
      <h1 class="page-title">用户管理</h1>
      <p class="page-desc">管理用户角色与状态（仅管理员可见）</p>
    </div>
    <div class="card" style="padding:18px">
      <div class="toolbar">
        <input v-model="keyword" class="input search" placeholder="搜索用户名/邮箱" @keyup.enter="load" />
        <button class="btn" @click="load" :disabled="loading">搜索</button>
        <div class="spacer"></div>
        <span class="tag-gray tag">共 {{ userCount }} 位用户</span>
      </div>
      <table class="data-table">
        <thead>
          <tr>
            <th>用户名</th>
            <th>邮箱</th>
            <th>角色</th>
            <th>状态</th>
            <th>注册时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="user in users" :key="user.id">
            <td>{{ user.username }} <span v-if="user.id === currentUserId" class="tag" style="margin-left:4px">我</span></td>
            <td>{{ user.email || '--' }}</td>
            <td>
              <select class="select" style="width:auto;padding:4px 8px" :value="user.role" @change="changeRole(user, ($event.target as HTMLSelectElement).value)">
                <option value="USER">USER</option>
                <option value="ADMIN">ADMIN</option>
              </select>
            </td>
            <td>
              <span :class="['tag', user.status === 1 ? 'tag-green' : 'tag-red']">
                {{ user.status === 1 ? '启用' : '禁用' }}
              </span>
            </td>
            <td>{{ formatTime(user.createTime) }}</td>
            <td>
              <div class="cell-actions">
                <button class="btn btn-sm btn-text" @click="toggleStatus(user)">
                  {{ user.status === 1 ? '禁用' : '启用' }}
                </button>
                <button class="btn btn-sm btn-text" @click="openReset(user)">重置密码</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-if="resetTarget" class="modal-mask" @click.self="resetTarget = null">
      <div class="modal" style="max-width:400px">
        <div class="modal-head">
          <span class="modal-title">重置「{{ resetTarget.username }}」的密码</span>
          <button class="btn btn-sm" @click="resetTarget = null">关闭</button>
        </div>
        <div class="modal-body">
          <div class="field">
            <label>新密码（至少 6 位）</label>
            <input v-model="newPassword" class="input" type="password" />
          </div>
          <div v-if="actionError" class="error-text">{{ actionError }}</div>
        </div>
        <div class="modal-foot">
          <button class="btn" @click="resetTarget = null">取消</button>
          <button class="btn btn-primary" @click="submitReset">确认重置</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import dayjs from 'dayjs'
import {
  adminListUsers, adminStats, adminUpdateUserStatus, adminUpdateUserRole,
  adminResetPassword, type SysUser
} from '@/api/index'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const users = ref<SysUser[]>([])
const userCount = ref(0)
const keyword = ref('')
const loading = ref(false)
const currentUserId = userStore.userId
const resetTarget = ref<SysUser | null>(null)
const newPassword = ref('')
const actionError = ref('')

onMounted(() => load())

async function load() {
  loading.value = true
  try {
    const [listRes, statsRes] = await Promise.all([
      adminListUsers(1, 200, keyword.value.trim() || undefined),
      adminStats()
    ])
    users.value = listRes.data || []
    userCount.value = statsRes.data.userCount || 0
  } catch { /* ignore */ }
  finally { loading.value = false }
}

async function toggleStatus(user: SysUser) {
  const next = user.status === 1 ? 0 : 1
  if (!confirm(`确定${next === 0 ? '禁用' : '启用'}用户「${user.username}」吗？`)) return
  try {
    await adminUpdateUserStatus(user.id, next)
    user.status = next
  } catch (e: any) { alert(e?.message || '操作失败') }
}

async function changeRole(user: SysUser, role: string) {
  if (!confirm(`确定将「${user.username}」的角色改为 ${role} 吗？`)) return
  try {
    await adminUpdateUserRole(user.id, role)
    user.role = role
  } catch (e: any) {
    alert(e?.message || '操作失败')
    await load()
  }
}

function openReset(user: SysUser) {
  resetTarget.value = user
  newPassword.value = ''
  actionError.value = ''
}

async function submitReset() {
  if (!resetTarget.value) return
  if (newPassword.value.length < 6) { actionError.value = '密码至少 6 位'; return }
  try {
    await adminResetPassword(resetTarget.value.id, newPassword.value)
    resetTarget.value = null
    alert('密码已重置')
  } catch (e: any) { actionError.value = e?.message || '重置失败' }
}

function formatTime(value: string) {
  return value ? dayjs(value).format('YYYY-MM-DD') : '--'
}
</script>
