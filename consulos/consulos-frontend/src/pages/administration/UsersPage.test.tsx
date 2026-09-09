import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { UsersPage } from '@/pages/administration/UsersPage'
import type { PageResponse, UserAdmin } from '@/types/admin'

const mockUser = {
  id: 'user-1',
  email: 'agent@consulos.local',
  nom: 'Diallo',
  prenom: 'Awa',
  roleCode: 'ROLE_AGENT',
  roleLibelle: 'Agent',
  actif: true,
  derniereConnexion: null,
  createdAt: '2026-01-01T00:00:00Z',
} satisfies UserAdmin

const { usersState, toggleMutate } = vi.hoisted(() => ({
  usersState: {
    data: null as PageResponse<UserAdmin> | null,
    isLoading: false,
  },
  toggleMutate: vi.fn(),
}))

vi.mock('@/hooks/useAdmin', () => ({
  useAdminUsers: () => usersState,
  useToggleUserActif: () => ({ mutate: toggleMutate, isPending: false }),
}))

vi.mock('@/components/administration/UsersFormModal', () => ({
  UsersFormModal: ({
    open,
    onCreated,
    onOpenChange,
    user,
  }: {
    open: boolean
    onCreated?: (password: string) => void
    onOpenChange: (open: boolean) => void
    user?: UserAdmin | null
  }) =>
    open ? (
      <div>
        <div>{user ? `Editing ${user.email}` : 'Users form modal open'}</div>
        <button
          type="button"
          onClick={() => {
            onCreated?.('TempPass-42')
            onOpenChange(false)
          }}
        >
          Simulate create success
        </button>
      </div>
    ) : null,
}))

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <UsersPage />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('UsersPage', () => {
  beforeEach(() => {
    usersState.data = {
      content: [mockUser],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    }
    toggleMutate.mockReset()
  })

  it('renders agents list', () => {
    renderPage()
    expect(screen.getByText(/Gestion des agents/i)).toBeInTheDocument()
    expect(screen.getByText(/Awa Diallo/)).toBeInTheDocument()
    expect(screen.getByText('agent@consulos.local')).toBeInTheDocument()
    expect(screen.getByText('Agent')).toBeInTheDocument()
    expect(screen.getByText('Actif')).toBeInTheDocument()
  })

  it('opens create modal', async () => {
    const user = userEvent.setup()
    renderPage()
    await user.click(screen.getByRole('button', { name: /Nouvel agent/i }))
    expect(screen.getByText('Users form modal open')).toBeInTheDocument()
  })

  it('opens edit modal for selected agent', async () => {
    const user = userEvent.setup()
    renderPage()
    const edit = document.querySelector('svg.lucide-pencil')?.closest('button')
    expect(edit).toBeTruthy()
    await user.click(edit!)
    expect(screen.getByText('Editing agent@consulos.local')).toBeInTheDocument()
  })

  it('shows temporary password after create', async () => {
    const user = userEvent.setup()
    renderPage()
    await user.click(screen.getByRole('button', { name: /Nouvel agent/i }))
    await user.click(screen.getByRole('button', { name: /Simulate create success/i }))
    expect(screen.getByText(/Mot de passe temporaire/i)).toBeInTheDocument()
    expect(screen.getByText('TempPass-42')).toBeInTheDocument()
  })

  it('toggles user actif', async () => {
    const user = userEvent.setup()
    renderPage()
    const power = document.querySelector('svg.lucide-power')?.closest('button')
    expect(power).toBeTruthy()
    await user.click(power!)
    expect(toggleMutate).toHaveBeenCalledWith('user-1')
  })

  it('paginates when multiple pages', async () => {
    usersState.data = {
      content: [mockUser],
      page: 0,
      size: 20,
      totalElements: 40,
      totalPages: 2,
    }
    const user = userEvent.setup()
    renderPage()
    expect(screen.getByText('1 / 2')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: /Suivant/i }))
    expect(screen.getByText('2 / 2')).toBeInTheDocument()
  })
})
