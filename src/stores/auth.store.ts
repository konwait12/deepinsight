import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api'
import { STORAGE_KEYS } from '@/constants'
import { clearAuthStorage, readAuthToken } from '@/utils/authState'

export const useAuthStore = defineStore('auth', () => {
  const rawToken = readAuthToken()
  const rawUsername = localStorage.getItem(STORAGE_KEYS.USERNAME)
  const rawRole = localStorage.getItem(STORAGE_KEYS.ROLE)
  const token = ref(rawToken)
  const isBootstrapped = ref(false)
  const user = ref<{ username: string; role: string } | null>(
    token.value && rawUsername ? { username: rawUsername, role: rawRole || '' } : null,
  )

  const isAuthenticated = computed(() => isBootstrapped.value && !!token.value && !!user.value)

  const persistUser = (username: string, role: string) => {
    user.value = { username, role }
    localStorage.setItem(STORAGE_KEYS.USERNAME, username)
    localStorage.setItem(STORAGE_KEYS.ROLE, role)
  }

  const login = async (loginData: { username: string; password: string }) => {
    const response = await authApi.login(loginData)
    const authToken = response.data?.data?.token
    if (!authToken) throw new Error('Missing auth token in login response')
    token.value = authToken
    localStorage.setItem(STORAGE_KEYS.TOKEN, authToken)
    persistUser(response.data?.data?.username || loginData.username, response.data?.data?.role || '')
    isBootstrapped.value = true
    return response.data.data
  }

  const register = async (registerData: { username: string; password: string }) => {
    const response = await authApi.register(registerData)
    const authToken = response.data?.data?.token
    if (!authToken) throw new Error('Missing auth token in register response')
    token.value = authToken
    localStorage.setItem(STORAGE_KEYS.TOKEN, authToken)
    persistUser(response.data?.data?.username || registerData.username, response.data?.data?.role || '')
    isBootstrapped.value = true
    return response.data.data
  }

  const logout = () => {
    token.value = null
    user.value = null
    isBootstrapped.value = true
    clearAuthStorage()
  }

  const bootstrap = async () => {
    token.value = readAuthToken()
    if (!token.value) {
      logout()
      isBootstrapped.value = true
      return
    }

    try {
      const response = await authApi.me()
      const currentUser = response.data?.data
      if (response.data?.code === 200 && currentUser) {
        persistUser(currentUser.username, currentUser.role)
      } else {
        logout()
      }
    } catch (error: any) {
      const status = error?.response?.status
      const code = error?.response?.data?.code
      if (status === 401 || status === 403 || code === 401 || code === 403) logout()
      else user.value = null
    } finally {
      isBootstrapped.value = true
    }
  }

  return { token, user, isAuthenticated, isBootstrapped, login, register, logout, bootstrap }
})
