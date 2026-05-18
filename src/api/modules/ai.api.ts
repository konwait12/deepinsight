import apiClient from '../client'
import { API_ENDPOINTS } from '@/constants'
import type { ApiResponse } from '@/types/common'

export interface ChatRequest {
  message: string
  history?: Array<{ role: string; content: string }>
  conversationId?: number
  temperature?: number
  deepThink?: boolean
  reasoningLevel?: 'off' | 'quick' | 'low' | 'deep' | 'max'
  attachments?: any[]
  resources?: any[]
}

export interface WorkspaceFolderRequest {
  name: string
  parentId?: number | null
}

export interface WorkspaceMoveFolderRequest {
  parentId?: number | null
  sortOrder?: number
}

export interface WorkspaceMoveItemRequest {
  folderId?: number | null
  sortOrder?: number
}

export const aiApi = {
  chat(data: ChatRequest) {
    return apiClient.post<ApiResponse<{ reply: string; conversationId?: number }>>(API_ENDPOINTS.AI.CHAT, data)
  },

  listConversations() {
    return apiClient.get<ApiResponse<any[]>>('/ai/conversations')
  },

  getMessages(conversationId: number) {
    return apiClient.get<ApiResponse<any[]>>(`/ai/conversations/${conversationId}/messages`)
  },

  deleteConversation(id: number) {
    return apiClient.delete(`/ai/conversations/${id}`)
  },

  listWorkspaceResources() {
    return apiClient.get<ApiResponse<any[]>>('/ai/workspace/resources')
  },

  listWorkspaceItems(limit = 50) {
    return apiClient.get<ApiResponse<any[]>>(`/ai/workspace/items?limit=${limit}`)
  },

  getWorkspaceTree() {
    return apiClient.get<ApiResponse<{ folders: any[]; items: any[] }>>('/ai/workspace/tree')
  },

  createWorkspaceFolder(data: WorkspaceFolderRequest) {
    return apiClient.post<ApiResponse<any>>('/ai/workspace/folders', data)
  },

  renameWorkspaceFolder(folderId: number, name: string) {
    return apiClient.put<ApiResponse<any>>(`/ai/workspace/folders/${folderId}/rename`, { name })
  },

  moveWorkspaceFolder(folderId: number, data: WorkspaceMoveFolderRequest) {
    return apiClient.put<ApiResponse<any>>(`/ai/workspace/folders/${folderId}/move`, data)
  },

  deleteWorkspaceFolder(folderId: number) {
    return apiClient.delete<ApiResponse<null>>(`/ai/workspace/folders/${folderId}`)
  },

  uploadWorkspaceFiles(files: File[], folderId?: number | null) {
    const form = new FormData()
    files.forEach((file) => form.append('files', file))
    const query = folderId == null ? '' : `?folderId=${folderId}`
    return apiClient.post<ApiResponse<any[]>>(`/ai/workspace/upload${query}`, form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },

  saveWorkspaceItem(data: any) {
    return apiClient.post<ApiResponse<any>>('/ai/workspace/items', data)
  },

  renameWorkspaceItem(itemId: number, name: string) {
    return apiClient.put<ApiResponse<any>>(`/ai/workspace/items/${itemId}/rename`, { name })
  },

  moveWorkspaceItem(itemId: number, data: WorkspaceMoveItemRequest) {
    return apiClient.put<ApiResponse<any>>(`/ai/workspace/items/${itemId}/move`, data)
  },

  deleteWorkspaceItem(itemId: number) {
    return apiClient.delete<ApiResponse<null>>(`/ai/workspace/items/${itemId}`)
  },

  downloadWorkspaceItem(itemId: number) {
    return apiClient.get(`/ai/workspace/items/${itemId}/download`, { responseType: 'blob' })
  },

  saveVisualView(data: any) {
    return apiClient.post<ApiResponse<any>>('/ai/workspace/visual-views', data)
  },
}
