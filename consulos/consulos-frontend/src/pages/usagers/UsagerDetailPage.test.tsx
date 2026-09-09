import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { UsagerDetailPage } from '@/pages/usagers/UsagerDetailPage'
import type { UsagerDetail } from '@/types/usager'

const mockUsager = {
  id: 'u1',
  numero: 'GNB-2024-00001',
  nom: 'Camara',
  prenom: 'Mamadou',
  nationalite: 'GNB',
  typeUsager: 'DIASPORA',
  telephone: '+33612345678',
  email: 'mamadou.camara@email.com',
  actif: true,
  dateNaissance: '1990-01-15',
  lieuNaissance: 'Bissau',
  adresse: 'Paris',
  numeroDocument: 'P123',
  typeDocument: 'PASSEPORT',
  metadata: {},
  organisationId: 'org-1',
  createdAt: '2024-01-01T00:00:00Z',
  updatedAt: '2024-01-01T00:00:00Z',
} satisfies UsagerDetail

const { usagerState, authState, exportRgpdMutate, exportPdfMutate } = vi.hoisted(() => ({
  usagerState: {
    data: null as UsagerDetail | null | undefined,
    isLoading: false,
  },
  authState: { user: { role: 'ROLE_AGENT' } as { role: string } | null },
  exportRgpdMutate: vi.fn(),
  exportPdfMutate: vi.fn(),
}))

vi.mock('@/stores/authStore', () => ({
  useAuthStore: (selector: (state: typeof authState) => unknown) => selector(authState),
}))

vi.mock('@/hooks/useUsagers', () => ({
  useUsager: () => usagerState,
}))

vi.mock('@/hooks/useUsagerExports', () => ({
  useExportUsagerRgpd: () => ({ mutate: exportRgpdMutate, isPending: false }),
  useExportUsagerFichePdf: () => ({ mutate: exportPdfMutate, isPending: false }),
}))

vi.mock('@/hooks/useUsagerQueries', () => ({
  useUsagerDossiers: () => ({ data: [], isLoading: false }),
  useUsagerDocuments: () => ({ data: [], isLoading: false }),
  useUsagerActivity: () => ({ data: [], isLoading: false }),
}))

vi.mock('@/components/usagers/UsagerDossiersList', () => ({
  UsagerDossiersList: () => <div>Dossiers list stub</div>,
}))

vi.mock('@/components/usagers/UsagerDocumentsList', () => ({
  UsagerDocumentsList: () => <div>Documents list stub</div>,
}))

vi.mock('@/components/usagers/UsagerActivityTimeline', () => ({
  UsagerActivityTimeline: () => <div>Activity timeline stub</div>,
}))

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/usagers/u1']}>
        <Routes>
          <Route path="/usagers/:id" element={<UsagerDetailPage />} />
          <Route path="/usagers" element={<div>Usagers list</div>} />
          <Route path="/usagers/:id/edit" element={<div>Edit usager</div>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('UsagerDetailPage', () => {
  beforeEach(() => {
    authState.user = { role: 'ROLE_AGENT' }
    usagerState.data = mockUsager
    usagerState.isLoading = false
    exportRgpdMutate.mockReset()
    exportPdfMutate.mockReset()
  })

  it('shows loading and not found', () => {
    usagerState.isLoading = true
    usagerState.data = undefined
    const { unmount } = renderPage()
    expect(screen.getByText(/Chargement/i)).toBeInTheDocument()
    unmount()

    usagerState.isLoading = false
    usagerState.data = null
    renderPage()
    expect(screen.getByText(/Usager introuvable/i)).toBeInTheDocument()
  })

  it('renders identity and export/edit actions for agent', async () => {
    const user = userEvent.setup()
    renderPage()
    expect(screen.getByText('GNB-2024-00001')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: /Mamadou Camara/i })).toBeInTheDocument()
    expect(screen.getAllByText('DIASPORA').length).toBeGreaterThanOrEqual(1)
    expect(screen.getByRole('link', { name: /Retour à la liste/i })).toHaveAttribute(
      'href',
      '/usagers',
    )

    await user.click(screen.getByRole('button', { name: /Fiche PDF/i }))
    expect(exportPdfMutate).toHaveBeenCalledWith('u1')
    await user.click(screen.getByRole('button', { name: /Exporter RGPD/i }))
    expect(exportRgpdMutate).toHaveBeenCalledWith('u1')
    expect(screen.getByRole('button', { name: /Modifier/i })).toBeInTheDocument()
  })

  it('hides write actions for caissier', () => {
    authState.user = { role: 'ROLE_CAISSIER' }
    renderPage()
    expect(screen.queryByRole('button', { name: /Modifier/i })).toBeNull()
    expect(screen.queryByRole('button', { name: /Exporter RGPD/i })).toBeNull()
  })
})
