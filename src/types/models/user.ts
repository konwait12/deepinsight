export interface User {
  id: number
  username: string
  email?: string
  role: UserRole
  avatar?: string
  createdAt?: string
  updatedAt?: string
}

export type UserRole = 'USER' | 'ADMIN'

export interface LoginRequest {
  username: string
  password: string
}

export interface RegisterRequest {
  username: string
  password: string
  email?: string
}

export interface AuthData {
  token: string
  role: UserRole
  username: string
}
