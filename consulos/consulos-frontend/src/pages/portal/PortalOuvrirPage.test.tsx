import { render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { PortalOuvrirPage } from '@/pages/portal/PortalOuvrirPage'

const { mutateMock } = vi.hoisted(() => ({
  mutateMock: vi.fn(),
}))

vi.mock('@/hooks/usePortal', () => ({
  useOpenPortalSession: () => ({ mutate: mutateMock, isPending: false }),
}))

function renderPage(path: string) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path="/portal/ouvrir" element={<PortalOuvrirPage />} />
        <Route path="/portal/dossier" element={<div>Portal dossier</div>} />
        <Route path="/portal" element={<div>Portal access</div>} />
      </Routes>
    </MemoryRouter>,
  )
}

describe('PortalOuvrirPage', () => {
  beforeEach(() => {
    mutateMock.mockReset()
  })

  it('shows error when token is missing', () => {
    renderPage('/portal/ouvrir')
    expect(screen.getByText(/Lien invalide ou expiré/i)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /Recevoir un lien d'accès/i })).toHaveAttribute(
      'href',
      '/portal',
    )
    expect(mutateMock).not.toHaveBeenCalled()
  })

  it('shows loading while opening session', () => {
    mutateMock.mockImplementation(() => undefined)
    renderPage('/portal/ouvrir?token=abc')
    expect(screen.getByText(/Ouverture de votre espace/i)).toBeInTheDocument()
    expect(mutateMock).toHaveBeenCalledWith('abc', expect.any(Object))
  })

  it('navigates to portal dossier on success', async () => {
    mutateMock.mockImplementation((_token, options?: { onSuccess?: () => void }) => {
      options?.onSuccess?.()
    })
    renderPage('/portal/ouvrir?token=abc')
    expect(await screen.findByText('Portal dossier')).toBeInTheDocument()
  })

  it('shows error when mutate fails', async () => {
    mutateMock.mockImplementation((_token, options?: { onError?: () => void }) => {
      options?.onError?.()
    })
    renderPage('/portal/ouvrir?token=bad')
    await waitFor(() => {
      expect(screen.getByText(/Lien invalide ou expiré/i)).toBeInTheDocument()
    })
  })
})
