import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { CaissePage } from '@/pages/encaissement/CaissePage'
import type { DossierSummary } from '@/types/dossier'
import type { EncaissementSummary, JournalCaisse, PageResponse, Tarif } from '@/types/encaissement'
import type { UsagerSummary } from '@/types/usager'

const { encaissementsState, mockEncaissement, emptyPage } = vi.hoisted(() => {
  function emptyPage<T>(content: T[] = []): PageResponse<T> {
    return {
      content,
      page: 0,
      size: 20,
      totalElements: content.length,
      totalPages: 1,
    }
  }

  const mockEncaissement = {
    id: 'e1',
    numeroRecu: 'REC-2024-00001',
    usagerId: 'u1',
    usagerNom: 'Camara',
    usagerPrenom: 'Mamadou',
    dossierId: 'd1',
    dossierReference: 'DOS-001',
    typeServiceLibelle: 'Passeport',
    montantTotal: 50,
    montantPercu: 0,
    devise: 'EUR',
    modePaiement: 'ESPECES',
    statut: 'DU',
    createdAt: '2024-06-01T10:00:00Z',
  } satisfies EncaissementSummary

  return {
    emptyPage,
    mockEncaissement,
    encaissementsState: {
      data: emptyPage([mockEncaissement]) as PageResponse<EncaissementSummary>,
      isLoading: false,
    },
  }
})

vi.mock('@/hooks/useEncaissements', () => ({
  useEncaissements: () => encaissementsState,
  useTarifs: () => ({ data: [] as Tarif[], isLoading: false }),
  useCreateEncaissement: () => ({ mutate: vi.fn(), isPending: false }),
  useJournalCaisse: () => ({
    data: {
      date: '2024-06-01',
      parMode: [],
      nombreTransactions: 0,
      montantTotalPercu: 0,
    } satisfies JournalCaisse,
    isLoading: false,
  }),
  useEnregistrerPaiement: () => ({ mutate: vi.fn(), isPending: false }),
}))

vi.mock('@/hooks/useEncaissementExports', () => ({
  useExportJournalExcel: () => ({ mutate: vi.fn(), isPending: false }),
}))

vi.mock('@/components/encaissement/JournalCaissePanel', () => ({
  JournalCaissePanel: () => <div>Journal de caisse</div>,
}))

vi.mock('@/hooks/usePayments', () => ({
  usePaymentStatus: () => ({ data: { enabled: false }, isLoading: false }),
  usePaymentCheckout: () => ({ mutate: vi.fn(), isPending: false }),
}))

vi.mock('@/hooks/useUsagers', () => ({
  useSearchUsagers: () => ({ data: [] as UsagerSummary[], isLoading: false }),
}))

vi.mock('@/hooks/useDossiers', () => ({
  useDossiers: () => ({ data: emptyPage<DossierSummary>(), isLoading: false }),
}))

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <Routes>
          <Route path="/" element={<CaissePage />} />
          <Route path="/caisse/recu/:id" element={<div>Recu page</div>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('CaissePage', () => {
  beforeEach(() => {
    encaissementsState.data = emptyPage([mockEncaissement])
    encaissementsState.isLoading = false
  })

  it('renders list with mocked encaissement', () => {
    renderPage()
    expect(screen.getByRole('heading', { name: /Caisse consulaire/i })).toBeInTheDocument()
    expect(screen.getByText('REC-2024-00001')).toBeInTheDocument()
    expect(screen.getByText(/Mamadou Camara/)).toBeInTheDocument()
    expect(screen.getByText('Dû')).toBeInTheDocument()
    expect(screen.getByText(/50[,.]00/)).toBeInTheDocument()
    expect(screen.getByText(/Nouvel encaissement/i)).toBeInTheDocument()
    expect(screen.getByText(/Journal de caisse/i)).toBeInTheDocument()
  })

  it('shows empty state when no encaissements', () => {
    encaissementsState.data = emptyPage()
    renderPage()
    expect(screen.getByText(/Aucun encaissement pour cette date/i)).toBeInTheDocument()
  })

  it('exposes date filter and usager search', () => {
    renderPage()
    expect(document.querySelector('input[type="date"]')).toBeTruthy()
    expect(
      screen.getByPlaceholderText(/Rechercher un usager \(min\. 2 caractères\)/i),
    ).toBeInTheDocument()
  })

  it('links to receipt page', () => {
    renderPage()
    const recu = screen.getByTitle(/Voir le reçu/i)
    expect(recu).toHaveAttribute('href', '/caisse/recu/e1')
  })

  it('opens paiement dialog for DU encaissement', async () => {
    renderPage()
    fireEvent.click(screen.getByRole('button', { name: /Enregistrer paiement/i }))
    expect(await screen.findByRole('heading', { name: /Enregistrer un paiement/i })).toBeInTheDocument()
  })
})
