import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { PaiementEnLignePage } from '@/pages/encaissement/PaiementEnLignePage'
import type { PaymentSession } from '@/services/paymentService'

const pendingSession = {
  id: 'ps-1',
  encaissementId: 'e1',
  provider: 'STRIPE',
  providerSessionId: 'sess_abc',
  status: 'PENDING',
  amount: 50,
  currency: 'EUR',
  checkoutUrl: null,
} satisfies PaymentSession

const { sessionState, completeMutate } = vi.hoisted(() => ({
  sessionState: {
    data: null as PaymentSession | null | undefined,
    isLoading: false,
  },
  completeMutate: vi.fn(),
}))

vi.mock('@/hooks/usePayments', () => ({
  usePaymentSession: () => sessionState,
  useCompletePaymentSession: () => ({ mutate: completeMutate, isPending: false }),
}))

function renderPage(path: string) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[path]}>
        <PaiementEnLignePage />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('PaiementEnLignePage', () => {
  beforeEach(() => {
    sessionState.data = pendingSession
    sessionState.isLoading = false
    completeMutate.mockReset()
  })

  it('shows missing session message', () => {
    renderPage('/caisse/paiement')
    expect(screen.getByText(/Session de paiement manquante/i)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /Retour à la caisse/i })).toHaveAttribute(
      'href',
      '/caisse',
    )
  })

  it('shows loading then not found', () => {
    sessionState.isLoading = true
    sessionState.data = undefined
    const { unmount } = renderPage('/caisse/paiement?session=sess_abc')
    expect(screen.getByText(/Chargement/i)).toBeInTheDocument()
    unmount()

    sessionState.isLoading = false
    sessionState.data = null
    renderPage('/caisse/paiement?session=sess_abc')
    expect(screen.getByText(/Session introuvable/i)).toBeInTheDocument()
  })

  it('confirms pending payment', async () => {
    const user = userEvent.setup()
    renderPage('/caisse/paiement?session=sess_abc')
    expect(screen.getByText(/Paiement en ligne/i)).toBeInTheDocument()
    expect(screen.getByText(/50[,.]00/)).toBeInTheDocument()
    expect(screen.getByText('STRIPE')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: /Simuler le paiement réussi/i }))
    expect(completeMutate).toHaveBeenCalledWith('sess_abc')
  })

  it('shows success when completed', () => {
    sessionState.data = { ...pendingSession, status: 'COMPLETED' }
    renderPage('/caisse/paiement?session=sess_abc')
    expect(screen.getByText(/Paiement enregistré/i)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Simuler le paiement réussi/i })).toBeNull()
  })
})
