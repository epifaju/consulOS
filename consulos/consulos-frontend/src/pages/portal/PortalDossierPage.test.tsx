import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { PortalDossierPage } from '@/pages/portal/PortalDossierPage'
import type { PortalDossierSummary } from '@/stores/portalStore'

const dossier = {
  id: 'd1',
  reference: 'DOS-2026-00001',
  statut: 'SOUMIS',
  typeServiceLibelle: 'Passeport',
  dateSoumission: '2026-09-01',
  dateEcheance: null,
  usagerPrenom: 'Awa',
  usagerNom: 'Diallo',
} satisfies PortalDossierSummary

const { portalState, clearSession, uploadMutate, createRdvMutate, docsState, slotsState } =
  vi.hoisted(() => ({
    portalState: {
      sessionToken: 'tok' as string | null,
      dossier: null as PortalDossierSummary | null,
    },
    clearSession: vi.fn(),
    uploadMutate: vi.fn(),
    createRdvMutate: vi.fn(),
    docsState: { data: [] as { id: string; nomOriginal: string; typeDocument: string; statutControle: string; tailleOctets: number }[], isLoading: false },
    slotsState: {
      data: [] as { heureDebut: string; heureFin: string; placesRestantes: number; quotaMax: number }[],
      isLoading: false,
    },
  }))

vi.mock('@/stores/portalStore', () => ({
  usePortalStore: (selector: (s: {
    sessionToken: string | null
    dossier: PortalDossierSummary | null
    clearSession: typeof clearSession
  }) => unknown) =>
    selector({
      sessionToken: portalState.sessionToken,
      dossier: portalState.dossier,
      clearSession,
    }),
}))

vi.mock('@/hooks/usePortal', () => ({
  usePortalDossier: () => ({ data: portalState.dossier, isLoading: false }),
  usePortalDocuments: () => docsState,
  usePortalUploadDocument: () => ({ mutate: uploadMutate, isPending: false }),
  usePortalDisponibilites: () => slotsState,
  usePortalCreateRdv: () => ({ mutate: createRdvMutate, isPending: false }),
}))

vi.mock('@/hooks/use-toast', () => ({
  toast: vi.fn(),
}))

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/portal/dossier']}>
        <Routes>
          <Route path="/portal/dossier" element={<PortalDossierPage />} />
          <Route path="/portal" element={<div>Portal access</div>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('PortalDossierPage', () => {
  beforeEach(() => {
    portalState.sessionToken = 'tok'
    portalState.dossier = dossier
    docsState.data = []
    slotsState.data = []
    clearSession.mockReset()
    uploadMutate.mockReset()
    createRdvMutate.mockReset()
  })

  it('redirects when no session token', () => {
    portalState.sessionToken = null
    renderPage()
    expect(screen.getByText('Portal access')).toBeInTheDocument()
  })

  it('renders shell and dossier summary', () => {
    renderPage()
    expect(screen.getByText(/Mon dossier/i)).toBeInTheDocument()
    expect(screen.getByText('DOS-2026-00001')).toBeInTheDocument()
    expect(screen.getByText(/Awa Diallo/)).toBeInTheDocument()
    expect(screen.getByText('SOUMIS')).toBeInTheDocument()
    expect(screen.getByText('Passeport')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Quitter/i })).toBeInTheDocument()
  })

  it('shows empty documents and empty rdv slots', async () => {
    const user = userEvent.setup()
    renderPage()
    expect(screen.getByText(/Aucun document déposé/i)).toBeInTheDocument()
    await user.click(screen.getByRole('tab', { name: /Prendre rendez-vous/i }))
    expect(screen.getByText(/Aucun créneau ce jour/i)).toBeInTheDocument()
  })

  it('books an available slot', async () => {
    slotsState.data = [
      { heureDebut: '09:00:00', heureFin: '09:30:00', placesRestantes: 2, quotaMax: 5 },
    ]
    const user = userEvent.setup()
    renderPage()
    await user.click(screen.getByRole('tab', { name: /Prendre rendez-vous/i }))
    await user.click(screen.getByRole('button', { name: /09:00/i }))
    expect(createRdvMutate).toHaveBeenCalledWith(
      expect.objectContaining({ dateHeure: expect.stringMatching(/T09:00:00Z$/) }),
      expect.any(Object),
    )
  })
})
