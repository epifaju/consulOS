import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { renderHook, waitFor } from '@testing-library/react'
import { type ReactNode } from 'react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { useReplaceDocumentFile } from '@/hooks/useDocuments'

const { replaceFile } = vi.hoisted(() => ({
  replaceFile: vi.fn(),
}))

vi.mock('@/services/documentService', () => ({
  documentService: {
    replaceFile: (...args: unknown[]) => replaceFile(...args),
  },
}))

describe('useReplaceDocumentFile', () => {
  let queryClient: QueryClient

  beforeEach(() => {
    replaceFile.mockReset()
    replaceFile.mockResolvedValue({ data: { data: { id: 'doc-1' } } })
    queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
    })
  })

  function wrapper({ children }: { children: ReactNode }) {
    return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  }

  it('calls replaceFile and invalidates document queries including dossier', async () => {
    const invalidate = vi.spyOn(queryClient, 'invalidateQueries')
    const { result } = renderHook(() => useReplaceDocumentFile('dossier-1'), { wrapper })
    const file = new File(['x'], 'scan.pdf', { type: 'application/pdf' })

    await result.current.mutateAsync({ id: 'doc-1', file })

    expect(replaceFile).toHaveBeenCalledWith('doc-1', file)
    await waitFor(() => {
      expect(invalidate).toHaveBeenCalledWith({ queryKey: ['documents-list'] })
      expect(invalidate).toHaveBeenCalledWith({ queryKey: ['usager-documents'] })
      expect(invalidate).toHaveBeenCalledWith({ queryKey: ['dossier-documents', 'dossier-1'] })
    })
  })

  it('does not invalidate dossier-documents when dossierId is omitted', async () => {
    const invalidate = vi.spyOn(queryClient, 'invalidateQueries')
    const { result } = renderHook(() => useReplaceDocumentFile(), { wrapper })
    const file = new File(['x'], 'scan.pdf', { type: 'application/pdf' })

    await result.current.mutateAsync({ id: 'doc-1', file })

    await waitFor(() => {
      expect(invalidate).toHaveBeenCalledWith({ queryKey: ['documents-list'] })
    })
    expect(
      invalidate.mock.calls.some((call) => {
        const key = (call[0] as { queryKey: unknown[] } | undefined)?.queryKey
        return Array.isArray(key) && key[0] === 'dossier-documents'
      }),
    ).toBe(false)
  })
})
