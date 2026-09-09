import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ProtectedRoute } from '@/router/ProtectedRoute'
import type { UserProfile } from '@/types/user'

const authState = vi.hoisted(() => ({
  isAuthenticated: false,
  user: null as UserProfile | null,
}))

vi.mock('@/stores/authStore', () => ({
  useAuthStore: (selector: (state: typeof authState) => unknown) => selector(authState),
}))

function renderProtected(roles?: string[], initialPath = '/admin') {
  return render(
    <MemoryRouter initialEntries={[initialPath]}>
      <Routes>
        <Route element={<ProtectedRoute roles={roles} />}>
          <Route path="/admin" element={<div>Protected content</div>} />
        </Route>
        <Route path="/login" element={<div>Login page</div>} />
      </Routes>
    </MemoryRouter>,
  )
}

describe('ProtectedRoute', () => {
  beforeEach(() => {
    authState.isAuthenticated = false
    authState.user = null
  })

  it('redirects anonymous users to login with returnUrl', () => {
    renderProtected(undefined, '/admin')
    expect(screen.getByText('Login page')).toBeInTheDocument()
  })

  it('renders outlet when authenticated without role restriction', () => {
    authState.isAuthenticated = true
    authState.user = {
      id: 'u1',
      email: 'agent@consulos.local',
      nom: 'Diallo',
      prenom: 'Awa',
      role: 'ROLE_AGENT',
      organisationId: 'org-1',
    }
    renderProtected()
    expect(screen.getByText('Protected content')).toBeInTheDocument()
  })

  it('shows forbidden card when role is not allowed', () => {
    authState.isAuthenticated = true
    authState.user = {
      id: 'u1',
      email: 'agent@consulos.local',
      nom: 'Diallo',
      prenom: 'Awa',
      role: 'ROLE_AGENT',
      organisationId: 'org-1',
    }
    renderProtected(['ROLE_ADMIN'])
    expect(screen.getByText(/Accès refusé/i)).toBeInTheDocument()
    expect(screen.getByText(/droits nécessaires/i)).toBeInTheDocument()
  })

  it('allows matching role', () => {
    authState.isAuthenticated = true
    authState.user = {
      id: 'u1',
      email: 'admin@consulos.local',
      nom: 'Admin',
      prenom: 'Sys',
      role: 'ROLE_ADMIN',
      organisationId: 'org-1',
    }
    renderProtected(['ROLE_ADMIN'])
    expect(screen.getByText('Protected content')).toBeInTheDocument()
  })
})
