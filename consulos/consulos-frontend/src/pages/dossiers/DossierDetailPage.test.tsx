import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { DossierDetailPage } from '@/pages/dossiers/DossierDetailPage'
import type { DossierDetail } from '@/types/dossier'

const mockDossier = {
  id: 'd1',
  reference: 'DOS-2024-00042',
  usagerId: 'u1',
  usagerNom: 'Camara',
  usagerPrenom: 'Mamadou',
  typeServiceId: 'ts1',
  typeServiceCode: 'PASSEPORT',
  typeServiceLibelle: 'Passeport',
  statut: 'SOUMIS',
  agentId: 'a1',
  agentNom: 'Agent Test',
  priorite: 'NORMALE',
  dateSoumission: '2024-06-01T10:00:00Z',
  dateEcheance: '2024-07-01T10:00:00Z',
  motifRejet: null,
  notesInternes: 'Note interne',
  metadata: null,
  checklist: [{ code: 'ID', libelle: 'Pièce d’identité' }],
  commentaires: [
    {
      id: 'c1',
      auteurId: 'a1',
      auteurNom: 'Agent Test',
      contenu: 'Premier commentaire',
      createdAt: '2024-06-02T10:00:00Z',
    },
  ],
  createdAt: '2024-06-01T09:00:00Z',
  updatedAt: '2024-06-01T10:00:00Z',
} satisfies DossierDetail

const { dossierState, authState, transitionMutate, commentaireMutate } = vi.hoisted(() => ({
  dossierState: {
    data: null as DossierDetail | null | undefined,
    isLoading: false,
  },
  authState: { user: { role: 'ROLE_AGENT' } as { role: string } | null },
  transitionMutate: vi.fn(),
  commentaireMutate: vi.fn(),
}))

vi.mock('@/stores/authStore', () => ({
  useAuthStore: (selector: (state: typeof authState) => unknown) => selector(authState),
}))

vi.mock('@/hooks/useDossiers', () => ({
  useDossier: () => dossierState,
  useDossierHistorique: () => ({ data: [], isLoading: false }),
  useDossierTransition: () => ({ mutate: transitionMutate, isPending: false }),
  useAddDossierCommentaire: () => ({ mutate: commentaireMutate, isPending: false }),
}))

vi.mock('@/components/dossiers/AssignAgentPanel', () => ({
  AssignAgentPanel: () => <div>Assign agent panel</div>,
}))

vi.mock('@/components/dossiers/ChecklistViewer', () => ({
  ChecklistViewer: () => <div>Checklist stub</div>,
}))

vi.mock('@/components/dossiers/WorkflowTimeline', () => ({
  WorkflowTimeline: () => <div>Workflow timeline stub</div>,
}))

vi.mock('@/components/documents/DocumentList', () => ({
  DocumentList: () => <div>Document list stub</div>,
}))

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/dossiers/d1']}>
        <Routes>
          <Route path="/dossiers/:id" element={<DossierDetailPage />} />
          <Route path="/dossiers" element={<div>Dossiers list</div>} />
          <Route path="/usagers/:id" element={<div>Usager detail</div>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('DossierDetailPage', () => {
  beforeEach(() => {
    authState.user = { role: 'ROLE_AGENT' }
    dossierState.data = { ...mockDossier, statut: 'SOUMIS' }
    dossierState.isLoading = false
    transitionMutate.mockReset()
    commentaireMutate.mockReset()
  })

  it('shows loading and not found', () => {
    dossierState.isLoading = true
    dossierState.data = undefined
    const { unmount } = renderPage()
    expect(screen.getByText(/Chargement/i)).toBeInTheDocument()
    unmount()

    dossierState.isLoading = false
    dossierState.data = null
    renderPage()
    expect(screen.getByText(/Dossier introuvable/i)).toBeInTheDocument()
  })

  it('renders header and agent transition actions for SOUMIS', async () => {
    const user = userEvent.setup()
    renderPage()
    expect(screen.getByText('DOS-2024-00042')).toBeInTheDocument()
    expect(screen.getByText('Soumis')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /Mamadou Camara/i })).toHaveAttribute(
      'href',
      '/usagers/u1',
    )
    expect(screen.getByText(/Assign agent panel/i)).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: /Mettre en revue/i }))
    expect(transitionMutate).toHaveBeenCalledWith({ statut: 'EN_REVUE' })
  })

  it('requires motif for incomplet transition', async () => {
    const user = userEvent.setup()
    renderPage()
    await user.click(screen.getByRole('button', { name: /Marquer incomplet/i }))
    expect(screen.getByPlaceholderText(/Motif \(obligatoire\)/i)).toBeInTheDocument()
    expect(transitionMutate).not.toHaveBeenCalled()

    await user.type(screen.getByPlaceholderText(/Motif \(obligatoire\)/i), 'Pièces manquantes')
    await user.click(screen.getByRole('button', { name: /^Confirmer$/i }))
    expect(transitionMutate).toHaveBeenCalledWith(
      { statut: 'INCOMPLET', motif: 'Pièces manquantes' },
      expect.any(Object),
    )
  })

  it('shows validate/reject for superviseur on EN_REVUE', () => {
    authState.user = { role: 'ROLE_SUPERVISEUR' }
    dossierState.data = { ...mockDossier, statut: 'EN_REVUE' }
    renderPage()
    expect(screen.getByRole('button', { name: /Valider/i })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Rejeter/i })).toBeInTheDocument()
  })

  it('adds commentaire', async () => {
    const user = userEvent.setup()
    renderPage()
    await user.click(screen.getByRole('tab', { name: /Commentaires/i }))
    expect(screen.getByText('Premier commentaire')).toBeInTheDocument()
    await user.type(screen.getByPlaceholderText(/commentaire/i), 'Nouveau suivi')
    await user.click(screen.getByRole('button', { name: /Publier/i }))
    expect(commentaireMutate).toHaveBeenCalledWith('Nouveau suivi', expect.any(Object))
  })
})
