import type { UserProfile } from '@/types/user'

export interface LoginRequest {
  email: string
  password: string
}

export interface LoginResponse {
  accessToken: string
  user: UserProfile
}

export interface RefreshResponse {
  accessToken: string
}

export interface ResetPasswordRequest {
  token: string
  newPassword: string
  confirmPassword: string
}
