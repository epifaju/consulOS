import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { AxiosError } from 'axios'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { LoginPage } from '@/pages/auth/LoginPage'
import { authService } from '@/services/authService'
import type { ApiResponse } from '@/types/api'
import type { LoginResponse } from '@/types/auth'
import type { UserProfile } from '@/types/user'

const mockLogin = vi.fn()

const mockUser = {
  id: '11111111-1111-1111-1111-111111111111',
  email: 'agent@consulos.local',
  nom: 'Diallo',
  prenom: 'Awa',
  role: 'ROLE_AGENT',
  organisationId: '22222222-2222-2222-2222-222222222222',
  organisationCode: 'GNB-FR',
} satisfies UserProfile

vi.mock('@/stores/authStore', () => ({
  useAuthStore: (selector: (state: { login: typeof mockLogin }) => unknown) =>
    selector({ login: mockLogin }),
}))

vi.mock('@/services/authService', () => ({
  authService: {
    login: vi.fn(),
  },
}))

function renderLogin(initialPath = '/login') {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[initialPath]}>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/" element={<div>Home dashboard</div>} />
          <Route path="/caisse" element={<div>Caisse landing</div>} />
          <Route path="/administration" element={<div>Admin landing</div>} />
          <Route path="/dossiers" element={<div>Dossiers landing</div>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

function loginApiResponse(user: UserProfile): { data: ApiResponse<LoginResponse> } {
  return {
    data: {
      success: true,
      data: { accessToken: 'access-token', user },
      message: null,
      errorCode: null,
      timestamp: '2026-09-09T10:00:00Z',
    },
  }
}

async function fillAndSubmit(email: string, password: string) {
  const user = userEvent.setup()
  await user.type(screen.getByLabelText(/Adresse e-mail/i), email)
  await user.type(screen.getByLabelText(/^Mot de passe$/i), password)
  await user.click(screen.getByRole('button', { name: /Se connecter/i }))
  return user
}

describe('LoginPage', () => {
  beforeEach(() => {
    mockLogin.mockReset()
    vi.mocked(authService.login).mockReset()
  })

  it('renders brand, fields and links', () => {
    renderLogin()
    expect(screen.getByText('ConsulOS')).toBeInTheDocument()
    expect(screen.getByText(/Bienvenue sur ConsulOS/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/Adresse e-mail/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/^Mot de passe$/i)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Se connecter/i })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /Mot de passe oublié/i })).toHaveAttribute(
      'href',
      '/forgot-password',
    )
    expect(screen.getByRole('link', { name: /Accès usager/i })).toHaveAttribute('href', '/portal')
  })

  it('does not call authService when password is too short', async () => {
    renderLogin()
    await fillAndSubmit('agent@consulos.local', 'short')
    expect(authService.login).not.toHaveBeenCalled()
  })

  it('logs in and navigates to home for ROLE_AGENT', async () => {
    vi.mocked(authService.login).mockResolvedValue(loginApiResponse(mockUser))
    renderLogin()
    await fillAndSubmit('agent@consulos.local', 'Password1')

    await waitFor(() => {
      expect(mockLogin).toHaveBeenCalledWith('access-token', expect.objectContaining({
        email: 'agent@consulos.local',
        role: 'ROLE_AGENT',
      }))
    })
    expect(await screen.findByText('Home dashboard')).toBeInTheDocument()
  })

  it('redirects caissier to /caisse', async () => {
    vi.mocked(authService.login).mockResolvedValue(
      loginApiResponse({ ...mockUser, role: 'ROLE_CAISSIER' }),
    )
    renderLogin()
    await fillAndSubmit('caissier@consulos.local', 'Password1')
    expect(await screen.findByText('Caisse landing')).toBeInTheDocument()
  })

  it('honors returnUrl over role default', async () => {
    vi.mocked(authService.login).mockResolvedValue(loginApiResponse(mockUser))
    renderLogin('/login?returnUrl=%2Fdossiers')
    await fillAndSubmit('agent@consulos.local', 'Password1')
    expect(await screen.findByText('Dossiers landing')).toBeInTheDocument()
  })

  it('shows alert on 401 invalid credentials', async () => {
    const error = new AxiosError<ApiResponse<null>>('Unauthorized')
    error.response = {
      status: 401,
      data: {
        success: false,
        data: null,
        message: null,
        errorCode: null,
        timestamp: '2026-09-09T10:00:00Z',
      },
      statusText: 'Unauthorized',
      headers: {},
      config: { headers: {} } as never,
    }
    vi.mocked(authService.login).mockRejectedValue(error)

    renderLogin()
    await fillAndSubmit('agent@consulos.local', 'Password1')

    expect(await screen.findByRole('alert')).toHaveTextContent(/Email ou mot de passe incorrect/i)
  })

  it('shows service unavailable on 502', async () => {
    const error = new AxiosError<ApiResponse<null>>('Bad Gateway')
    error.response = {
      status: 502,
      data: {
        success: false,
        data: null,
        message: null,
        errorCode: null,
        timestamp: '2026-10-17T10:00:00Z',
      },
      statusText: 'Bad Gateway',
      headers: {},
      config: { headers: {} } as never,
    }
    vi.mocked(authService.login).mockRejectedValue(error)
    renderLogin()
    await fillAndSubmit('agent@consulos.local', 'Password1')
    expect(await screen.findByRole('alert')).toHaveTextContent(/Service indisponible/i)
  })

  it('shows service unavailable on timeout', async () => {
    const error = new AxiosError<ApiResponse<null>>('timeout')
    error.code = 'ECONNABORTED'
    vi.mocked(authService.login).mockRejectedValue(error)
    renderLogin()
    await fillAndSubmit('agent@consulos.local', 'Password1')
    expect(await screen.findByRole('alert')).toHaveTextContent(/Service indisponible/i)
  })
})
