import { beforeEach, describe, expect, it, vi } from 'vitest'
import { authService } from '@/services/authService'

const { get, post } = vi.hoisted(() => ({
  get: vi.fn(),
  post: vi.fn(),
}))

vi.mock('@/lib/apiClient', () => ({
  apiClient: { get, post },
}))

describe('authService', () => {
  beforeEach(() => {
    get.mockReset()
    post.mockReset()
    get.mockResolvedValue({ data: { success: true, data: null } })
    post.mockResolvedValue({ data: { success: true, data: null } })
  })

  it('posts login credentials', async () => {
    await authService.login({ email: 'admin@consulos.local', password: 'Secret1!' })

    expect(post).toHaveBeenCalledWith('/auth/login', {
      email: 'admin@consulos.local',
      password: 'Secret1!',
    })
  })

  it('posts refresh without body', async () => {
    await authService.refresh()

    expect(post).toHaveBeenCalledWith('/auth/refresh')
  })

  it('posts logout and forgot-password email', async () => {
    await authService.logout()
    await authService.forgotPassword('admin@consulos.local')

    expect(post).toHaveBeenCalledWith('/auth/logout')
    expect(post).toHaveBeenCalledWith('/auth/forgot-password', { email: 'admin@consulos.local' })
  })

  it('posts reset-password payload and gets current profile', async () => {
    await authService.resetPassword({
      token: 'tok-1',
      newPassword: 'NewSecret1!',
      confirmPassword: 'NewSecret1!',
    })
    await authService.me()

    expect(post).toHaveBeenCalledWith('/auth/reset-password', {
      token: 'tok-1',
      newPassword: 'NewSecret1!',
      confirmPassword: 'NewSecret1!',
    })
    expect(get).toHaveBeenCalledWith('/auth/me')
  })
})
