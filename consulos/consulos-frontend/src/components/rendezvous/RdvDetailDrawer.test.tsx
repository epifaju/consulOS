import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { RdvDetailDrawer } from '@/components/rendezvous/RdvDetailDrawer'
import type { RdvDetail } from '@/types/rendezvous'

const { detailState, updateMutate } = vi.hoisted(() => ({
  detailState: {
    data: null as RdvDetail | null,
    isLoading: false,
  },
  updateMutate: vi.fn(),
}))

vi.mock('@/hooks/useRendezvous', () => ({
  useRdvDetail: () => detailState,
  useUpdateRdvStatut: () => ({ mutate: updateMutate, isPending: false }),
}))

const confirmed = {
  id: 'rdv-1',
  usagerId: 'u1',
  usagerNom: 'Diallo',
  usagerPrenom: 'Awa',
  usagerEmail: 'awa@example.com',
  dossierId: 'd1',
  dossierReference: 'DOS-001',
  typeServiceId: 'ts1',
  typeServiceLibelle: 'Passeport',
  agentId: 'ag1',
  agentNom: 'Camara',
  dateHeure: '2026-06-15T09:00:00.000Z',
  dureeMinutes: 30,
  statut: 'CONFIRME',
  motif: null,
  notes: 'Accueil',
  rappelEnvoye: true,
  createdAt: '2026-06-01T00:00:00Z',
  updatedAt: '2026-06-01T00:00:00Z',
} satisfies RdvDetail

function renderDrawer(open = true) {
  return render(
    <MemoryRouter>
      <RdvDetailDrawer rdvId="rdv-1" open={open} onOpenChange={vi.fn()} />
    </MemoryRouter>,
  )
}

describe('RdvDetailDrawer', () => {
  beforeEach(() => {
    detailState.data = confirmed
    detailState.isLoading = false
    updateMutate.mockReset()
  })

  it('shows loading state', () => {
    detailState.data = null
    detailState.isLoading = true
    renderDrawer()
    expect(screen.getByText(/Chargement/i)).toBeInTheDocument()
  })

  it('renders detail with links and confirmed actions', () => {
    renderDrawer()
    expect(screen.getByText(/Détail du rendez-vous/i)).toBeInTheDocument()
    expect(screen.getByText('Confirmé')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /Awa Diallo/i })).toHaveAttribute('href', '/usagers/u1')
    expect(screen.getByRole('link', { name: 'DOS-001' })).toHaveAttribute('href', '/dossiers/d1')
    expect(screen.getByText('Passeport')).toBeInTheDocument()
    expect(screen.getByText('Camara')).toBeInTheDocument()
    expect(screen.getByText('Accueil')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /^Présent$/i })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /^Reporter$/i })).toBeInTheDocument()
  })

  it('marks attendance and closes', async () => {
    const onOpenChange = vi.fn()
    updateMutate.mockImplementation((_payload, options?: { onSuccess?: () => void }) => {
      options?.onSuccess?.()
    })
    const user = userEvent.setup()
    render(
      <MemoryRouter>
        <RdvDetailDrawer rdvId="rdv-1" open onOpenChange={onOpenChange} />
      </MemoryRouter>,
    )
    await user.click(screen.getByRole('button', { name: /^Présent$/i }))
    await waitFor(() => {
      expect(updateMutate).toHaveBeenCalledWith({ statut: 'PRESENT' }, expect.any(Object))
    })
    expect(onOpenChange).toHaveBeenCalledWith(false)
  })

  it('asks for a motif before cancelling', async () => {
    const onOpenChange = vi.fn()
    updateMutate.mockImplementation((_payload, options?: { onSuccess?: () => void }) => {
      options?.onSuccess?.()
    })
    const user = userEvent.setup()
    render(
      <MemoryRouter>
        <RdvDetailDrawer rdvId="rdv-1" open onOpenChange={onOpenChange} />
      </MemoryRouter>,
    )
    await user.click(screen.getByRole('button', { name: /^Annuler$/i }))
    expect(updateMutate).not.toHaveBeenCalled()
    await user.type(screen.getByPlaceholderText(/Motif/i), 'Indisponible')
    await user.click(screen.getByRole('button', { name: /^Confirmer$/i }))
    await waitFor(() => {
      expect(updateMutate).toHaveBeenCalledWith(
        { statut: 'ANNULE', motif: 'Indisponible' },
        expect.any(Object),
      )
    })
    expect(onOpenChange).toHaveBeenCalledWith(false)
  })

  it('shows confirm action for pending appointments', () => {
    detailState.data = { ...confirmed, statut: 'EN_ATTENTE' }
    renderDrawer()
    expect(screen.getByRole('button', { name: /^Confirmer$/i })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /^Présent$/i })).not.toBeInTheDocument()
  })

  it('hides status actions for terminal states', () => {
    detailState.data = { ...confirmed, statut: 'PRESENT' }
    renderDrawer()
    expect(screen.queryByRole('button', { name: /^Présent$/i })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /^Annuler$/i })).not.toBeInTheDocument()
  })
})
