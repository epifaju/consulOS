import axios, { type AxiosError } from 'axios'
import i18n from '@/lib/i18n'
import type { ApiResponse } from '@/types/api'

type ApiErrorBody = ApiResponse<unknown>

export function resolveApiErrorMessage(error: unknown): string {
  if (!isAxiosError(error)) {
    return i18n.t('errors.unexpected', { ns: 'common' })
  }

  const data = error.response?.data
  const status = error.response?.status
  const errorCode = data?.errorCode ?? null

  if (status === 403 || errorCode === 'ACCESS_DENIED') {
    return data?.message ?? i18n.t('errors.accessDenied', { ns: 'common' })
  }

  if (status === 401 || errorCode === 'INVALID_CREDENTIALS') {
    return data?.message ?? i18n.t('errors.unauthorized', { ns: 'common' })
  }

  if (errorCode === 'RATE_LIMIT_EXCEEDED') {
    return data?.message ?? i18n.t('errors.rateLimit', { ns: 'common' })
  }

  if (data?.message) {
    return data.message
  }

  return i18n.t('errors.unexpected', { ns: 'common' })
}

export function shouldToastApiError(error: unknown): boolean {
  if (!isAxiosError(error)) {
    return false
  }
  const status = error.response?.status
  const url = error.config?.url ?? ''
  if (AUTH_SILENT_PATHS.some((path) => url.includes(path))) {
    return false
  }
  return status === 403 || status === 422 || status === 409
}

const AUTH_SILENT_PATHS = ['/auth/login', '/auth/refresh', '/auth/logout']

function isAxiosError(error: unknown): error is AxiosError<ApiErrorBody> {
  return axios.isAxiosError(error)
}
