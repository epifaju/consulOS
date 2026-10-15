import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { DocumentList } from '@/components/documents/DocumentList'
import type { DocumentItem } from '@/types/document'

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
} satisfies DocumentItem

const { docsState, authState, updateMutate, deleteMutate } = vi.hoisted(() => ({
  docsState: {
    data: null as DocumentItem[] | null,
    isLoading: false,
  },
  authState: { user: { role: 'ROLE_ADMIN' } as { role: string } | null },
  updateMutate: vi.fn(),
  deleteMutate: vi.fn(),
}))

vi.mock('@/stores/authStore', () => ({
  useAuthStore: (selector: (state: typeof authState) => unknown) => selector(authState),
}))

vi.mock('@/hooks/useDocuments', () => ({
  useDossierDocuments: () => docsState,
  useUpdateDocumentStatut: () => ({ mutate: updateMutate, isPending: false }),
  useDeleteDocument: () => ({ mutate: deleteMutate, isPending: false }),
}))

vi.mock('@/components/documents/DocumentViewer', () => ({
  DocumentViewer: () => <button type="button">Voir document</button>,
}))

vi.mock('@/components/documents/DocumentUploadZone', () => ({
  DocumentUploadZone: () => <div>Upload zone stub</div>,
}))

describe('DocumentList', () => {
  beforeEach(() => {
    authState.user = { role: 'ROLE_ADMIN' }
    docsState.data = [mockDoc]
    docsState.isLoading = false
    updateMutate.mockReset()
    deleteMutate.mockReset()
  })

  it('shows loading then empty states', () => {
    docsState.isLoading = true
    docsState.data = null
    const { unmount } = render(<DocumentList dossierId="d1" />)
    expect(screen.getByText(/Chargement/i)).toBeInTheDocument()
    unmount()

    docsState.isLoading = false
    docsState.data = []
    render(<DocumentList dossierId="d1" />)
    expect(screen.getByText(/Aucun document pour ce dossier/i)).toBeInTheDocument()
  })

  it('renders document row', () => {
    render(<DocumentList dossierId="d1" usagerId="u1" />)
    expect(screen.getByText('passeport.pdf')).toBeInTheDocument()
    expect(screen.getByText(/Pièce d'identité/i)).toBeInTheDocument()
    expect(screen.getByText('2.0 Ko')).toBeInTheDocument()
    expect(screen.getByText('Reçu')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Voir document/i })).toBeInTheDocument()
  })

  it('opens upload dialog', async () => {
    const user = userEvent.setup()
    render(<DocumentList dossierId="d1" usagerId="u1" />)
    await user.click(screen.getByRole('button', { name: /Ajouter un document/i }))
    expect(screen.getByText(/Upload zone stub/i)).toBeInTheDocument()
  })

  it('deletes as admin after confirm', async () => {
    const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(true)
    const user = userEvent.setup()
    render(<DocumentList dossierId="d1" />)
    const trash = document.querySelector('svg.lucide-trash-2')?.closest('button')
    expect(trash).toBeTruthy()
    await user.click(trash!)
    expect(confirmSpy).toHaveBeenCalled()
    expect(deleteMutate).toHaveBeenCalledWith('doc-1')
    confirmSpy.mockRestore()
  })

  it('hides delete for non-admin', () => {
    authState.user = { role: 'ROLE_AGENT' }
    render(<DocumentList dossierId="d1" />)
    expect(document.querySelector('svg.lucide-trash-2')).toBeNull()
  })
})
