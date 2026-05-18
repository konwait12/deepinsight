import { STORAGE_KEYS } from '@/constants'

const invalidTokenValues = new Set(['', 'undefined', 'null', 'false', 'nan'])

export function normalizeToken(value: string | null | undefined) {
  const token = (value || '').trim()
  if (invalidTokenValues.has(token.toLowerCase())) return null
  return token
}

export function readAuthToken() {
  return normalizeToken(localStorage.getItem(STORAGE_KEYS.TOKEN))
}

export function clearAuthStorage() {
  localStorage.removeItem(STORAGE_KEYS.TOKEN)
  localStorage.removeItem(STORAGE_KEYS.ROLE)
  localStorage.removeItem(STORAGE_KEYS.USERNAME)
}

export function hasStoredAuthToken() {
  return !!readAuthToken()
}
