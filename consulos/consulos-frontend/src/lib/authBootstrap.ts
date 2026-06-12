import axios from 'axios'
import { apiClient } from '@/lib/apiClient'
import { useAuthStore } from '@/stores/authStore'
import type { ApiResponse } from '@/types/api'
import type { RefreshResponse } from '@/types/auth'
import type { UserProfile } from '@/types/user'

/** Restaure la session depuis le cookie refresh (rechargement de page). */
export async function bootstrapAuthSession(): Promise<boolean> {
  try {
    const refreshResponse = await axios.post<ApiResponse<RefreshResponse>>(
      '/api/v1/auth/refresh',
      {},
      { withCredentials: true },
    )
    const accessToken = refreshResponse.data.data?.accessToken
    if (!accessToken) {
      return false
    }

    useAuthStore.getState().updateToken(accessToken)

    const meResponse = await apiClient.get<ApiResponse<UserProfile>>('/auth/me')
    const user = meResponse.data.data
    if (!user) {
      useAuthStore.getState().logout()
      return false
    }

    useAuthStore.getState().login(accessToken, user)
    return true
  } catch {
    useAuthStore.getState().logout()
    return false
  }
}
