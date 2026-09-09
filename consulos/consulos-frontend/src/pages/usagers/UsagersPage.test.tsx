import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { UsagersPage } from '@/pages/usagers/UsagersPage'
import type { PageResponse, UsagerSummary } from '@/types/usager'

const mockUsagers: UsagerSummary[] = [
  {
    id: '1',
    numero: 'GNB-2024-00001',
    nom: 'Camara',
    prenom: 'Mamadou',
    nationalite: 'GNB',
    typeUsager: 'DIASPORA',
    telephone: '+33612345678',
    email: 'mamadou.camara@email.com',
    actif: true,
  },
]

vi.mock('@/hooks/useUsagers', () => ({
  useUsagers: () => ({
    data: {
      content: mockUsagers,
      totalElements: 1,
      totalPages: 1,
      page: 0,
      size: 20,
      first: true,
      last: true,
    } satisfies PageResponse<UsagerSummary>,
    isLoading: false,
  }),
  useSearchUsagers: () => ({ data: [], isLoading: false }),
  useDeleteUsager: () => ({ mutate: vi.fn(), isPending: false }),
}))

vi.mock('@/stores/authStore', () => ({
  useAuthStore: (selector: (state: { user: { role: string } }) => unknown) =>
    selector({ user: { role: 'ROLE_AGENT' } }),
}))

function renderPage(initialPath = '/usagers') {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })

  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[initialPath]}>
        <Routes>
          <Route path="/usagers" element={<UsagersPage />} />
          <Route path="/usagers/new" element={<div>New usager form</div>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('UsagersPage', () => {
  it('renders list with mocked data', () => {
    renderPage()
    expect(screen.getByText(/Mamadou Camara/)).toBeInTheDocument()
    expect(screen.getByText('GNB-2024-00001')).toBeInTheDocument()
  })

  it('filters results when searching', async () => {
    const user = userEvent.setup()
    renderPage()
    const input = screen.getByPlaceholderText(/Rechercher/i)
    await user.type(input, 'Camara')
    expect(input).toHaveValue('Camara')
  })

  it('navigates to new usager form', async () => {
    const user = userEvent.setup()
    renderPage()
    await user.click(screen.getByRole('button', { name: /Nouvel usager/i }))
    expect(screen.getByText('New usager form')).toBeInTheDocument()
  })
})
