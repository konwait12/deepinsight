import apiClient from '../client'
import { API_ENDPOINTS } from '@/constants'
import type { ApiResponse } from '@/types/common'
import type { LoginRequest, RegisterRequest, AuthData, User } from '@/types/models'

export const authApi = {
  login(data: LoginRequest) {
    return apiClient.post<ApiResponse<AuthData>>(API_ENDPOINTS.AUTH.LOGIN, data)
  },

  register(data: RegisterRequest) {
    return apiClient.post<ApiResponse<AuthData>>(API_ENDPOINTS.AUTH.REGISTER, data)
  },

  me() {
    return apiClient.get<ApiResponse<User>>(API_ENDPOINTS.AUTH.ME)
  },
}
