import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { AppShell } from '@/components/layout/AppShell'
import type { UserProfile } from '@/types/user'

const { authState, logout } = vi.hoisted(() => {
  const logout = vi.fn()
  const authState = {
    user: null as UserProfile | null,
    logout,
  }
  return { authState, logout }
})

vi.mock('@/stores/authStore', () => ({
  useAuthStore: (selector: (s: typeof authState) => unknown) => selector(authState),
}))

vi.mock('@/components/notifications/NotificationBell', () => ({
  NotificationBell: () => <div data-testid="notification-bell" />,
}))

const adminUser = {
  id: 'u1',
  email: 'admin@consulos.local',
  nom: 'Diallo',
  prenom: 'Awa',
  role: 'ROLE_ADMIN',
  organisationId: 'org-1',
} satisfies UserProfile

const agentUser = {
  ...adminUser,
  role: 'ROLE_AGENT',
  prenom: 'Mamadou',
  nom: 'Camara',
} satisfies UserProfile

function renderShell(path = '/') {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route element={<AppShell />}>
          <Route path="*" element={<div>Page content</div>} />
        </Route>
      </Routes>
    </MemoryRouter>,
  )
}

describe('AppShell', () => {
  beforeEach(() => {
    authState.user = adminUser
    logout.mockReset()
  })

  it('renders brand, outlet and admin nav', () => {
    renderShell('/usagers')
    expect(screen.getAllByText(/ConsulOS/i).length).toBeGreaterThanOrEqual(1)
    expect(screen.getByText('Page content')).toBeInTheDocument()
    expect(screen.getByTestId('notification-bell')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /Administration/i })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /Caisse/i })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /^Usagers$/i })).toBeInTheDocument()
  })

  it('hides admin and caisse for agent role', () => {
    authState.user = agentUser
    renderShell()
    expect(screen.queryByRole('link', { name: /Administration/i })).not.toBeInTheDocument()
    expect(screen.queryByRole('link', { name: /Caisse/i })).not.toBeInTheDocument()
    expect(screen.getByRole('link', { name: /Usagers/i })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /Dossiers/i })).toBeInTheDocument()
  })

  it('logs out from user menu', async () => {
    const user = userEvent.setup()
    renderShell()
    await user.click(screen.getByRole('button', { name: /Awa Diallo/i }))
    await user.click(screen.getByRole('menuitem', { name: /Déconnexion/i }))
    expect(logout).toHaveBeenCalled()
  })
})
