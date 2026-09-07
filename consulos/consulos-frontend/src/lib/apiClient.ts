import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios'
import { resolveApiErrorMessage, shouldToastApiError } from '@/lib/apiErrors'
import { toast } from '@/hooks/use-toast'
import { useAuthStore } from '@/stores/authStore'

export const apiClient = axios.create({
  baseURL: '/api/v1',
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
  },
})

apiClient.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = useAuthStore.getState().accessToken
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

const AUTH_NO_REFRESH_PATHS = [
  '/auth/login',
  '/auth/logout',
  '/auth/forgot-password',
  '/auth/reset-password',
]

let isRefreshing = false
let refreshQueue: Array<(token: string) => void> = []

const processQueue = (token: string) => {
  refreshQueue.forEach((callback) => callback(token))
  refreshQueue = []
}

apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean }

    if (shouldToastApiError(error)) {
      toast({
        variant: 'destructive',
        title: resolveApiErrorMessage(error),
      })
    }

    if (error.response?.status !== 401 || originalRequest._retry) {
      return Promise.reject(error)
    }

    if (AUTH_NO_REFRESH_PATHS.some((path) => originalRequest.url?.includes(path))) {
      return Promise.reject(error)
    }

    if (originalRequest.url?.includes('/auth/refresh')) {
      useAuthStore.getState().logout()
      window.location.href = '/login'
      return Promise.reject(error)
    }

    if (isRefreshing) {
      return new Promise((resolve) => {
        refreshQueue.push((token: string) => {
          originalRequest.headers.Authorization = `Bearer ${token}`
          resolve(apiClient(originalRequest))
        })
      })
    }

    originalRequest._retry = true
    isRefreshing = true

    try {
      const response = await axios.post(
        '/api/v1/auth/refresh',
        {},
        { withCredentials: true },
      )
      const newToken = response.data?.data?.accessToken as string
      useAuthStore.getState().updateToken(newToken)
      processQueue(newToken)
      originalRequest.headers.Authorization = `Bearer ${newToken}`
      return apiClient(originalRequest)
    } catch (refreshError) {
      useAuthStore.getState().logout()
      window.location.href = '/login'
      return Promise.reject(refreshError)
    } finally {
      isRefreshing = false
    }
  },
)
