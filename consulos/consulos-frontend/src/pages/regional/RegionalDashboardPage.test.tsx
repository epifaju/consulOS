import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { RegionalDashboardPage } from '@/pages/regional/RegionalDashboardPage'
import type { RegionalPosteKpi, RegionalStats } from '@/services/regionalService'
import type { ReseauMe } from '@/services/reseauService'

const mockStats = {
  reseauId: 'reseau-1',
  postesCount: 2,
  dossiersOuverts: 40,
  enRevue: 5,
  rdvJour: 8,
  dossiersEnRetard: 3,
  recettesJour: 860,
  recettesPeriode: 12500,
} satisfies RegionalStats

const mockPoste = {
  organisationId: 'org-poste-1',
  code: 'GNB-FR',
  nom: 'Paris',
  dossiersOuverts: 20,
  enRevue: 2,
  rdvJour: 4,
  recettesJour: 400,
} satisfies RegionalPosteKpi

const mockMe = {
  reseaux: [
    {
      id: 'reseau-1',
      code: 'EU-WEST',
      nom: 'Europe Ouest',
      description: null,
      roleReseau: 'ROLE_ADMIN_RESEAU',
      postes: [
        {
          organisationId: 'org-poste-1',
          code: 'GNB-FR',
          nom: 'Paris',
          paysCode: 'FR',
          actif: true,
        },
      ],
    },
  ],
} satisfies ReseauMe

const { authState, statusState, meState, statsState, postesState } = vi.hoisted(() => ({
  authState: { user: { role: 'ROLE_ADMIN_RESEAU' } as { role: string } | null },
  statusState: { data: { enabled: true } as { enabled: boolean } | undefined, isLoading: false },
  meState: { data: null as ReseauMe | null, isLoading: false },
  statsState: { data: null as RegionalStats | null, isLoading: false },
  postesState: { data: [] as RegionalPosteKpi[], isLoading: false },
}))

vi.mock('@/stores/authStore', () => ({
  useAuthStore: (selector: (state: typeof authState) => unknown) => selector(authState),
}))

vi.mock('@/hooks/useReseau', () => ({
  useReseauStatus: () => statusState,
  useMyReseaux: () => meState,
}))

vi.mock('@/hooks/useRegional', () => ({
  useRegionalStats: () => statsState,
  useRegionalPostes: () => postesState,
  useRegionalPosteStats: () => ({ data: null, isLoading: false }),
}))

vi.mock('@/components/regional/RegionalReportingPanel', () => ({
  RegionalReportingPanel: () => <div>Reporting panel</div>,
}))

vi.mock('@/components/regional/RegionalAlertsPanel', () => ({
  RegionalAlertsPanel: () => <div>Alerts panel</div>,
}))

vi.mock('@/components/regional/RegionalAdminPanel', () => ({
  RegionalAdminPanel: () => <div>Admin panel</div>,
}))

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <RegionalDashboardPage />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('RegionalDashboardPage', () => {
  beforeEach(() => {
    authState.user = { role: 'ROLE_ADMIN_RESEAU' }
    statusState.data = { enabled: true }
    statusState.isLoading = false
    meState.data = mockMe
    meState.isLoading = false
    statsState.data = mockStats
    statsState.isLoading = false
    postesState.data = [mockPoste]
    postesState.isLoading = false
  })

  it('forbids roles without reseau access', () => {
    authState.user = { role: 'ROLE_AGENT' }
    renderPage()
    expect(screen.getByText(/Accès réservé aux profils réseau/i)).toBeInTheDocument()
  })

  it('shows disabled module message', () => {
    statusState.data = { enabled: false }
    renderPage()
    expect(screen.getByText(/Module réseau désactivé/i)).toBeInTheDocument()
  })

  it('shows empty membership', () => {
    meState.data = { reseaux: [] }
    renderPage()
    expect(screen.getByText(/Aucun réseau rattaché/i)).toBeInTheDocument()
  })

  it('renders KPIs and postes table', async () => {
    const user = userEvent.setup()
    renderPage()
    expect(screen.getByRole('heading', { name: /Tableau de bord régional/i })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'EU-WEST' })).toBeInTheDocument()
    expect(screen.getByText('40')).toBeInTheDocument()
    expect(screen.getByText('GNB-FR')).toBeInTheDocument()
    expect(screen.getByText('Paris')).toBeInTheDocument()
    expect(screen.getByText(/Dossiers en retard/i)).toBeInTheDocument()

    await user.click(screen.getByText('Paris'))
    expect(screen.getByText(/Détail du poste/i)).toBeInTheDocument()
  })
})
