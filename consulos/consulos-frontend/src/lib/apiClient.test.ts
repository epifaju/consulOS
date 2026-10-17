import { AxiosHeaders, type InternalAxiosRequestConfig } from 'axios'
import { beforeEach, describe, expect, it } from 'vitest'
import { apiClient } from '@/lib/apiClient'
import { useAuthStore } from '@/stores/authStore'

type RequestFulfilled = (
  config: InternalAxiosRequestConfig,
) => InternalAxiosRequestConfig | Promise<InternalAxiosRequestConfig>

function requestInterceptor(): RequestFulfilled {
  const interceptors = apiClient.interceptors.request as unknown as {
    handlers: Array<{ fulfilled?: RequestFulfilled }>
  }
  const handler = interceptors.handlers.find((item) => item.fulfilled)
  if (!handler?.fulfilled) {
    throw new Error('Request interceptor missing')
  }
  return handler.fulfilled
}

describe('apiClient request interceptor', () => {
  beforeEach(() => {
    useAuthStore.setState({
      accessToken: null,
      user: null,
      isAuthenticated: false,
    })
  })

  it('sets bearer token', async () => {
    useAuthStore.setState({ accessToken: 'access-token' })
    const config = {
      headers: new AxiosHeaders({ 'Content-Type': 'application/json' }),
      data: { ok: true },
    } as InternalAxiosRequestConfig

    const out = await requestInterceptor()(config)

    expect(out.headers.get('Authorization')).toBe('Bearer access-token')
    expect(out.headers.get('Content-Type')).toMatch(/application\/json/i)
  })

  it('clears JSON content-type for FormData so the browser can set the boundary', async () => {
    const form = new FormData()
    form.append('file', new Blob(['x']), 'scan.pdf')
    const headers = new AxiosHeaders()
    headers.set('Content-Type', 'application/json')
    const config = {
      headers,
      data: form,
    } as InternalAxiosRequestConfig

    const out = await requestInterceptor()(config)

    const contentType = out.headers.get('Content-Type')
    expect(contentType === undefined || contentType === null || contentType === false || contentType === '').toBe(
      true,
    )
  })
})
