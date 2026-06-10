import { clearAuthStorage } from '@/utils/authState'

export function isAiAuthError(error: any) {
  const status = Number(error?.response?.status)
  const code = Number(error?.response?.data?.code)
  const message = String(error?.response?.data?.message || error?.message || '')
  return status === 401 || status === 403 || code === 401 || code === 403 || message.includes('请先登录')
}

export function clearAiAuthIfNeeded(error: any) {
  if (!isAiAuthError(error)) return false
  clearAuthStorage()
  return true
}

export function formatAiErrorMessage(error: any, isZh = true) {
  const serverMessage = String(error?.response?.data?.message || '').trim()
  const clientMessage = String(error?.message || '').trim()
  if (isAiAuthError(error)) {
    return isZh
      ? '登录状态已失效，请重新登录后再使用 AI。'
      : 'Your session has expired. Please sign in again before using AI.'
  }
  if (serverMessage) {
    return isZh ? `AI 服务暂时不可用：${serverMessage}` : `AI is temporarily unavailable: ${serverMessage}`
  }
  if (clientMessage) {
    return isZh ? `AI 服务暂时不可用：${clientMessage}` : `AI is temporarily unavailable: ${clientMessage}`
  }
  return isZh ? 'AI 服务暂时不可用：未知错误' : 'AI is temporarily unavailable: unknown error'
}
