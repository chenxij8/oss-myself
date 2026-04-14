import { defineStore } from 'pinia'
import { ref } from 'vue'

/**
 * 用户 Store
 */
export const useUserStore = defineStore(
  'user',
  () => {
    // 状态
    const userId = ref<number | null>(null)
    const username = ref<string>('')
    const role = ref<string>('')
    const accessToken = ref<string>('')

    /**
     * 设置用户信息
     */
    const setUserInfo = (user: any) => {
      userId.value = user.userId
      username.value = user.username
      role.value = user.role
      accessToken.value = user.accessToken
    }

    /**
     * 清除用户信息
     */
    const clearUserInfo = () => {
      userId.value = null
      username.value = ''
      role.value = ''
      accessToken.value = ''
    }

    /**
     * 检查是否已登录
     */
    const isLoggedIn = () => {
      return !!accessToken.value
    }

    /**
     * 检查是否为管理员
     */
    const isAdmin = () => {
      return role.value === 'ADMIN'
    }

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
      enabled: true,
      strategies: [
        {
          key: 'user',
          storage: localStorage,
          paths: ['userId', 'username', 'role', 'accessToken']
        }
      ]
    }
  }
)
