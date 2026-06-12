import { create } from 'zustand'
import type { UserProfile } from '@/types/user'

interface AuthState {
  user: UserProfile | null
  accessToken: string | null
  isAuthenticated: boolean
  login: (token: string, user: UserProfile) => void
  logout: () => void
  updateToken: (token: string) => void
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  accessToken: null,
  isAuthenticated: false,
  login: (token, user) =>
    set({
      accessToken: token,
      user,
      isAuthenticated: true,
    }),
  logout: () =>
    set({
      accessToken: null,
      user: null,
      isAuthenticated: false,
    }),
  updateToken: (token) =>
    set({
      accessToken: token,
      isAuthenticated: true,
    }),
}))
