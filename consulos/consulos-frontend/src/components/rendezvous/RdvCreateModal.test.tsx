import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { RdvCreateModal } from '@/components/rendezvous/RdvCreateModal'
import type { TypeService } from '@/types/dossier'
import type { Creneau } from '@/types/rendezvous'
import type { UsagerSummary } from '@/types/usager'

const TYPE_ID = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'

const mockType = {
  id: TYPE_ID,
  code: 'PASSEPORT',
  libelle: 'Passeport',
  delaiJours: 15,
  tarifDefaut: 50,
  checklist: [{ code: 'ID', libelle: 'Pièce' }],
} satisfies TypeService

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

const { slotsState, searchState, createMutate } = vi.hoisted(() => ({
  slotsState: {
    data: [] as Creneau[],
    isLoading: false,
  },
  searchState: {
    data: [] as UsagerSummary[],
    isLoading: false,
  },
  createMutate: vi.fn(),
}))

vi.mock('@/hooks/useRendezvous', () => ({
  useDisponibilites: () => slotsState,
  useCreateRdv: () => ({ mutate: createMutate, isPending: false }),
}))

vi.mock('@/hooks/useUsagers', () => ({
  useSearchUsagers: () => searchState,
}))

vi.mock('@/hooks/useDossiers', () => ({
  useTypesService: () => ({ data: [mockType], isLoading: false }),
}))

describe('RdvCreateModal', () => {
  beforeEach(() => {
    slotsState.data = [
      { heureDebut: '09:00:00', heureFin: '09:30:00', placesRestantes: 2, quotaMax: 4 },
    ]
    slotsState.isLoading = false
    searchState.data = [mockUsager]
    createMutate.mockReset()
  })

  it('keeps next disabled until a slot is chosen', () => {
    render(<RdvCreateModal open onOpenChange={vi.fn()} />)
    expect(screen.getByText(/Nouveau rendez-vous/i)).toBeInTheDocument()
    expect(screen.getByText(/Date & créneau/i)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Suivant/i })).toBeDisabled()
  })

  it('shows empty slots message', () => {
    slotsState.data = []
    render(<RdvCreateModal open onOpenChange={vi.fn()} />)
    expect(screen.getByText(/Aucun créneau disponible/i)).toBeInTheDocument()
  })

  it('creates an appointment through the three steps', async () => {
    const onOpenChange = vi.fn()
    createMutate.mockImplementation((_payload, options?: { onSuccess?: () => void }) => {
      options?.onSuccess?.()
    })
    const user = userEvent.setup()
    render(
      <RdvCreateModal
        open
        onOpenChange={onOpenChange}
        initialDate={new Date('2026-06-15T00:00:00Z')}
        initialTime=""
      />,
    )

    await user.click(screen.getByRole('button', { name: /09:00/ }))
    await user.click(screen.getByRole('button', { name: /Suivant/i }))

    expect(screen.getByText(/Usager/i)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Suivant/i })).toBeDisabled()
    await user.type(screen.getByPlaceholderText(/Rechercher un usager/i), 'Ca')
    await user.click(await screen.findByRole('button', { name: /Mamadou Camara — GNB-2024-00001/i }))
    await user.click(screen.getByRole('button', { name: /Suivant/i }))

    expect(screen.getByText(/Détails/i)).toBeInTheDocument()
    expect(screen.getByText(/2026-06-15 09:00/)).toBeInTheDocument()
    expect(screen.getByText(/Mamadou Camara/)).toBeInTheDocument()
    await user.type(screen.getByPlaceholderText(/^Notes$/i), 'Bureau 2')
    await user.click(screen.getByRole('button', { name: /^Créer$/i }))

    await waitFor(() => {
      expect(createMutate).toHaveBeenCalledWith(
        {
          usagerId: 'u1',
          dateHeure: '2026-06-15T09:00:00Z',
          typeServiceId: undefined,
          notes: 'Bureau 2',
        },
        expect.any(Object),
      )
    })
    expect(onOpenChange).toHaveBeenCalledWith(false)
  })

  it('goes back to the date step from usager search', async () => {
    const user = userEvent.setup()
    render(<RdvCreateModal open onOpenChange={vi.fn()} />)
    await user.click(screen.getByRole('button', { name: /09:00/ }))
    await user.click(screen.getByRole('button', { name: /Suivant/i }))
    await user.click(screen.getByRole('button', { name: /Précédent/i }))
    expect(screen.getByText(/Date & créneau/i)).toBeInTheDocument()
  })
})
