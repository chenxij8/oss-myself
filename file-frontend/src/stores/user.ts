import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { LoginResponse } from '@/api/index'
import type { PersistedStateStorage } from 'pinia-plugin-persistedstate'

/** 基于 uni storage 的适配器，H5/小程序/App 通用 */
const uniStorage: PersistedStateStorage = {
  getItem: (key: string) => uni.getStorageSync(key) || null,
  setItem: (key: string, value: string) => uni.setStorageSync(key, value),
  removeItem: (key: string) => uni.removeStorageSync(key)
}

/**
 * 用户 Store —— token 的唯一事实来源。
 * 使用 uni storage 而非 localStorage，保证 H5/小程序/App 行为一致。
 */
export const useUserStore = defineStore(
  'user',
  () => {
    const userId = ref<number | null>(null)
    const username = ref<string>('')
    const role = ref<string>('')
    const accessToken = ref<string>('')

    const setUserInfo = (user: LoginResponse) => {
      userId.value = user.userId
      username.value = user.username
      role.value = user.role
      accessToken.value = user.accessToken
      uni.setStorageSync('access_token', user.accessToken)
    }

    /** 统一登出：同时清理内存、uni storage 与持久化缓存 */
    const clearUserInfo = () => {
      userId.value = null
      username.value = ''
      role.value = ''
      accessToken.value = ''
      uni.removeStorageSync('access_token')
    }

    const isLoggedIn = () => !!accessToken.value
    const isAdmin = () => role.value === 'ADMIN'

    return {
      userId,
      username,
      role,
      accessToken,
      setUserInfo,
      clearUserInfo,
      isLoggedIn,
      isAdmin
    }
  },
  {
    persist: {
      storage: uniStorage,
      paths: ['userId', 'username', 'role', 'accessToken']
    }
  }
)
