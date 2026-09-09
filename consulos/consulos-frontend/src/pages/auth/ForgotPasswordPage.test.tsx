import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ForgotPasswordPage } from '@/pages/auth/ForgotPasswordPage'
import { authService } from '@/services/authService'

vi.mock('@/services/authService', () => ({
  authService: {
    forgotPassword: vi.fn(),
  },
}))

function renderPage() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <ForgotPasswordPage />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('ForgotPasswordPage', () => {
  beforeEach(() => {
    vi.mocked(authService.forgotPassword).mockReset()
  })

  it('renders title, email field and submit', () => {
    renderPage()
    expect(screen.getByText(/Mot de passe oublié/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/Adresse e-mail/i)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Envoyer le lien/i })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /Retour à la connexion/i })).toHaveAttribute(
      'href',
      '/login',
    )
  })

  it('does not call service for invalid email', async () => {
    const user = userEvent.setup()
    renderPage()
    await user.type(screen.getByLabelText(/Adresse e-mail/i), 'not-an-email')
    await user.click(screen.getByRole('button', { name: /Envoyer le lien/i }))
    expect(authService.forgotPassword).not.toHaveBeenCalled()
  })

  it('shows success and MailHog hint after submit', async () => {
    vi.mocked(authService.forgotPassword).mockResolvedValue({ data: {} } as never)
    const user = userEvent.setup()
    renderPage()
    await user.type(screen.getByLabelText(/Adresse e-mail/i), 'agent@consulos.local')
    await user.click(screen.getByRole('button', { name: /Envoyer le lien/i }))

    expect(
      await screen.findByText(/Si un compte existe pour cette adresse/i),
    ).toBeInTheDocument()
    expect(screen.getByText(/MailHog/i)).toBeInTheDocument()
  })

  it('shows generic error on failure', async () => {
    vi.mocked(authService.forgotPassword).mockRejectedValue(new Error('fail'))
    const user = userEvent.setup()
    renderPage()
    await user.type(screen.getByLabelText(/Adresse e-mail/i), 'agent@consulos.local')
    await user.click(screen.getByRole('button', { name: /Envoyer le lien/i }))

    await waitFor(() => {
      expect(screen.getByRole('alert')).toHaveTextContent(/Impossible d'envoyer l'e-mail/i)
    })
  })
})
