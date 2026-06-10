import apiClient from '../client'
import { API_ENDPOINTS } from '@/constants'
import type { ApiResponse } from '@/types/common'
import type { ModelOption } from '@/types/models'

export const predictionApi = {
  listModels() {
    return apiClient.get<ApiResponse<ModelOption[]>>(API_ENDPOINTS.PREDICTION.MODELS)
  },

  recommend(data: { user_history: number[]; user_id?: string; top_k?: number; include_job_info?: boolean }) {
    return apiClient.post<ApiResponse<any>>(API_ENDPOINTS.PREDICTION.RECOMMEND, data)
  },

  health() {
    return apiClient.get<ApiResponse<Record<string, unknown>>>(`${API_ENDPOINTS.PREDICTION.HEALTH}`)
  },
}
