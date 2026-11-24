import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { LoginResponse } from '@/api/index'
import { TOKEN_KEY } from '@/api/http'

export const useUserStore = defineStore('user', () => {
  const userId = ref<number | null>(null)
  const username = ref('')
  const role = ref('')

  function setLogin(data: LoginResponse) {
    userId.value = data.userId
    username.value = data.username
    role.value = data.role
    localStorage.setItem(TOKEN_KEY, data.accessToken)
    localStorage.setItem('user_info', JSON.stringify({
      userId: data.userId,
      username: data.username,
      role: data.role
    }))
  }

  function restore() {
    if (userId.value) return
    try {
      const raw = localStorage.getItem('user_info')
      if (raw) {
        const info = JSON.parse(raw)
        userId.value = info.userId
        username.value = info.username
        role.value = info.role
      }
    } catch {
      clear()
    }
  }

  function clear() {
    userId.value = null
    username.value = ''
    role.value = ''
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem('user_info')
  }

  const isLoggedIn = () => !!localStorage.getItem(TOKEN_KEY)
  const isAdmin = () => role.value === 'ADMIN'

  restore()

  return { userId, username, role, setLogin, clear, isLoggedIn, isAdmin }
})
