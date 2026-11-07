import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { renderHook, waitFor } from '@testing-library/react'
import { type ReactNode } from 'react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { useDocumentOcr, useOcrSuggestions } from '@/hooks/useOcr'
import type { DocumentOcrResult, OcrFieldSuggestion } from '@/services/ocrService'

const { getDocumentOcr, getSuggestions } = vi.hoisted(() => ({
  getDocumentOcr: vi.fn(),
  getSuggestions: vi.fn(),
}))

vi.mock('@/services/ocrService', () => ({
  ocrService: {
    getDocumentOcr: (...args: unknown[]) => getDocumentOcr(...args),
    getSuggestions: (...args: unknown[]) => getSuggestions(...args),
  },
}))

const ocrResult: DocumentOcrResult = {
  documentId: 'doc-1',
  provider: 'tesseract',
  status: 'DONE',
  extractedText: 'Mamadou Camara',
  errorMessage: null,
  updatedAt: '2026-03-15T10:00:00Z',
}

const suggestion: OcrFieldSuggestion = {
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
  confidence: 0.91,
}

describe('useDocumentOcr / useOcrSuggestions', () => {
  let queryClient: QueryClient

  beforeEach(() => {
    getDocumentOcr.mockReset()
    getSuggestions.mockReset()
    queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    })
  })

  function wrapper({ children }: { children: ReactNode }) {
    return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  }

  it('loads OCR when documentId is set', async () => {
    getDocumentOcr.mockResolvedValue({ data: { data: ocrResult } })

    const { result } = renderHook(() => useDocumentOcr('doc-1'), { wrapper })

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true)
    })
    expect(getDocumentOcr).toHaveBeenCalledWith('doc-1')
    expect(result.current.data?.extractedText).toBe('Mamadou Camara')
  })

  it('does not fetch when documentId is null', () => {
    const { result } = renderHook(() => useDocumentOcr(null), { wrapper })

    expect(result.current.fetchStatus).toBe('idle')
    expect(getDocumentOcr).not.toHaveBeenCalled()
  })

  it('returns null when OCR request fails', async () => {
    getDocumentOcr.mockRejectedValue(new Error('not found'))

    const { result } = renderHook(() => useDocumentOcr('doc-missing'), { wrapper })

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true)
    })
    expect(result.current.data).toBeNull()
  })

  it('loads suggestions and swallows errors as null', async () => {
    getSuggestions.mockResolvedValue({ data: { data: suggestion } })

    const { result } = renderHook(() => useOcrSuggestions('doc-1'), { wrapper })

    await waitFor(() => {
      expect(result.current.data?.nom).toBe('Camara')
    })

    getSuggestions.mockRejectedValue(new Error('down'))
    queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    })
    const { result: failed } = renderHook(() => useOcrSuggestions('doc-1'), { wrapper })

    await waitFor(() => {
      expect(failed.current.isSuccess).toBe(true)
    })
    expect(failed.current.data).toBeNull()
  })
})
