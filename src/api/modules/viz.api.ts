import apiClient from '../client'
import { API_ENDPOINTS } from '@/constants'
import type { ApiResponse } from '@/types/common'

export type VisualAnalysisTarget = {
  runId: number
  runType: 'training' | 'upload'
}

export type VisualAnalysisBatchRequest = {
  title?: string
  targets: VisualAnalysisTarget[]
  modules: string[]
}

export type VisualAnalysisSaveRequest = {
  batchId?: number
  resultIds?: number[]
  title?: string
}

export type VisualAnalysisImportChatRequest = {
  batchId?: number
  resultIds?: number[]
  message?: string
}

export const visualizationApi = {
  listExperiments() {
    return apiClient.get<ApiResponse<any[]>>('/experiments')
  },

  listRuns() {
    return apiClient.get<ApiResponse<any[]>>(API_ENDPOINTS.LOGS.RUNS)
  },

  createRun(data: { name: string }) {
    return apiClient.post<ApiResponse<any>>(API_ENDPOINTS.LOGS.RUNS, data)
  },

  uploadRunFiles(runId: number, file: File) {
    const form = new FormData()
    form.append('file', file)
    return apiClient.post<ApiResponse<any>>(`${API_ENDPOINTS.LOGS.RUNS}/${runId}/upload`, form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },

  getScalars(runId: number, tag: string) {
    return apiClient.get<ApiResponse<any[]>>(`${API_ENDPOINTS.VISUAL_DATA}/${runId}/scalars?tag=${encodeURIComponent(tag)}`)
  },

  getTrainingScalars(trainingId: number, type: string, tag: string) {
    return apiClient.get<ApiResponse<any[]>>(`/experiments/${trainingId}/scalars?type=${type}&tag=${encodeURIComponent(tag)}`)
  },

  getTrainingImages(trainingId: number, type: string, tag: string) {
    return apiClient.get<ApiResponse<any[]>>(`/experiments/${trainingId}/images?type=${type}&tag=${encodeURIComponent(tag)}`)
  },

  getTrainingTags(trainingId: number, type: string) {
    return apiClient.get<ApiResponse<string[]>>(`/experiments/${trainingId}/tags?type=${type}`)
  },

  getTrainingAudio(trainingId: number, type: string, tag: string) {
    return apiClient.get<ApiResponse<any[]>>(`/experiments/${trainingId}/audio?type=${type}&tag=${encodeURIComponent(tag)}`)
  },

  getTrainingText(trainingId: number, type: string, tag: string) {
    return apiClient.get<ApiResponse<any[]>>(`/experiments/${trainingId}/text?type=${type}&tag=${encodeURIComponent(tag)}`)
  },

  getTrainingHistograms(trainingId: number, type: string, tag: string) {
    return apiClient.get<ApiResponse<any[]>>(`/experiments/${trainingId}/histograms?type=${type}&tag=${encodeURIComponent(tag)}`)
  },

  getTrainingEmbeddings(trainingId: number, type: string, tag: string) {
    return apiClient.get<ApiResponse<any[]>>(`/experiments/${trainingId}/embeddings?type=${type}&tag=${encodeURIComponent(tag)}`)
  },

  getTrainingPRCurves(trainingId: number, type: string, tag: string) {
    return apiClient.get<ApiResponse<any[]>>(`/experiments/${trainingId}/pr-curves?type=${type}&tag=${encodeURIComponent(tag)}`)
  },

  getTrainingRocCurves(trainingId: number, type: string, tag: string) {
    return apiClient.get<ApiResponse<any[]>>(`/experiments/${trainingId}/roc-curves?type=${type}&tag=${encodeURIComponent(tag)}`)
  },

  getTrainingHParams(trainingId: number, type: string) {
    return apiClient.get<ApiResponse<any[]>>(`/experiments/${trainingId}/hparams?type=${type}`)
  },

  getTrainingProfiler(trainingId: number, type: string, tag: string) {
    return apiClient.get<ApiResponse<any[]>>(`/experiments/${trainingId}/profiler?type=${type}&tag=${encodeURIComponent(tag)}`)
  },

  getAnalysisModules() {
    return apiClient.get<ApiResponse<any[]>>('/visual-analysis/modules')
  },

  listAnalysisBatches(limit = 6) {
    return apiClient.get<ApiResponse<any[]>>(`/visual-analysis/batches?limit=${limit}`)
  },

  getAnalysisBatch(batchId: number) {
    return apiClient.get<ApiResponse<any>>(`/visual-analysis/batches/${batchId}`)
  },

  createAnalysisBatch(data: VisualAnalysisBatchRequest) {
    return apiClient.post<ApiResponse<any>>('/visual-analysis/batches', data)
  },

  regenerateAnalysisAiPanel(resultId: number) {
    return apiClient.post<ApiResponse<any>>(`/visual-analysis/results/${resultId}/ai-panel`)
  },

  regenerateModelAiPanel(resultId: number) {
    return apiClient.post<ApiResponse<any>>(`/visual-analysis/results/${resultId}/ai-panel/model`)
  },

  regenerateRuleAiPanel(resultId: number) {
    return apiClient.post<ApiResponse<any>>(`/visual-analysis/results/${resultId}/ai-panel/rule`)
  },

  saveAnalysisResults(data: VisualAnalysisSaveRequest) {
    return apiClient.post<ApiResponse<any>>('/visual-analysis/results/save', data)
  },

  listSavedAnalysisResults(limit = 50) {
    return apiClient.get<ApiResponse<any[]>>(`/visual-analysis/results/saved?limit=${limit}`)
  },

  importAnalysisResultsToChat(data: VisualAnalysisImportChatRequest) {
    return apiClient.post<ApiResponse<any>>('/visual-analysis/results/import-chat', data)
  },

  saveVisualView(data: any) {
    return apiClient.post<ApiResponse<any>>('/ai/workspace/visual-views', data)
  },
}

export const adminApi = {
  status() {
    return apiClient.get<ApiResponse<Record<string, number>>>(API_ENDPOINTS.ADMIN.STATUS)
  },
}
