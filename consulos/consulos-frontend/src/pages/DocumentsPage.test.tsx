import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { DocumentsPage } from '@/pages/DocumentsPage'
import type { DocumentListItem, PageResponse } from '@/types/document'

const mockDoc = {
  id: 'doc-1',
  dossierId: 'd1',
  usagerId: 'u1',
  nomFichier: 'stored.pdf',
  nomOriginal: 'passeport.pdf',
  typeDocument: 'PIECE_IDENTITE',
  mimeType: 'application/pdf',
  tailleOctets: 2048,
  statutControle: 'RECU',
  version: 1,
  notes: null,
  createdAt: '2024-06-01T10:00:00Z',
  dossierReference: 'DOS-2024-00042',
  usagerNom: 'Camara',
  usagerPrenom: 'Mamadou',
} satisfies DocumentListItem

function pageOf(content: DocumentListItem[]): PageResponse<DocumentListItem> {
  return {
    content,
    page: 0,
    size: 20,
    totalElements: content.length,
    totalPages: 1,
    first: true,
    last: true,
  }
}

const { docsState, authState, deleteMutate } = vi.hoisted(() => ({
  docsState: {
    data: null as PageResponse<DocumentListItem> | null,
    isLoading: false,
  },
  authState: { user: { role: 'ROLE_ADMIN' } as { role: string } | null },
  deleteMutate: vi.fn(),
}))

vi.mock('@/stores/authStore', () => ({
  useAuthStore: (selector: (state: typeof authState) => unknown) => selector(authState),
}))

vi.mock('@/hooks/useDocuments', () => ({
  useDocumentsList: () => docsState,
  useUpdateDocumentStatut: () => ({ mutate: vi.fn(), isPending: false }),
  useDeleteDocument: () => ({ mutate: deleteMutate, isPending: false }),
}))

vi.mock('@/components/documents/DocumentViewer', () => ({
  DocumentViewer: () => <button type="button">Voir document</button>,
}))

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <DocumentsPage />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('DocumentsPage', () => {
  beforeEach(() => {
    authState.user = { role: 'ROLE_ADMIN' }
    docsState.data = pageOf([mockDoc])
    docsState.isLoading = false
    deleteMutate.mockReset()
  })

  it('shows empty state', () => {
    docsState.data = pageOf([])
    renderPage()
    expect(screen.getByText(/Aucun document ne correspond aux filtres/i)).toBeInTheDocument()
  })

  it('renders document row', () => {
    renderPage()
    expect(screen.getByRole('heading', { name: /^Documents$/i })).toBeInTheDocument()
    expect(screen.getByText('passeport.pdf')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /DOS-2024-00042/i })).toHaveAttribute(
      'href',
      '/dossiers/d1',
    )
    expect(screen.getAllByText('Reçu').length).toBeGreaterThanOrEqual(2)
    expect(screen.getByText(/Mamadou Camara/)).toBeInTheDocument()
  })

  it('shows delete for admin and hides for agent', () => {
    const { unmount } = renderPage()
    expect(screen.getByRole('button', { name: /Voir document/i })).toBeInTheDocument()
    expect(document.querySelector('svg.lucide-trash-2')).toBeTruthy()
    unmount()

    authState.user = { role: 'ROLE_AGENT' }
    renderPage()
    expect(document.querySelector('svg.lucide-trash-2')).toBeNull()
  })

  it('deletes after confirm', async () => {
    const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(true)
    const user = userEvent.setup()
    renderPage()
    const trash = document.querySelector('svg.lucide-trash-2')?.closest('button')
    expect(trash).toBeTruthy()
    await user.click(trash!)
    expect(confirmSpy).toHaveBeenCalled()
    expect(deleteMutate).toHaveBeenCalledWith('doc-1')
    confirmSpy.mockRestore()
  })
})
