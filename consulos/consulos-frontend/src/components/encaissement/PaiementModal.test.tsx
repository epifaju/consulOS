import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { PaiementModal } from '@/components/encaissement/PaiementModal'
import type { EncaissementSummary } from '@/types/encaissement'

const { paiementMutate, checkoutMutate, paymentStatusState } = vi.hoisted(() => ({
  paiementMutate: vi.fn(),
  checkoutMutate: vi.fn(),
  paymentStatusState: {
    data: { enabled: false } as { enabled: boolean } | undefined,
    isLoading: false,
  },
}))

vi.mock('@/hooks/useEncaissements', () => ({
  useEnregistrerPaiement: () => ({ mutate: paiementMutate, isPending: false }),
}))

vi.mock('@/hooks/usePayments', () => ({
  usePaymentStatus: () => paymentStatusState,
  usePaymentCheckout: () => ({ mutate: checkoutMutate, isPending: false }),
}))

const encaissement = {
  id: 'e1',
  numeroRecu: 'REC-2024-00001',
  usagerId: 'u1',
  usagerNom: 'Camara',
  usagerPrenom: 'Mamadou',
  dossierId: 'd1',
  dossierReference: 'DOS-001',
  typeServiceLibelle: 'Passeport',
  montantTotal: 50,
  montantPercu: 20,
  devise: 'EUR',
  modePaiement: 'ESPECES',
  statut: 'PARTIEL',
  createdAt: '2024-06-01T10:00:00Z',
} satisfies EncaissementSummary

describe('PaiementModal', () => {
  beforeEach(() => {
    paiementMutate.mockReset()
    checkoutMutate.mockReset()
    paymentStatusState.data = { enabled: false }
  })

  it('hydrates remaining amount and confirms payment', async () => {
    const onOpenChange = vi.fn()
    const onSuccess = vi.fn()
    paiementMutate.mockImplementation((_payload, options?: { onSuccess?: () => void }) => {
      options?.onSuccess?.()
    })
    const user = userEvent.setup()
    render(
      <PaiementModal
        open
        onOpenChange={onOpenChange}
        encaissement={encaissement}
        onSuccess={onSuccess}
      />,
    )

    expect(screen.getByText(/Enregistrer un paiement/i)).toBeInTheDocument()
    expect(screen.getByText(/Reste dû/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/Montant à encaisser/i)).toHaveAttribute('id', 'paiement-montant')
    expect(screen.getByDisplayValue('30')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: /Confirmer le paiement/i }))
    await waitFor(() => {
      expect(paiementMutate).toHaveBeenCalledWith(
        { montant: 30, modePaiement: 'ESPECES', notes: undefined },
        expect.any(Object),
      )
    })
    expect(onOpenChange).toHaveBeenCalledWith(false)
    expect(onSuccess).toHaveBeenCalled()
  })

  it('closes on cancel', async () => {
    const onOpenChange = vi.fn()
    const user = userEvent.setup()
    render(
      <PaiementModal open onOpenChange={onOpenChange} encaissement={encaissement} />,
    )
    await user.click(screen.getByRole('button', { name: /Annuler/i }))
    expect(onOpenChange).toHaveBeenCalledWith(false)
  })

  it('shows online checkout when payments enabled', async () => {
    paymentStatusState.data = { enabled: true }
    const user = userEvent.setup()
    render(
      <PaiementModal open onOpenChange={vi.fn()} encaissement={encaissement} />,
    )
    await user.click(screen.getByRole('button', { name: /Payer en ligne/i }))
    expect(checkoutMutate).toHaveBeenCalledWith('e1', expect.any(Object))
  })

  it('disables confirm when already paid', () => {
    render(
      <PaiementModal
        open
        onOpenChange={vi.fn()}
        encaissement={{ ...encaissement, montantPercu: 50, statut: 'PAYE' }}
      />,
    )
    expect(screen.getByRole('button', { name: /Confirmer le paiement/i })).toBeDisabled()
  })
})
