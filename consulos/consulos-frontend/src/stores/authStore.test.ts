import { beforeEach, describe, expect, it } from 'vitest'
import { useAuthStore } from '@/stores/authStore'
import type { UserProfile } from '@/types/user'

const user = {
  id: '11111111-1111-1111-1111-111111111111',
  email: 'agent@consulos.local',
  nom: 'Diallo',
  prenom: 'Awa',
  role: 'ROLE_AGENT',
  organisationId: '22222222-2222-2222-2222-222222222222',
} satisfies UserProfile

describe('authStore', () => {
  beforeEach(() => {
    useAuthStore.setState({
      user: null,
      accessToken: null,
      isAuthenticated: false,
    })
  })

  it('login sets token, user and authenticated flag', () => {
    useAuthStore.getState().login('access-token', user)

    const state = useAuthStore.getState()
    expect(state.accessToken).toBe('access-token')
    expect(state.user).toEqual(user)
    expect(state.isAuthenticated).toBe(true)
  })

  it('logout clears session', () => {
    useAuthStore.getState().login('access-token', user)
    useAuthStore.getState().logout()

    const state = useAuthStore.getState()
    expect(state.accessToken).toBeNull()
    expect(state.user).toBeNull()
    expect(state.isAuthenticated).toBe(false)
  })

  it('updateToken refreshes access token and keeps authenticated', () => {
    useAuthStore.getState().login('old', user)
    useAuthStore.getState().updateToken('new-token')

    expect(useAuthStore.getState().accessToken).toBe('new-token')
    expect(useAuthStore.getState().isAuthenticated).toBe(true)
    expect(useAuthStore.getState().user).toEqual(user)
  })
})
