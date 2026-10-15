import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { DocumentViewer } from '@/components/documents/DocumentViewer'
import type { DocumentOcrResult, OcrFieldSuggestion } from '@/services/ocrService'

const { getContent, getDownloadUrl, ocrState, suggestionsState } = vi.hoisted(() => ({
  getContent: vi.fn(),
  getDownloadUrl: vi.fn(),
  ocrState: {
    data: null as DocumentOcrResult | null,
    isLoading: false,
  },
  suggestionsState: {
    data: null as OcrFieldSuggestion | null,
  },
}))

vi.mock('@/services/documentService', () => ({
  documentService: {
    getContent: (...args: unknown[]) => getContent(...args),
    getDownloadUrl: (...args: unknown[]) => getDownloadUrl(...args),
  },
}))

vi.mock('@/hooks/useOcr', () => ({
  useDocumentOcr: () => ocrState,
  useOcrSuggestions: () => suggestionsState,
}))

vi.mock('@/hooks/use-toast', () => ({
  toast: vi.fn(),
}))

const doneOcr = {
  documentId: 'doc-1',
  provider: 'tesseract',
  status: 'DONE',
  extractedText: 'CAMARA MAMADOU',
  errorMessage: null,
  updatedAt: '2026-06-01T00:00:00Z',
} satisfies DocumentOcrResult

const suggestions = {
  documentId: 'doc-1',
  provider: 'tesseract',
  status: 'DONE',
  nom: 'Camara',
  prenom: 'Mamadou',
  dateNaissance: null,
  lieuNaissance: null,
  nationalite: 'GNB',
  numeroDocument: null,
  typeDocument: 'PASSEPORT',
  confidence: 0.82,
} satisfies OcrFieldSuggestion

function renderViewer(
  props: Partial<{ mimeType: string | null; usagerId: string | null; nomOriginal: string }> = {},
) {
  return render(
    <MemoryRouter>
      <Routes>
        <Route
          path="/"
          element={
            <DocumentViewer
              documentId="doc-1"
              mimeType={props.mimeType ?? 'application/pdf'}
              nomOriginal={props.nomOriginal ?? 'passeport.pdf'}
              usagerId={props.usagerId}
            />
          }
        />
        <Route path="/usagers/:id/edit" element={<div>Edit usager page</div>} />
        <Route path="/usagers/new" element={<div>New usager page</div>} />
      </Routes>
    </MemoryRouter>,
  )
}

function clickIcon(className: string) {
  const button = document.querySelector(`svg.${className}`)?.closest('button')
  if (!button) {
    throw new Error(`button for ${className} not found`)
  }
  return button
}

describe('DocumentViewer', () => {
  beforeEach(() => {
    getContent.mockReset()
    getDownloadUrl.mockReset()
    ocrState.data = doneOcr
    ocrState.isLoading = false
    suggestionsState.data = suggestions
    vi.stubGlobal('URL', {
      createObjectURL: vi.fn(() => 'blob:doc-url'),
      revokeObjectURL: vi.fn(),
    })
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    vi.restoreAllMocks()
  })

  it('opens a PDF preview and OCR text', async () => {
    getContent.mockResolvedValue({ data: new Blob(['%PDF'], { type: 'application/pdf' }) })
    const user = userEvent.setup()
    renderViewer()
    await user.click(clickIcon('lucide-eye'))
    expect(await screen.findByText('passeport.pdf')).toBeInTheDocument()
    expect(document.querySelector('iframe[title="passeport.pdf"]')).toHaveAttribute(
      'src',
      'blob:doc-url',
    )
    expect(screen.getByText('CAMARA MAMADOU')).toBeInTheDocument()
    expect(screen.getByText(/Confiance suggestions : 82 %/i)).toBeInTheDocument()
    expect(getContent).toHaveBeenCalledWith('doc-1', 'inline')
  })

  it('shows image preview', async () => {
    getContent.mockResolvedValue({ data: new Blob(['img'], { type: 'image/png' }) })
    const user = userEvent.setup()
    renderViewer({ mimeType: 'image/png', nomOriginal: 'photo.png' })
    await user.click(clickIcon('lucide-eye'))
    expect(await screen.findByRole('img', { name: 'photo.png' })).toHaveAttribute(
      'src',
      'blob:doc-url',
    )
  })

  it('shows no-preview for unsupported mime', async () => {
    getContent.mockResolvedValue({ data: new Blob(['zip']) })
    const user = userEvent.setup()
    renderViewer({ mimeType: 'application/zip', nomOriginal: 'archive.zip' })
    await user.click(clickIcon('lucide-eye'))
    expect(await screen.findByText(/Aperçu non disponible/i)).toBeInTheDocument()
  })

  it('falls back to presigned URL then toasts on total failure', async () => {
    getContent.mockRejectedValue(new Error('content down'))
    getDownloadUrl.mockRejectedValue(new Error('url down'))
    const user = userEvent.setup()
    renderViewer()
    await user.click(clickIcon('lucide-eye'))
    await waitFor(() => {
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    })
  })

  it('downloads via attachment blob', async () => {
    getContent.mockResolvedValue({ data: new Blob(['pdf']) })
    const originalCreate = document.createElement.bind(document)
    const anchor = originalCreate('a')
    const click = vi.spyOn(anchor, 'click').mockImplementation(() => {})
    vi.spyOn(document, 'createElement').mockImplementation((tag: string, options?: ElementCreationOptions) => {
      if (tag === 'a') return anchor
      return originalCreate(tag, options)
    })
    const user = userEvent.setup()
    renderViewer()
    await user.click(clickIcon('lucide-download'))
    await waitFor(() => {
      expect(getContent).toHaveBeenCalledWith('doc-1', 'attachment')
      expect(anchor.download).toBe('passeport.pdf')
      expect(click).toHaveBeenCalled()
    })
  })

  it('prefills existing usager from OCR suggestions', async () => {
    getContent.mockResolvedValue({ data: new Blob(['%PDF'], { type: 'application/pdf' }) })
    const user = userEvent.setup()
    renderViewer({ usagerId: 'u1' })
    await user.click(clickIcon('lucide-eye'))
    await user.click(await screen.findByRole('button', { name: /Pré-remplir fiche usager/i }))
    expect(await screen.findByText('Edit usager page')).toBeInTheDocument()
  })

  it('shows failed OCR message', async () => {
    ocrState.data = { ...doneOcr, status: 'FAILED', extractedText: null }
    suggestionsState.data = null
    getContent.mockResolvedValue({ data: new Blob(['%PDF']) })
    const user = userEvent.setup()
    renderViewer()
    await user.click(clickIcon('lucide-eye'))
    expect(await screen.findByText(/Échec OCR/i)).toBeInTheDocument()
  })
})
