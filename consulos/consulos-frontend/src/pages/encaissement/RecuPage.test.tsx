import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { RecuPage } from '@/pages/encaissement/RecuPage'
import { encaissementService } from '@/services/encaissementService'
import type { EncaissementDetail } from '@/types/encaissement'

const mockEnc = {
  id: 'e1',
  numeroRecu: 'REC-2024-00001',
  usagerId: 'u1',
  usagerNom: 'Camara',
  usagerPrenom: 'Mamadou',
  usagerNumero: 'USG-1',
  dossierId: 'd1',
  dossierReference: 'DOS-001',
  typeServiceLibelle: 'Passeport',
  montantTotal: 50,
  montantPercu: 50,
  devise: 'EUR',
  modePaiement: 'ESPECES',
  statut: 'PAYE',
  createdAt: '2024-06-01T10:00:00Z',
  datePaiement: '2024-06-01T10:05:00Z',
  caissierId: 'c1',
  caissierNom: 'Agent',
  notes: null,
  updatedAt: '2024-06-01T10:05:00Z',
} satisfies EncaissementDetail

vi.mock('@/hooks/useEncaissements', () => ({
  useEncaissement: () => ({ data: mockEnc, isLoading: false }),
}))

vi.mock('@/services/encaissementService', () => ({
  encaissementService: {
    getRecuPdf: vi.fn(),
  },
}))

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/caisse/recu/e1']}>
        <Routes>
          <Route path="/caisse/recu/:id" element={<RecuPage />} />
          <Route path="/caisse" element={<div>Caisse</div>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('RecuPage', () => {
  beforeEach(() => {
    vi.mocked(encaissementService.getRecuPdf).mockReset()
    vi.stubGlobal('URL', {
      createObjectURL: vi.fn(() => 'blob:recu-url'),
      revokeObjectURL: vi.fn(),
    })
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('renders title, receipt number and back link', async () => {
    vi.mocked(encaissementService.getRecuPdf).mockResolvedValue({ data: new Blob(['pdf']) } as never)
    renderPage()
    expect(screen.getByRole('heading', { name: /Reçu consulaire/i })).toBeInTheDocument()
    expect(screen.getByText('REC-2024-00001')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /Retour à la caisse/i })).toHaveAttribute(
      'href',
      '/caisse',
    )
    await waitFor(() => {
      expect(screen.getByTitle(/Reçu consulaire/i)).toBeInTheDocument()
    })
    expect(screen.getByRole('button', { name: /Télécharger PDF/i })).toBeEnabled()
    expect(screen.getByRole('button', { name: /Imprimer/i })).toBeEnabled()
  })

  it('shows error when PDF load fails', async () => {
    vi.mocked(encaissementService.getRecuPdf).mockRejectedValue(new Error('pdf fail'))
    renderPage()
    expect(await screen.findByText(/Impossible de charger le reçu PDF/i)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Télécharger PDF/i })).toBeDisabled()
  })
})
