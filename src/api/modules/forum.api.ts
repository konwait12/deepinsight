import apiClient from '../client'
import { API_ENDPOINTS } from '@/constants'
import type { ApiResponse } from '@/types/common'

export const forumApi = {
  listPosts() {
    return apiClient.get<ApiResponse<any[]>>(API_ENDPOINTS.FORUM.POSTS)
  },

  getPost(id: number | string) {
    return apiClient.get<ApiResponse<any>>(`${API_ENDPOINTS.FORUM.POSTS}/${id}`)
  },

  createPost(data: { title: string; content: string; coverUrl?: string | null }) {
    return apiClient.post<ApiResponse<any>>(API_ENDPOINTS.FORUM.POSTS, data)
  },

  deletePost(id: number) {
    return apiClient.delete(`${API_ENDPOINTS.FORUM.POSTS}/${id}`)
  },

  getComments(postId: number | string) {
    return apiClient.get<ApiResponse<any[]>>(`${API_ENDPOINTS.FORUM.POSTS}/${postId}/comments`)
  },

  addComment(postId: number | string, content: string, parentId?: number | null) {
    return apiClient.post<ApiResponse<any>>(`${API_ENDPOINTS.FORUM.POSTS}/${postId}/comments`, { content, parentId })
  },

  uploadFiles(files: File[]) {
    const form = new FormData()
    for (const f of files) form.append('files', f)
    return apiClient.post<ApiResponse<Array<{ name: string; url: string; size: string }>>>(API_ENDPOINTS.FORUM.UPLOAD, form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },

  listKnowledgeNodes() {
    return apiClient.get<ApiResponse<any[]>>(API_ENDPOINTS.FORUM.KNOWLEDGE_NODES)
  },

  listKnowledgeArticles() {
    return apiClient.get<ApiResponse<any[]>>(API_ENDPOINTS.FORUM.KNOWLEDGE_ARTICLES)
  },

  getKnowledgeArticle(id: number | string) {
    return apiClient.get<ApiResponse<any>>(`${API_ENDPOINTS.FORUM.KNOWLEDGE_ARTICLES}/${id}`)
  },
}
