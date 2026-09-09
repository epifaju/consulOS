import { AxiosError, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import { describe, expect, it } from 'vitest'
import { resolveApiErrorMessage, shouldToastApiError } from '@/lib/apiErrors'
import type { ApiResponse } from '@/types/api'

function axiosError(
  status: number | undefined,
  data: Partial<ApiResponse<unknown>> | undefined,
  url = '/api/dossiers',
): AxiosError<ApiResponse<unknown>> {
  const config = { url } as InternalAxiosRequestConfig
  const response =
    status === undefined
      ? undefined
      : ({
          status,
          data,
          statusText: 'Error',
          headers: {},
          config,
        } as AxiosResponse<ApiResponse<unknown>>)
  return new AxiosError('fail', undefined, config, undefined, response)
}

describe('resolveApiErrorMessage', () => {
  it('returns unexpected for non-axios errors', () => {
    expect(resolveApiErrorMessage(new Error('x'))).toMatch(/inattendue|unexpected/i)
  })

  it('prefers server message for 403 / ACCESS_DENIED', () => {
    expect(
      resolveApiErrorMessage(
        axiosError(403, { success: false, message: 'Pas le droit', errorCode: 'ACCESS_DENIED' }),
      ),
    ).toBe('Pas le droit')
    expect(resolveApiErrorMessage(axiosError(403, { success: false }))).toMatch(/refus|denied/i)
  })

  it('handles 401 and rate limit', () => {
    expect(resolveApiErrorMessage(axiosError(401, { success: false }))).toMatch(/Session|expired/i)
    expect(
      resolveApiErrorMessage(
        axiosError(429, { success: false, errorCode: 'RATE_LIMIT_EXCEEDED' }),
      ),
    ).toMatch(/Trop|Too many|Demasiad/i)
  })

  it('returns data.message when present otherwise unexpected', () => {
    expect(
      resolveApiErrorMessage(axiosError(500, { success: false, message: 'Boom métier' })),
    ).toBe('Boom métier')
    expect(resolveApiErrorMessage(axiosError(500, { success: false }))).toMatch(
      /inattendue|unexpected/i,
    )
  })
})

describe('shouldToastApiError', () => {
  it('is false for non-axios and silent auth paths', () => {
    expect(shouldToastApiError(new Error('x'))).toBe(false)
    expect(shouldToastApiError(axiosError(403, {}, '/auth/login'))).toBe(false)
    expect(shouldToastApiError(axiosError(403, {}, '/auth/refresh'))).toBe(false)
  })

  it('toasts only 403 / 422 / 409 on non-auth urls', () => {
    expect(shouldToastApiError(axiosError(403, {}, '/api/usagers'))).toBe(true)
    expect(shouldToastApiError(axiosError(422, {}, '/api/usagers'))).toBe(true)
    expect(shouldToastApiError(axiosError(409, {}, '/api/usagers'))).toBe(true)
    expect(shouldToastApiError(axiosError(500, {}, '/api/usagers'))).toBe(false)
  })
})
