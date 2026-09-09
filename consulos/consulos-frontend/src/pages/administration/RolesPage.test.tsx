import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { RolesPage } from '@/pages/administration/RolesPage'
import type { Role } from '@/types/admin'

vi.mock('@/hooks/useAdmin', () => ({
  useAdminRoles: () => ({
    data: [
      {
        id: 'r1',
        code: 'ROLE_AGENT',
        libelle: 'Agent',
        permissions: ['USAGER_READ', 'DOSSIER_WRITE'],
      },
      {
        id: 'r2',
        code: 'ROLE_ADMIN',
        libelle: 'Administrateur',
        permissions: ['ADMIN_FULL'],
      },
    ] satisfies Role[],
    isLoading: false,
  }),
}))

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <RolesPage />
    </QueryClientProvider>,
  )
}

describe('RolesPage', () => {
  it('renders roles and permissions readonly', () => {
    renderPage()
    expect(screen.getByText(/Rôles et permissions/i)).toBeInTheDocument()
    expect(screen.getByText(/lecture seule/i)).toBeInTheDocument()
    expect(screen.getByText('Agent')).toBeInTheDocument()
    expect(screen.getByText('ROLE_AGENT')).toBeInTheDocument()
    expect(screen.getByText('USAGER_READ')).toBeInTheDocument()
    expect(screen.getByText('Administrateur')).toBeInTheDocument()
    expect(screen.getByText('ADMIN_FULL')).toBeInTheDocument()
  })
})
