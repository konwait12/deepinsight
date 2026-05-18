/** 统一API响应包裹 */
export interface ApiResponse<T = unknown> {
  code: number
  message?: string
  data: T
}

/** 分页数据结构 */
export interface PaginatedData<T> {
  content: T[]
  totalElements: number
  totalPages: number
  page: number
  size: number
}

/** 通用状态枚举 */
export type Status = 'pending' | 'running' | 'completed' | 'failed' | 'paused' | 'queued'

/** 主题模式 */
export type ThemeMode = 'light' | 'dark'

/** 语言 */
export type Lang = 'zh' | 'en'
