import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { DossierCreatePage } from '@/pages/dossiers/DossierCreatePage'
import type { TypeService } from '@/types/dossier'
import type { UsagerSummary } from '@/types/usager'

const mockUsager = {
  id: 'u1',
  numero: 'GNB-2024-00001',
  nom: 'Camara',
  prenom: 'Mamadou',
  nationalite: 'GNB',
  typeUsager: 'DIASPORA',
  telephone: null,
  email: null,
  actif: true,
} satisfies UsagerSummary

const mockType = {
  id: 'ts1',
  code: 'PASSEPORT',
  libelle: 'Passeport',
  delaiJours: 15,
  tarifDefaut: 50,
  checklist: [{ code: 'ID', libelle: 'Pièce' }],
} satisfies TypeService

const { searchState, createMutate } = vi.hoisted(() => ({
  searchState: { data: [] as UsagerSummary[], isLoading: false },
  createMutate: vi.fn(),
}))

vi.mock('@/hooks/useUsagers', () => ({
  useSearchUsagers: () => searchState,
}))

vi.mock('@/hooks/useDossiers', () => ({
  useTypesService: () => ({ data: [mockType], isLoading: false }),
  useCreateDossier: () => ({ mutate: createMutate, isPending: false }),
}))

vi.mock('@/components/dossiers/ChecklistViewer', () => ({
  ChecklistViewer: () => <div>Checklist stub</div>,
}))

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/dossiers/new']}>
        <Routes>
          <Route path="/dossiers/new" element={<DossierCreatePage />} />
          <Route path="/dossiers/:id" element={<div>Dossier created</div>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('DossierCreatePage', () => {
  beforeEach(() => {
    searchState.data = [mockUsager]
    createMutate.mockReset()
  })

  it('renders wizard header', () => {
    renderPage()
    expect(screen.getByRole('heading', { name: /Nouveau dossier/i })).toBeInTheDocument()
    expect(screen.getByText('Usager')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /Retour à la liste/i })).toHaveAttribute(
      'href',
      '/dossiers',
    )
    expect(screen.getByRole('button', { name: /Suivant/i })).toBeDisabled()
  })

  it('selects usager after search then goes to service step', async () => {
    const user = userEvent.setup()
    renderPage()
    await user.type(screen.getByPlaceholderText(/Rechercher/i), 'Ca')
    expect(await screen.findByText(/Mamadou Camara/)).toBeInTheDocument()
    await user.click(screen.getByText(/Mamadou Camara/))
    await user.click(screen.getByRole('button', { name: /Suivant/i }))
    expect(screen.getByText('Passeport')).toBeInTheDocument()
  })

  it('creates dossier from recap', async () => {
    const user = userEvent.setup()
    createMutate.mockImplementation((_payload, options?: { onSuccess?: (r: unknown) => void }) => {
      options?.onSuccess?.({ data: { data: { id: 'd-new' } } })
    })
    renderPage()
    await user.type(screen.getByPlaceholderText(/Rechercher/i), 'Ca')
    await user.click(await screen.findByText(/Mamadou Camara/))
    await user.click(screen.getByRole('button', { name: /Suivant/i }))
    await user.click(screen.getByText('Passeport'))
    await user.click(screen.getByRole('button', { name: /Suivant/i }))
    expect(screen.getAllByText(/Récapitulatif/i).length).toBeGreaterThanOrEqual(1)
    await user.click(screen.getByRole('button', { name: /Créer le dossier/i }))
    expect(createMutate).toHaveBeenCalledWith(
      { usagerId: 'u1', typeServiceId: 'ts1', notesInternes: undefined },
      expect.any(Object),
    )
    expect(await screen.findByText('Dossier created')).toBeInTheDocument()
  })
})
