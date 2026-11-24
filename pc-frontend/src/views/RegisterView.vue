<template>
  <div class="login-wrap">
    <div class="card login-card">
      <div class="login-logo">F</div>
      <h1 class="login-title">创建账号</h1>
      <form @submit.prevent="handleRegister">
        <div class="field">
          <label>用户名</label>
          <input v-model="form.username" class="input" placeholder="请输入用户名" autocomplete="username" />
        </div>
        <div class="field">
          <label>邮箱（选填）</label>
          <input v-model="form.email" class="input" type="email" placeholder="name@example.com" />
        </div>
        <div class="field">
          <label>密码</label>
          <input v-model="form.password" class="input" type="password" placeholder="至少 6 位" autocomplete="new-password" />
        </div>
        <div class="field">
          <label>确认密码</label>
          <input v-model="form.confirm" class="input" type="password" placeholder="再次输入密码" autocomplete="new-password" />
        </div>
        <div v-if="error" class="error-text">{{ error }}</div>
        <button class="btn btn-primary" style="width:100%;margin-top:8px" :disabled="loading">
          {{ loading ? '注册中...' : '注册并进入用户端' }}
        </button>
      </form>
      <div class="login-tip">
        注册成功后角色固定为 <strong>USER</strong>，仅可访问文件管理、图床与我的分享。<br />
        已有账号？<RouterLink to="/login">返回登录</RouterLink>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { register, login } from '@/api/index'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const form = reactive({ username: '', email: '', password: '', confirm: '' })
const error = ref('')
const loading = ref(false)

async function handleRegister() {
  error.value = ''
  if (!form.username.trim()) { error.value = '请输入用户名'; return }
  if (form.password.length < 6) { error.value = '密码长度不能少于 6 位'; return }
  if (form.password !== form.confirm) { error.value = '两次输入的密码不一致'; return }
  loading.value = true
  try {
    await register(form.username.trim(), form.password, form.email.trim() || undefined)
    const res = await login(form.username.trim(), form.password)
    userStore.setLogin(res.data)
    router.push('/files')
  } catch (e: any) {
    error.value = e?.message || '注册失败，请重试'
  } finally {
    loading.value = false
  }
}
</script>
