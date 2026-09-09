import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { AgendaPage } from '@/pages/rendezvous/AgendaPage'
import { formatDateISO, getWeekStart } from '@/utils/rdvUtils'
import type { RdvAgendaItem } from '@/types/rendezvous'
import type { TypeService } from '@/types/dossier'

const { agendaState } = vi.hoisted(() => ({
  agendaState: {
    data: [] as RdvAgendaItem[],
    isLoading: false,
  },
}))

vi.mock('@/hooks/useRendezvous', () => ({
  useAgenda: () => agendaState,
}))

vi.mock('@/hooks/useDossiers', () => ({
  useTypesService: () => ({
    data: [
      {
        id: 'ts1',
        code: 'PASSEPORT',
        libelle: 'Passeport',
        delaiJours: 15,
        tarifDefaut: 50,
        checklist: null,
      },
    ] satisfies TypeService[],
    isLoading: false,
  }),
}))

vi.mock('@/components/rendezvous/RdvCreateModal', () => ({
  RdvCreateModal: ({ open }: { open: boolean }) =>
    open ? <div>Create RDV modal open</div> : null,
}))

vi.mock('@/components/rendezvous/RdvDetailDrawer', () => ({
  RdvDetailDrawer: ({ open }: { open: boolean }) =>
    open ? <div>Detail RDV drawer open</div> : null,
}))

function buildRdvForCurrentWeek(): RdvAgendaItem {
  const weekStart = getWeekStart(new Date())
  const dateKey = formatDateISO(weekStart)
  return {
    id: 'rdv-1',
    usagerId: 'u1',
    usagerNom: 'Diallo',
    usagerPrenom: 'Jean',
    typeServiceId: 'ts1',
    typeServiceLibelle: 'Passeport',
    agentId: null,
    agentNom: null,
    dateHeure: `${dateKey}T09:00:00.000Z`,
    dureeMinutes: 30,
    statut: 'CONFIRME',
  }
}

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <AgendaPage />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('AgendaPage', () => {
  beforeEach(() => {
    agendaState.data = []
    agendaState.isLoading = false
  })

  it('renders title and new button', () => {
    renderPage()
    expect(screen.getByRole('heading', { name: /Agenda des rendez-vous/i })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Nouveau rendez-vous/i })).toBeInTheDocument()
  })

  it('shows loading message', () => {
    agendaState.isLoading = true
    renderPage()
    expect(screen.getByText(/Chargement…/i)).toBeInTheDocument()
  })

  it('renders RDV cell for matching UTC slot', () => {
    agendaState.data = [buildRdvForCurrentWeek()]
    renderPage()
    expect(screen.getByText(/Jean D\./)).toBeInTheDocument()
    expect(screen.getByText('Passeport')).toBeInTheDocument()
  })

  it('opens create modal from header button', async () => {
    const user = userEvent.setup()
    renderPage()
    await user.click(screen.getByRole('button', { name: /Nouveau rendez-vous/i }))
    expect(screen.getByText('Create RDV modal open')).toBeInTheDocument()
  })

  it('opens create modal from empty slot', async () => {
    const user = userEvent.setup()
    renderPage()
    const slots = screen.getAllByLabelText(/Ajouter un rendez-vous/i)
    await user.click(slots[0]!)
    expect(screen.getByText('Create RDV modal open')).toBeInTheDocument()
  })
})
