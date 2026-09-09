import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { DossiersPage } from '@/pages/dossiers/DossiersPage'
import type { DossierSummary, PageResponse, TypeService } from '@/types/dossier'

const { dossiersState, mockDossier, pageOf } = vi.hoisted(() => {
  function pageOf(content: DossierSummary[]): PageResponse<DossierSummary> {
    return {
      content,
      page: 0,
      size: 20,
      totalElements: content.length,
      totalPages: 1,
    }
  }

  const mockDossier = {
    id: 'd1',
    reference: 'DOS-2024-00042',
    usagerId: 'u1',
    usagerNom: 'Camara',
    usagerPrenom: 'Mamadou',
    typeServiceLibelle: 'Passeport',
    statut: 'SOUMIS',
    agentId: 'a1',
    agentNom: 'Agent Test',
    priorite: 'NORMALE',
    dateEcheance: '2024-07-01',
  } satisfies DossierSummary

  return {
    pageOf,
    mockDossier,
    dossiersState: {
      data: pageOf([mockDossier]) as PageResponse<DossierSummary>,
      isLoading: false,
    },
  }
})

vi.mock('@/hooks/useDossiers', () => ({
  useDossiers: () => dossiersState,
  useTypesService: () => ({
    data: [
      {
        id: 'ts1',
        code: 'PASSEPORT',
        libelle: 'Passeport',
        delaiJours: 15,
        tarifDefaut: 50,
        checklist: null,
      },
    ] satisfies TypeService[],
    isLoading: false,
  }),
}))

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/dossiers']}>
        <Routes>
          <Route path="/dossiers" element={<DossiersPage />} />
          <Route path="/dossiers/new" element={<div>New dossier form</div>} />
          <Route path="/dossiers/:id" element={<div>Dossier detail</div>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('DossiersPage', () => {
  beforeEach(() => {
    dossiersState.data = pageOf([mockDossier])
    dossiersState.isLoading = false
  })

  it('renders list with mocked dossier', () => {
    renderPage()
    expect(screen.getByRole('heading', { name: /^Dossiers$/i })).toBeInTheDocument()
    expect(screen.getByText('DOS-2024-00042')).toBeInTheDocument()
    expect(screen.getByText(/Mamadou Camara/)).toBeInTheDocument()
    expect(screen.getByText('Passeport')).toBeInTheDocument()
    expect(screen.getAllByText('Soumis').length).toBeGreaterThanOrEqual(2)
    expect(screen.getByText('Normale')).toBeInTheDocument()
  })

  it('shows empty state when no dossiers', () => {
    dossiersState.data = pageOf([])
    renderPage()
    expect(screen.getByText(/Aucun dossier trouvé/i)).toBeInTheDocument()
  })

  it('exposes date filters and statut chips', async () => {
    const user = userEvent.setup()
    renderPage()
    expect(screen.getByLabelText(/Date début/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/Date fin/i)).toBeInTheDocument()

    const soumisChip = screen.getByRole('button', { name: /^Soumis$/i })
    await user.click(soumisChip)
    expect(soumisChip.className).toMatch(/ring-2/)
  })

  it('navigates to new dossier form', async () => {
    const user = userEvent.setup()
    renderPage()
    await user.click(screen.getByRole('button', { name: /Nouveau dossier/i }))
    expect(screen.getByText('New dossier form')).toBeInTheDocument()
  })

  it('links to dossier detail', () => {
    renderPage()
    const detailLink = screen.getByRole('link')
    expect(detailLink).toHaveAttribute('href', '/dossiers/d1')
  })
})
