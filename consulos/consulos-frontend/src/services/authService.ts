import { apiClient } from '@/lib/apiClient'
import type { ApiResponse } from '@/types/api'
import type {
  LoginRequest,
  LoginResponse,
  RefreshResponse,
  ResetPasswordRequest,
} from '@/types/auth'
import type { UserProfile } from '@/types/user'

export const authService = {
  login: (dto: LoginRequest) =>
    apiClient.post<ApiResponse<LoginResponse>>('/auth/login', dto),

  refresh: () =>
    apiClient.post<ApiResponse<RefreshResponse>>('/auth/refresh'),

  logout: () => apiClient.post<ApiResponse<null>>('/auth/logout'),

  forgotPassword: (email: string) =>
    apiClient.post<ApiResponse<null>>('/auth/forgot-password', { email }),

  resetPassword: (dto: ResetPasswordRequest) =>
    apiClient.post<ApiResponse<null>>('/auth/reset-password', dto),

  me: () => apiClient.get<ApiResponse<UserProfile>>('/auth/me'),
}
