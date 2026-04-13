<template>
  <view class="login-container">
    <view class="login-box">
      <view class="login-title">文件共享管理系统</view>
      <view class="login-subtitle">企业级文件安全分享平台</view>

      <!-- 登录表单 -->
      <form @submit.prevent="handleLogin">
        <!-- 用户名输入 -->
        <view class="form-group">
          <view class="form-label">用户名</view>
          <input
            v-model="form.username"
            class="form-input"
            type="text"
            placeholder="请输入用户名（admin）"
            @blur="form.username = form.username.trim()"
          />
        </view>

        <!-- 密码输入 -->
        <view class="form-group">
          <view class="form-label">密码</view>
          <input
            v-model="form.password"
            class="form-input"
            :type="showPassword ? 'text' : 'password'"
            placeholder="请输入密码"
          />
          <view class="password-toggle" @click="showPassword = !showPassword">
            {{ showPassword ? '隐藏' : '显示' }}
          </view>
        </view>

        <!-- 登录按钮 -->
        <button class="login-btn" :disabled="loading">
          {{ loading ? '登录中...' : '登录' }}
        </button>
      </form>

      <!-- 提示信息 -->
      <view class="login-tips">
        <view class="tips-title">测试账号</view>
        <view class="tips-text">用户名: admin</view>
        <view class="tips-text">密码: admin123</view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { login } from '@/api'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

// 表单数据
const form = ref({
  username: 'admin',
  password: ''
})

const showPassword = ref(false)
const loading = ref(false)

/**
 * 处理登录
 */
const handleLogin = async () => {
  // 参数校验
  if (!form.value.username.trim()) {
    uni.showToast({ title: '用户名不能为空', icon: 'error' })
    return
  }

  if (!form.value.password.trim()) {
    uni.showToast({ title: '密码不能为空', icon: 'error' })
    return
  }

  try {
    loading.value = true

    // 调用登录 API
    const response = await login(form.value.username, form.value.password)

    if (response.code === 0) {
      // 保存用户信息和 token
      const loginData = response.data
      userStore.setUserInfo(loginData)
      uni.setStorageSync('access_token', loginData.accessToken)

      // 显示成功提示
      uni.showToast({ title: '登录成功', icon: 'success', duration: 1500 })

      // 延迟跳转到首页
      setTimeout(() => {
        uni.navigateTo({
          url: '/pages/index/index'
        })
      }, 1500)
    } else {
      uni.showToast({ title: response.message || '登录失败', icon: 'error' })
    }
  } catch (error: any) {
    uni.showToast({ title: error.message || '登录失败，请重试', icon: 'error' })
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  width: 100%;
  height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.login-box {
  width: 100%;
  max-width: 400px;
  background: white;
  border-radius: 12px;
  padding: 40px 30px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.15);
}

.login-title {
  font-size: 28px;
  font-weight: bold;
  color: #333;
  text-align: center;
  margin-bottom: 8px;
}

.login-subtitle {
  font-size: 14px;
  color: #999;
  text-align: center;
  margin-bottom: 30px;
}

.form-group {
  margin-bottom: 20px;
  position: relative;
}

.form-label {
  font-size: 14px;
  color: #333;
  margin-bottom: 8px;
  display: block;
  font-weight: 500;
}

.form-input {
  width: 100%;
  height: 44px;
  border: 1px solid #ddd;
  border-radius: 6px;
  padding: 0 12px;
  font-size: 14px;
  box-sizing: border-box;
  transition: border-color 0.3s;
}

.form-input:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.password-toggle {
  position: absolute;
  right: 12px;
  top: 38px;
  font-size: 12px;
  color: #667eea;
  cursor: pointer;
}

.login-btn {
  width: 100%;
  height: 48px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 16px;
  font-weight: bold;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
  margin-top: 10px;
}

.login-btn:active {
  transform: scale(0.98);
}

.login-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.login-tips {
  margin-top: 30px;
  padding: 15px;
  background: #f5f5f5;
  border-radius: 6px;
}

.tips-title {
  font-size: 12px;
  font-weight: bold;
  color: #333;
  margin-bottom: 8px;
}

.tips-text {
  font-size: 12px;
  color: #666;
  margin: 4px 0;
}
</style>
