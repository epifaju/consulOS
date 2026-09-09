import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ParametresPage } from '@/pages/administration/ParametresPage'
import type { Organisation } from '@/types/admin'
import type { Tarif } from '@/types/encaissement'
import type { ConfigCreneau, JourFermeture } from '@/services/parametresService'

const mockOrg = {
  id: 'org-1',
  code: 'GNB-FR',
  nom: 'Consulat Paris',
  paysCode: 'FRA',
  langueDefaut: 'fr',
  deviseDefaut: 'EUR',
  actif: true,
} satisfies Organisation

const { orgState, updateMutate, tarifsState } = vi.hoisted(() => ({
  orgState: { data: null as Organisation | null },
  updateMutate: vi.fn(),
  tarifsState: { data: [] as Tarif[], isLoading: false },
}))

vi.mock('@/hooks/useAdmin', () => ({
  useOrganisation: () => orgState,
  useUpdateOrganisation: () => ({ mutate: updateMutate, isPending: false }),
}))

vi.mock('@/hooks/useEncaissements', () => ({
  useTarifs: () => tarifsState,
}))

vi.mock('@/services/parametresService', () => ({
  parametresService: {
    listCreneaux: vi.fn(async () => ({
      data: {
        data: [
          {
            id: 'c1',
            typeServiceId: null,
            jourSemaine: 1,
            heureDebut: '09:00',
            heureFin: '12:00',
            dureeMinutes: 30,
            quotaMax: 10,
            actif: true,
          },
        ] satisfies ConfigCreneau[],
      },
    })),
    listJoursFermeture: vi.fn(async () => ({
      data: {
        data: [
          {
            id: 'f1',
            dateFermeture: '2026-12-25',
            motif: 'Noël',
          },
        ] satisfies JourFermeture[],
      },
    })),
  },
}))

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <ParametresPage />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('ParametresPage', () => {
  beforeEach(() => {
    orgState.data = mockOrg
    updateMutate.mockReset()
    tarifsState.data = [
      {
        id: 't1',
        typeServiceId: 'ts1',
        libelle: 'Passeport',
        montant: 86,
        devise: 'EUR',
        actif: true,
      },
    ]
  })

  it('renders title and general form hydrated', async () => {
    renderPage()
    expect(screen.getByText(/Paramètres du poste/i)).toBeInTheDocument()
    await waitFor(() => {
      expect(screen.getByDisplayValue('Consulat Paris')).toBeInTheDocument()
      expect(screen.getByDisplayValue('FRA')).toBeInTheDocument()
    })
  })

  it('saves organisation updates', async () => {
    const user = userEvent.setup()
    renderPage()
    await waitFor(() => expect(screen.getByDisplayValue('Consulat Paris')).toBeInTheDocument())
    const nom = screen.getByDisplayValue('Consulat Paris')
    await user.clear(nom)
    await user.type(nom, 'Consulat Lyon')
    await user.click(screen.getByRole('button', { name: /Enregistrer/i }))
    expect(updateMutate).toHaveBeenCalledWith(
      expect.objectContaining({ nom: 'Consulat Lyon', paysCode: 'FRA' }),
    )
  })

  it('shows horaires and tarifs tabs', async () => {
    const user = userEvent.setup()
    renderPage()
    await user.click(screen.getByRole('tab', { name: /Horaires/i }))
    expect(await screen.findByText(/J1 : 09:00–12:00/)).toBeInTheDocument()
    expect(screen.getByText(/2026-12-25 — Noël/)).toBeInTheDocument()

    await user.click(screen.getByRole('tab', { name: /Tarifs/i }))
    expect(await screen.findByText(/Passeport — 86 EUR/)).toBeInTheDocument()
  })
})
