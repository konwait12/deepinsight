import apiClient from '../client'
import { API_ENDPOINTS } from '@/constants'
import type { ApiResponse } from '@/types/common'
import type { Dataset, DatasetForm } from '@/types/models'

export interface DataAsset {
  key: string
  id: number | string
  type: string
  typeLabel: string
  title: string
  subtype: string
  status: string
  format: string
  sourceType: string
  updatedAt?: string
  summary?: string
  readiness?: string
  route?: string
  cloudSaved?: boolean
  official?: boolean
  readOnly?: boolean
  canManage?: boolean
  canSync?: boolean
  metrics?: Record<string, string | number>
}

export interface DataAssetOverview {
  assets: DataAsset[]
  summary: {
    total: number
    datasets: number
    training: number
    runs: number
    analysis: number
    cloud: number
    ready: number
  }
  updatedAt: string
}

export interface DatasetPreview {
  datasetId?: number
  kind: string
  columns: string[]
  rows: string[][]
  totalRows: number
  statsRows: number
  featureCount: number
  seed: number[]
  message?: string
}

export const datasetApi = {
  list() {
    return apiClient.get<ApiResponse<Dataset[]>>(API_ENDPOINTS.DATASETS)
  },

  getById(id: number) {
    return apiClient.get<ApiResponse<Dataset>>(`${API_ENDPOINTS.DATASETS}/${id}`)
  },

  create(data: DatasetForm) {
    return apiClient.post<ApiResponse<Dataset>>(API_ENDPOINTS.DATASETS, data)
  },

  upload(file: File, data: { name?: string; description?: string; taskType?: string }) {
    const form = new FormData()
    form.append('file', file)
    if (data.name) form.append('name', data.name)
    if (data.description) form.append('description', data.description)
    if (data.taskType) form.append('taskType', data.taskType)
    return apiClient.post<ApiResponse<Dataset>>(`${API_ENDPOINTS.DATASETS}/upload`, form, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 30000,
    })
  },

  preview(id: number) {
    return apiClient.get<ApiResponse<DatasetPreview>>(`${API_ENDPOINTS.DATASETS}/${id}/preview`)
  },

  delete(id: number) {
    return apiClient.delete(`${API_ENDPOINTS.DATASETS}/${id}`)
  },

  assets() {
    return apiClient.get<ApiResponse<DataAssetOverview>>('/data-assets')
  },

  syncAssetsToCloud(data: { assetKeys: string[]; includeCloudItems?: boolean }) {
    return apiClient.post<ApiResponse<{ savedCount: number; items: any[] }>>('/data-assets/sync-cloud', data)
  },
}
