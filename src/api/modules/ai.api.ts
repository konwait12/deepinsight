import apiClient from '../client'
import { API_BASE_URL, API_ENDPOINTS } from '@/constants'
import type { ApiResponse } from '@/types/common'
import { readAuthToken } from '@/utils/authState'

export interface ChatNavigation {
  key: string
  label: string
  path: string
  description: string
  promptHint?: string
  confidence?: number
  reply?: string
  mode?: 'direct' | 'suggested'
  reason?: string
}

export interface ChatRelatedArticle {
  id: number
  source: string
  sourceLabel: string
  title: string
  snippet: string
  path?: string
  nodeId?: string
  category?: string
  tags?: string
  official?: boolean
  pinned?: boolean
  score?: number
}

export interface ChatWebResult {
  title: string
  url: string
  snippet: string
  source: string
  rank?: number
  refId?: string
}

export interface ChatReasoningDiagnostics {
  level?: string
  label?: string
  enabled?: boolean
  strategy?: string
  temperature?: number
  related?: {
    matched?: number
    limit?: number
  }
  web?: {
    requested?: boolean
    attempted?: boolean
    returned?: number
    limit?: number
    query?: string
  }
  attachments?: {
    count?: number
  }
  context?: {
    memoryUsed?: boolean
    kbDocsFound?: boolean
    siteContextUsed?: boolean
    workspaceUsed?: boolean
  }
  model?: {
    provider?: string
    model?: string
    effectiveModel?: string
    nativeReasoning?: boolean
    nativeReasoningLabel?: string
    maxTokens?: number
    appliedControls?: string[]
  }
}

export interface ChatApiStatus {
  status?: string
  provider?: string
  host?: string
  errorType?: string
  httpStatus?: number
  localFallback?: boolean
}

export interface ChatRequest {
  message: string
  history?: Array<{ role: string; content: string }>
  conversationId?: number
  temperature?: number
  deepThink?: boolean
  reasoningLevel?: 'off' | 'quick' | 'low' | 'deep' | 'max'
  webSearch?: boolean
  attachments?: any[]
  resources?: any[]
}

export interface ChatStreamData {
  phase?: string
  message?: string
  delta?: string
  conversationId?: number
  reply?: string
  role?: string
  reasoning?: string
  navigation?: ChatNavigation
  relatedArticles?: ChatRelatedArticle[]
  webResults?: ChatWebResult[]
  reasoningDiagnostics?: ChatReasoningDiagnostics
  webSearchAttempted?: boolean
  webSearchUsed?: boolean
  webSearchQuery?: string
  webResultCount?: number
  siteContextUsed?: boolean
  apiStatus?: ChatApiStatus
  ok?: boolean
  [key: string]: any
}

export type ChatStreamEventName = 'status' | 'reasoning' | 'content' | 'metadata' | 'done' | 'error' | 'message'

export interface ChatStreamHandlers {
  onStatus?: (data: ChatStreamData) => void
  onReasoning?: (data: ChatStreamData) => void
  onContent?: (data: ChatStreamData) => void
  onMetadata?: (data: ChatStreamData) => void
  onDone?: (data: ChatStreamData) => void
  onError?: (data: ChatStreamData) => void
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
    return apiClient.post<ApiResponse<{
      reply: string
      conversationId?: number
      reasoning?: string
      navigation?: ChatNavigation
      relatedArticles?: ChatRelatedArticle[]
      webResults?: ChatWebResult[]
      reasoningDiagnostics?: ChatReasoningDiagnostics
      webSearchAttempted?: boolean
      webSearchUsed?: boolean
      webSearchQuery?: string
      siteContextUsed?: boolean
      apiStatus?: ChatApiStatus
    }>>(API_ENDPOINTS.AI.CHAT, data, { timeout: 90000 })
  },

  async chatStream(data: ChatRequest, handlers: ChatStreamHandlers = {}, signal?: AbortSignal) {
    const token = readAuthToken()
    const response = await fetch(`${API_BASE_URL}${API_ENDPOINTS.AI.CHAT_STREAM}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: JSON.stringify(data),
      signal,
    })

    if (!response.ok) {
      const body = await response.text().catch(() => '')
      throw {
        response: {
          status: response.status,
          data: { message: body || response.statusText },
        },
        message: body || response.statusText,
      }
    }
    if (!response.body) {
      throw new Error('Streaming response is not supported by this browser.')
    }

    const decoder = new TextDecoder('utf-8')
    const reader = response.body.getReader()
    let buffer = ''

    const dispatch = (block: string) => {
      const parsed = parseSseBlock(block)
      if (!parsed) return
      if (parsed.event === 'status') handlers.onStatus?.(parsed.data)
      else if (parsed.event === 'reasoning') handlers.onReasoning?.(parsed.data)
      else if (parsed.event === 'content') handlers.onContent?.(parsed.data)
      else if (parsed.event === 'metadata') handlers.onMetadata?.(parsed.data)
      else if (parsed.event === 'done') handlers.onDone?.(parsed.data)
      else if (parsed.event === 'error') handlers.onError?.(parsed.data)
    }

    while (true) {
      const { value, done } = await reader.read()
      buffer += decoder.decode(value || new Uint8Array(), { stream: !done }).replace(/\r\n/g, '\n')
      let splitAt = buffer.indexOf('\n\n')
      while (splitAt >= 0) {
        const block = buffer.slice(0, splitAt)
        buffer = buffer.slice(splitAt + 2)
        dispatch(block)
        splitAt = buffer.indexOf('\n\n')
      }
      if (done) break
    }
    if (buffer.trim()) dispatch(buffer)
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

function parseSseBlock(block: string): { event: ChatStreamEventName; data: ChatStreamData } | null {
  let event: ChatStreamEventName = 'message'
  const dataLines: string[] = []

  for (const rawLine of block.split('\n')) {
    const line = rawLine.trimEnd()
    if (!line || line.startsWith(':')) continue
    if (line.startsWith('event:')) {
      const name = line.slice('event:'.length).trim()
      event = (name || 'message') as ChatStreamEventName
    } else if (line.startsWith('data:')) {
      dataLines.push(line.slice('data:'.length).trimStart())
    }
  }

  if (!dataLines.length) return null
  const dataText = dataLines.join('\n')
  try {
    return { event, data: JSON.parse(dataText) }
  } catch {
    return { event, data: { message: dataText, delta: dataText } }
  }
}
