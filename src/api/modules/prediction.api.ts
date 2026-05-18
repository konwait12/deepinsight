import apiClient from '../client'
import { API_ENDPOINTS } from '@/constants'
import type { ApiResponse } from '@/types/common'
import type { ModelOption } from '@/types/models'

export const predictionApi = {
  listModels() {
    return apiClient.get<ApiResponse<ModelOption[]>>(API_ENDPOINTS.PREDICTION.MODELS)
  },

  classify(data: { model: string }) {
    return apiClient.post<ApiResponse<any>>(API_ENDPOINTS.PREDICTION.CLASSIFY, data)
  },
}
