import apiClient from '../client'
import { API_ENDPOINTS } from '@/constants'
import type { ApiResponse } from '@/types/common'

export const adminApi = {
  status() {
    return apiClient.get<ApiResponse<Record<string, number>>>(API_ENDPOINTS.ADMIN.STATUS)
  },

  users: {
    list() {
      return apiClient.get<ApiResponse<any[]>>(API_ENDPOINTS.ADMIN.USERS)
    },
    setRole(id: number, role: string) {
      return apiClient.put(`${API_ENDPOINTS.ADMIN.USERS}/${id}/role?role=${role}`)
    },
    delete(id: number) {
      return apiClient.delete(`${API_ENDPOINTS.ADMIN.USERS}/${id}`)
    },
  },

  aiConfigs: {
    list() {
      return apiClient.get<ApiResponse<any[]>>(API_ENDPOINTS.ADMIN.AI_CONFIGS)
    },
    save(id: number | null, data: any) {
      const url = id ? `${API_ENDPOINTS.ADMIN.AI_CONFIGS}/${id}` : API_ENDPOINTS.ADMIN.AI_CONFIGS
      return id ? apiClient.put(url, data) : apiClient.post(url, data)
    },
    activate(id: number) {
      return apiClient.put(`${API_ENDPOINTS.ADMIN.AI_CONFIGS}/${id}/activate`)
    },
    delete(id: number) {
      return apiClient.delete(`${API_ENDPOINTS.ADMIN.AI_CONFIGS}/${id}`)
    },
  },

  aiKnowledgeTraining: {
    status() {
      return apiClient.get<ApiResponse<any>>('/ai/knowledge/train/status')
    },
    train() {
      return apiClient.post<ApiResponse<any>>('/ai/knowledge/train')
    },
  },

  knowledgeNodes: {
    list() {
      return apiClient.get<ApiResponse<any[]>>(API_ENDPOINTS.ADMIN.KNOWLEDGE_NODES)
    },
  },

  knowledgeArticles: {
    list() {
      return apiClient.get<ApiResponse<any[]>>(API_ENDPOINTS.ADMIN.KNOWLEDGE_ARTICLES)
    },
    delete(id: number) {
      return apiClient.delete(`${API_ENDPOINTS.ADMIN.KNOWLEDGE_ARTICLES}/${id}`)
    },
  },

  forumPosts: {
    list() {
      return apiClient.get<ApiResponse<any[]>>(API_ENDPOINTS.ADMIN.FORUM_POSTS)
    },
    delete(id: number) {
      return apiClient.delete(`${API_ENDPOINTS.ADMIN.FORUM_POSTS}/${id}`)
    },
  },

  datasets: {
    list() {
      return apiClient.get<ApiResponse<any[]>>(API_ENDPOINTS.ADMIN.DATASETS)
    },
  },

  trainingJobs: {
    list() {
      return apiClient.get<ApiResponse<any[]>>(API_ENDPOINTS.ADMIN.TRAINING_JOBS)
    },
  },
}
