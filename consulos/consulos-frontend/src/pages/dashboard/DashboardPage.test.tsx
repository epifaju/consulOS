import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { DashboardPage } from '@/pages/dashboard/DashboardPage'
import type { Alerte, DashboardStats } from '@/types/dashboard'

const mockStats: DashboardStats = {
  dossiersOuverts: { label: 'Dossiers', valeur: 42, valeurPrecedente: 38, tendancePourcent: 10.5 },
  enAttenteValidation: { label: 'Attente', valeur: 8, valeurPrecedente: 6, tendancePourcent: 33.3 },
  rdvJour: { label: 'RDV', valeur: 12, valeurPrecedente: 10, tendancePourcent: 20 },
  recettesJour: { label: 'Recettes', valeur: 860, valeurPrecedente: 720, tendancePourcent: 19.4 },
  dossierStats: {
    totalOuverts: 42,
    totalEnCours: 15,
    totalValides: 20,
    totalRejetes: 2,
    delaiMoyenHeures: 48,
    dossiersEnRetard: 3,
  },
  rdvStats: { total: 12, confirmes: 10, presents: 8, absents: 2, tauxPresence: 80 },
  recettesTotalPeriode: 12500,
}

const mockAlertes: Alerte[] = [
  {
    type: 'ECHEANCES_DEPASSEES',
    severite: 'critical',
    message: 'Dossiers en retard',
    count: 3,
    dossiersIds: ['a', 'b'],
  },
]

vi.mock('@/hooks/useDashboard', () => ({
  useDashboardStats: () => ({ data: mockStats, isLoading: false }),
  useDossiersChart: () => ({ data: [] }),
  useRecettesChart: () => ({ data: [] }),
  useDashboardAlerts: () => ({ data: mockAlertes }),
  useAgentsCharge: () => ({ data: [] }),
}))

vi.mock('@/stores/authStore', () => ({
  useAuthStore: (selector: (state: { user: { role: string } }) => unknown) =>
    selector({ user: { role: 'ROLE_SUPERVISEUR' } }),
}))

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <DashboardPage />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('DashboardPage', () => {
  it('renders 4 KPI cards with mocked data', () => {
    renderPage()
    expect(screen.getByText('42')).toBeInTheDocument()
    expect(screen.getByText('8')).toBeInTheDocument()
    expect(screen.getByText('12')).toBeInTheDocument()
    expect(screen.getByText('860,00 €')).toBeInTheDocument()
  })

  it('displays mocked alerts', () => {
    renderPage()
    expect(screen.getByText(/Dossiers en retard/)).toBeInTheDocument()
    expect(screen.getByText(/3 élément/)).toBeInTheDocument()
  })

  it('alert link points to dossiers with filter', async () => {
    renderPage()
    const link = screen.getByRole('link', { name: /Voir les 3 dossiers/i })
    expect(link).toHaveAttribute('href', '/dossiers?statut=EN_REVUE,VALIDE,SOUMIS')
    await userEvent.click(link)
  })
})
