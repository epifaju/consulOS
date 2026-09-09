import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ReportingPage } from '@/pages/reporting/ReportingPage'
import type { ReportingMensuel } from '@/types/reporting'

const mockReport = {
  annee: 2026,
  mois: 9,
  dateDebut: '2026-09-01',
  dateFin: '2026-09-30',
  dossiersCrees: 12,
  dossiersParStatut: [{ statut: 'VALIDE', count: 5 }],
  encaissements: {
    montantPercuTotal: 1250,
    nbTransactions: 8,
    parMode: [{ mode: 'ESPECES', count: 3, montant: 400 }],
    parService: [{ typeService: 'Passeport', montantTotal: 850, nbTransactions: 5 }],
  },
  rendezVous: {
    total: 20,
    parStatut: [{ statut: 'CONFIRME', count: 15 }],
  },
  documentsDeposes: 30,
  delaiMoyenHeures: 48,
} satisfies ReportingMensuel

const { authState, reportingState, exportExcelMutate } = vi.hoisted(() => ({
  authState: { user: { role: 'ROLE_SUPERVISEUR' } as { role: string } | null },
  reportingState: {
    data: null as ReportingMensuel | null,
    isLoading: false,
    isFetching: false,
    refetch: vi.fn(),
  },
  exportExcelMutate: vi.fn(),
}))

vi.mock('@/stores/authStore', () => ({
  useAuthStore: (selector: (state: typeof authState) => unknown) => selector(authState),
}))

vi.mock('@/hooks/useReporting', () => ({
  useReportingMensuel: () => reportingState,
  useExportReportingExcel: () => ({ mutate: exportExcelMutate, isPending: false }),
  useExportReportingDossiersCsv: () => ({ mutate: vi.fn(), isPending: false }),
  useExportReportingRdvCsv: () => ({ mutate: vi.fn(), isPending: false }),
}))

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <ReportingPage />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('ReportingPage', () => {
  beforeEach(() => {
    authState.user = { role: 'ROLE_SUPERVISEUR' }
    reportingState.data = mockReport
    reportingState.isLoading = false
    exportExcelMutate.mockReset()
  })

  it('forbids non-supervision roles', () => {
    authState.user = { role: 'ROLE_AGENT' }
    renderPage()
    expect(
      screen.getByText(/Reporting réservé aux superviseurs et administrateurs/i),
    ).toBeInTheDocument()
  })

  it('shows loading state', () => {
    reportingState.isLoading = true
    reportingState.data = null
    renderPage()
    expect(screen.getByText(/Chargement du reporting/i)).toBeInTheDocument()
  })

  it('renders KPIs from mocked report', () => {
    renderPage()
    expect(screen.getByRole('heading', { name: /Reporting mensuel/i })).toBeInTheDocument()
    expect(screen.getByText(/Dossiers créés/i)).toBeInTheDocument()
    expect(screen.getByText('12')).toBeInTheDocument()
    expect(screen.getByText(/1[\s\u202f]?250[,.]00/)).toBeInTheDocument()
  })

  it('triggers excel export', async () => {
    const user = userEvent.setup()
    renderPage()
    await user.click(screen.getByRole('button', { name: /Export Excel/i }))
    expect(exportExcelMutate).toHaveBeenCalledWith(
      expect.objectContaining({ annee: expect.any(Number), mois: expect.any(Number) }),
    )
  })
})
