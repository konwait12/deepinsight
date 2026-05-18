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
