import axios from 'axios'
import { API_BASE_URL } from '@/constants'
import { clearAuthStorage, readAuthToken } from '@/utils/authState'

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' },
})

apiClient.interceptors.request.use((config) => {
  const token = readAuthToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      clearAuthStorage()
    }
    return Promise.reject(error)
  },
)

export default apiClient
