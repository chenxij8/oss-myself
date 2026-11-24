<template>
  <view class="reg-container">
    <view class="reg-box">
      <view class="reg-title">创建账号</view>
      <view class="reg-subtitle">注册后进入用户端</view>

      <view class="form-group">
        <view class="form-label">用户名</view>
        <input v-model="form.username" class="form-input" type="text" placeholder="请输入用户名" />
      </view>
      <view class="form-group">
        <view class="form-label">邮箱（选填）</view>
        <input v-model="form.email" class="form-input" type="text" placeholder="name@example.com" />
      </view>
      <view class="form-group">
        <view class="form-label">密码</view>
        <input v-model="form.password" class="form-input" type="password" placeholder="至少 6 位" />
      </view>
      <view class="form-group">
        <view class="form-label">确认密码</view>
        <input v-model="form.confirm" class="form-input" type="password" placeholder="再次输入密码" />
      </view>

      <button class="reg-btn" :disabled="loading" @click="handleRegister">
        {{ loading ? '注册中...' : '注册并进入用户端' }}
      </button>
      <view class="reg-footer" @click="goLogin">已有账号？返回登录</view>
      <view class="reg-tip">注册用户默认角色为 USER，可使用文件托管与图床功能</view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { register, login } from '@/api/index'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const form = ref({ username: '', email: '', password: '', confirm: '' })
const loading = ref(false)

const handleRegister = async () => {
  if (!form.value.username.trim()) {
    uni.showToast({ title: '请输入用户名', icon: 'none' })
    return
  }
  if (form.value.password.length < 6) {
    uni.showToast({ title: '密码至少 6 位', icon: 'none' })
    return
  }
  if (form.value.password !== form.value.confirm) {
    uni.showToast({ title: '两次密码不一致', icon: 'none' })
    return
  }
  loading.value = true
  try {
    await register(form.value.username.trim(), form.value.password, form.value.email.trim() || undefined)
    const res = await login(form.value.username.trim(), form.value.password)
    userStore.setUserInfo(res.data)
    uni.showToast({ title: '注册成功', icon: 'success', duration: 1000 })
    setTimeout(() => uni.reLaunch({ url: '/pages/index/index' }), 1000)
  } catch (e: any) {
    uni.showToast({ title: e?.message || '注册失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

const goLogin = () => uni.navigateBack()
</script>

<style scoped>
.reg-container { min-height: 100vh; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); padding: 40px 24px; display: flex; align-items: flex-start; justify-content: center; }
.reg-box { width: 100%; max-width: 420px; background: #fff; border-radius: 16px; padding: 30px 24px; box-shadow: 0 10px 30px rgba(0,0,0,.12); }
.reg-title { font-size: 22px; font-weight: bold; color: #333; text-align: center; }
.reg-subtitle { font-size: 13px; color: #999; text-align: center; margin: 6px 0 24px; }
.form-group { margin-bottom: 16px; }
.form-label { font-size: 13px; color: #666; margin-bottom: 6px; }
.form-input { width: 100%; height: 44px; padding: 0 12px; border: 1px solid #e5e7eb; border-radius: 8px; font-size: 14px; box-sizing: border-box; }
.reg-btn { width: 100%; height: 46px; background: #667eea; color: #fff; border-radius: 8px; font-size: 15px; font-weight: bold; border: none; margin-top: 8px; }
.reg-footer { text-align: center; color: #667eea; font-size: 13px; margin-top: 16px; }
.reg-tip { margin-top: 16px; padding: 10px; background: #f5f7ff; border-radius: 8px; font-size: 11px; color: #888; text-align: center; line-height: 1.6; }
</style>
