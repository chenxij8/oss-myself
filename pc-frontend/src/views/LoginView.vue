<template>
  <div class="login-wrap">
    <div class="card login-card">
      <div class="login-logo">F</div>
      <h1 class="login-title">文件中心 · 登录</h1>
      <form @submit.prevent="handleLogin">
        <div class="field">
          <label>用户名</label>
          <input v-model="username" class="input" placeholder="请输入用户名" autocomplete="username" />
        </div>
        <div class="field">
          <label>密码</label>
          <input v-model="password" class="input" type="password" placeholder="请输入密码" autocomplete="current-password" />
        </div>
        <div v-if="error" class="error-text">{{ error }}</div>
        <button class="btn btn-primary" style="width:100%;margin-top:8px" :disabled="loading">
          {{ loading ? '登录中...' : '登录' }}
        </button>
      </form>
      <div class="login-tip">
        还没有账号？<RouterLink to="/register">立即注册</RouterLink><br />
        注册用户默认进入用户端，可使用文件托管与图床功能。
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { login } from '@/api/index'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const username = ref('')
const password = ref('')
const error = ref('')
const loading = ref(false)

async function handleLogin() {
  if (!username.value.trim() || !password.value) {
    error.value = '请输入用户名和密码'
    return
  }
  loading.value = true
  error.value = ''
  try {
    const res = await login(username.value.trim(), password.value)
    userStore.setLogin(res.data)
    const redirect = route.query.redirect as string | undefined
    router.push(redirect && redirect.startsWith('/') ? redirect : '/files')
  } catch (e: any) {
    error.value = e?.message || '登录失败，请重试'
  } finally {
    loading.value = false
  }
}
</script>
