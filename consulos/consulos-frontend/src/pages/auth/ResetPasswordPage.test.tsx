import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { AxiosError } from 'axios'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ResetPasswordPage } from '@/pages/auth/ResetPasswordPage'
import { authService } from '@/services/authService'
import type { ApiResponse } from '@/types/api'

vi.mock('@/services/authService', () => ({
  authService: {
    resetPassword: vi.fn(),
  },
}))

function renderPage(path = '/reset-password?token=valid-token') {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[path]}>
        <ResetPasswordPage />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('ResetPasswordPage', () => {
  beforeEach(() => {
    vi.mocked(authService.resetPassword).mockReset()
  })

  it('shows missing token error without query param', () => {
    renderPage('/reset-password')
    expect(screen.getByRole('alert')).toHaveTextContent(/Lien invalide ou incomplet/i)
    expect(screen.getByRole('link', { name: /Demander un nouveau lien/i })).toHaveAttribute(
      'href',
      '/forgot-password',
    )
  })

  it('does not call service for weak password', async () => {
    const user = userEvent.setup()
    renderPage()
    await user.type(screen.getByLabelText(/Nouveau mot de passe/i), 'weak')
    await user.type(screen.getByLabelText(/Confirmer le mot de passe/i), 'weak')
    await user.click(screen.getByRole('button', { name: /^Enregistrer$/i }))
    expect(authService.resetPassword).not.toHaveBeenCalled()
  })

  it('shows success after valid reset', async () => {
    vi.mocked(authService.resetPassword).mockResolvedValue({ data: {} } as never)
    const user = userEvent.setup()
    renderPage()
    await user.type(screen.getByLabelText(/Nouveau mot de passe/i), 'Password1')
    await user.type(screen.getByLabelText(/Confirmer le mot de passe/i), 'Password1')
    await user.click(screen.getByRole('button', { name: /^Enregistrer$/i }))

    expect(await screen.findByText(/Mot de passe mis à jour/i)).toBeInTheDocument()
  })

  it('shows invalid token api error', async () => {
    const error = new AxiosError<ApiResponse<null>>('bad token')
    error.response = {
      status: 400,
      data: {
        success: false,
        data: null,
        message: null,
        errorCode: 'INVALID_RESET_TOKEN',
        timestamp: '2026-09-09T10:00:00Z',
      },
      statusText: 'Bad Request',
      headers: {},
      config: { headers: {} } as never,
    }
    vi.mocked(authService.resetPassword).mockRejectedValue(error)

    const user = userEvent.setup()
    renderPage()
    await user.type(screen.getByLabelText(/Nouveau mot de passe/i), 'Password1')
    await user.type(screen.getByLabelText(/Confirmer le mot de passe/i), 'Password1')
    await user.click(screen.getByRole('button', { name: /^Enregistrer$/i }))

    await waitFor(() => {
      expect(screen.getByRole('alert')).toHaveTextContent(/expiré ou a déjà été utilisé/i)
    })
  })
})
