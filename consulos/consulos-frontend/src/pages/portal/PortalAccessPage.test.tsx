import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { PortalAccessPage } from '@/pages/portal/PortalAccessPage'

const { mutateMock } = vi.hoisted(() => ({
  mutateMock: vi.fn(),
}))

vi.mock('@/hooks/usePortal', () => ({
  useRequestPortalAccess: () => ({ mutate: mutateMock, isPending: false }),
}))

function renderPage() {
  return render(
    <MemoryRouter>
      <PortalAccessPage />
    </MemoryRouter>,
  )
}

describe('PortalAccessPage', () => {
  beforeEach(() => {
    mutateMock.mockReset()
  })

  it('renders portal access form', () => {
    renderPage()
    expect(screen.getByText(/Portail usager/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/Référence dossier/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/^Email$/i)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Recevoir un lien d'accès/i })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /Espace agents/i })).toHaveAttribute('href', '/login')
  })

  it('does not mutate when reference is too short', async () => {
    const user = userEvent.setup()
    renderPage()
    await user.type(screen.getByLabelText(/Référence dossier/i), 'DOS')
    await user.type(screen.getByLabelText(/^Email$/i), 'usager@example.com')
    await user.click(screen.getByRole('button', { name: /Recevoir un lien d'accès/i }))
    expect(mutateMock).not.toHaveBeenCalled()
  })

  it('shows success when mutate succeeds', async () => {
    mutateMock.mockImplementation((_values, options?: { onSuccess?: () => void }) => {
      options?.onSuccess?.()
    })
    const user = userEvent.setup()
    renderPage()
    await user.type(screen.getByLabelText(/Référence dossier/i), 'DOS-2026-00001')
    await user.type(screen.getByLabelText(/^Email$/i), 'usager@example.com')
    await user.click(screen.getByRole('button', { name: /Recevoir un lien d'accès/i }))

    expect(await screen.findByText(/Vérifiez votre boîte mail/i)).toBeInTheDocument()
  })

  it('shows generic error when mutate fails', async () => {
    mutateMock.mockImplementation((_values, options?: { onError?: () => void }) => {
      options?.onError?.()
    })
    const user = userEvent.setup()
    renderPage()
    await user.type(screen.getByLabelText(/Référence dossier/i), 'DOS-2026-00001')
    await user.type(screen.getByLabelText(/^Email$/i), 'usager@example.com')
    await user.click(screen.getByRole('button', { name: /Recevoir un lien d'accès/i }))

    await waitFor(() => {
      expect(screen.getByText(/Impossible d'envoyer la demande/i)).toBeInTheDocument()
    })
  })
})
