import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { DoublonAlert } from '@/components/usagers/DoublonAlert'
import type { DoublonGroup } from '@/types/usager'

const doublons = [
  {
    motif: 'Nom + date de naissance',
    similaires: [
      {
        id: 'u2',
        numero: 'GNB-2023-00009',
        nom: 'Camara',
        prenom: 'Mamadou',
        nationalite: 'GNB',
        typeUsager: 'DIASPORA',
        telephone: null,
        email: null,
        actif: true,
      },
    ],
  },
] satisfies DoublonGroup[]

describe('DoublonAlert', () => {
  it('renders nothing when empty', () => {
    const { container } = render(
      <MemoryRouter>
        <DoublonAlert doublons={[]} onIgnore={vi.fn()} />
      </MemoryRouter>,
    )
    expect(container).toBeEmptyDOMElement()
  })

  it('renders groups with links and ignore action', async () => {
    const onIgnore = vi.fn()
    const user = userEvent.setup()
    render(
      <MemoryRouter>
        <DoublonAlert doublons={doublons} onIgnore={onIgnore} />
      </MemoryRouter>,
    )
    expect(screen.getByText(/Doublons potentiels/i)).toBeInTheDocument()
    expect(screen.getByText(/Nom \+ date de naissance/i)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /GNB-2023-00009 — Mamadou Camara/i })).toHaveAttribute(
      'href',
      '/usagers/u2',
    )
    await user.click(screen.getByRole('button', { name: /Ignorer/i }))
    expect(onIgnore).toHaveBeenCalled()
  })
})
