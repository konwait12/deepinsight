import apiClient from '../client'
import { API_ENDPOINTS } from '@/constants'
import type { ApiResponse } from '@/types/common'
import type { TrainingJob, TrainingConfig, ModelOption } from '@/types/models'

export const trainingApi = {
  list() {
    return apiClient.get<ApiResponse<TrainingJob[]>>(API_ENDPOINTS.TRAINING)
  },

  create(config: TrainingConfig) {
    return apiClient.post<ApiResponse<TrainingJob>>(API_ENDPOINTS.TRAINING, config)
  },

  start(id: number) {
    return apiClient.put(`${API_ENDPOINTS.TRAINING}/${id}/start`)
  },

  pause(id: number) {
    return apiClient.put(`${API_ENDPOINTS.TRAINING}/${id}/pause`)
  },

  stop(id: number) {
    return apiClient.put(`${API_ENDPOINTS.TRAINING}/${id}/stop`)
  },

  delete(id: number) {
    return apiClient.delete(`${API_ENDPOINTS.TRAINING}/${id}`)
  },

  listModels() {
    return apiClient.get<ApiResponse<{ official: ModelOption[]; userModels: ModelOption[] }>>(API_ENDPOINTS.MODELS)
  },

  createModel(data: Partial<ModelOption>) {
    return apiClient.post(API_ENDPOINTS.MODELS, data)
  },

  getArticle(id: number) {
    return apiClient.get<ApiResponse<any>>(`/models/articles/${id}`)
  },
}
