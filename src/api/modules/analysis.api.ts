import apiClient from '../client'
import { API_ENDPOINTS } from '@/constants'
import type { ApiResponse } from '@/types/common'
import type { ModelComparison, AnalysisOverview, TrainingCurve, HyperparamData } from '@/types/models'

export const analysisApi = {
  modelComparison() {
    return apiClient.get<ApiResponse<ModelComparison[]>>(API_ENDPOINTS.ANALYSIS.MODEL_COMPARISON)
  },

  overview() {
    return apiClient.get<ApiResponse<AnalysisOverview>>(API_ENDPOINTS.ANALYSIS.OVERVIEW)
  },

  trainingCurve(jobId: number) {
    return apiClient.get<ApiResponse<TrainingCurve>>(`${API_ENDPOINTS.ANALYSIS.TRAINING_CURVE}/${jobId}`)
  },

  hyperparams() {
    return apiClient.get<ApiResponse<HyperparamData>>(API_ENDPOINTS.ANALYSIS.HYPERPARAMS)
  },
}
