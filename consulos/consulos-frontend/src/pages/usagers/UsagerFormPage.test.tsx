import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { UsagerFormPage } from '@/pages/usagers/UsagerFormPage'
import type { UsagerDetail } from '@/types/usager'

const mockUsager = {
  id: 'u1',
  numero: 'GNB-2024-00001',
  nom: 'Camara',
  prenom: 'Mamadou',
  nationalite: 'GNB',
  typeUsager: 'DIASPORA',
  telephone: null,
  email: 'm@example.com',
  actif: true,
  dateNaissance: '1990-01-01',
  lieuNaissance: 'Bissau',
  adresse: null,
  numeroDocument: null,
  typeDocument: null,
  metadata: {},
  organisationId: 'org-1',
  createdAt: '2024-01-01T00:00:00Z',
  updatedAt: '2024-01-01T00:00:00Z',
} satisfies UsagerDetail

const { usagerState, createMutate, updateMutate } = vi.hoisted(() => ({
  usagerState: {
    data: null as UsagerDetail | null | undefined,
    isLoading: false,
  },
  createMutate: vi.fn(),
  updateMutate: vi.fn(),
}))

vi.mock('@/hooks/useUsagers', () => ({
  useUsager: () => usagerState,
  useCreateUsager: () => ({ mutate: createMutate, isPending: false }),
  useUpdateUsager: () => ({ mutate: updateMutate, isPending: false }),
  useDoublons: () => ({ data: [], isLoading: false }),
}))

vi.mock('@/hooks/useOcr', () => ({
  useOcrSuggestions: () => ({ data: null, isLoading: false }),
}))

vi.mock('@/components/usagers/DoublonAlert', () => ({
  DoublonAlert: () => null,
}))

function renderForm(path: string) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[path]}>
        <Routes>
          <Route path="/usagers/new" element={<UsagerFormPage />} />
          <Route path="/usagers/:id" element={<UsagerFormPage />} />
          <Route path="/usagers/:id/view" element={<div>Usager view</div>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('UsagerFormPage', () => {
  beforeEach(() => {
    usagerState.data = undefined
    usagerState.isLoading = false
    createMutate.mockReset()
    updateMutate.mockReset()
  })

  it('renders create form sections', () => {
    renderForm('/usagers/new')
    expect(screen.getByRole('heading', { name: /Nouvel usager/i })).toBeInTheDocument()
    expect(screen.getByText(/Identité/i)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Enregistrer/i })).toBeInTheDocument()
  })

  it('does not create when nom/prenom empty', async () => {
    const user = userEvent.setup()
    renderForm('/usagers/new')
    await user.click(screen.getByRole('button', { name: /Enregistrer/i }))
    expect(createMutate).not.toHaveBeenCalled()
  })

  it('creates usager and navigates', async () => {
    createMutate.mockImplementation((_payload, options?: { onSuccess?: (r: unknown) => void }) => {
      options?.onSuccess?.({ data: { data: { usager: { id: 'u-new' } } } })
    })
    const user = userEvent.setup()
    renderForm('/usagers/new')
    await user.type(screen.getByLabelText(/^Nom$/i), 'Diallo')
    await user.type(screen.getByLabelText(/^Prénom$/i), 'Awa')
    await user.click(screen.getByRole('button', { name: /Enregistrer/i }))
    expect(createMutate).toHaveBeenCalled()
  })

  it('loads edit mode from usager', async () => {
    usagerState.data = mockUsager
    renderForm('/usagers/u1')
    expect(await screen.findByRole('heading', { name: /Modifier l'usager/i })).toBeInTheDocument()
    await waitFor(() => {
      expect(screen.getByDisplayValue('Camara')).toBeInTheDocument()
      expect(screen.getByDisplayValue('Mamadou')).toBeInTheDocument()
    })
  })
})
