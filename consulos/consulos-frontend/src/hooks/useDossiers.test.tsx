import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { renderHook, waitFor } from '@testing-library/react'
import { type ReactNode } from 'react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { useDossierTransition, useUpdateChecklistItem } from '@/hooks/useDossiers'

const { transition, updateChecklistItem } = vi.hoisted(() => ({
  transition: vi.fn(),
  updateChecklistItem: vi.fn(),
}))

vi.mock('@/services/dossierService', () => ({
  dossierService: {
    transition: (...args: unknown[]) => transition(...args),
    updateChecklistItem: (...args: unknown[]) => updateChecklistItem(...args),
  },
}))

describe('useDossiers mutations', () => {
  let queryClient: QueryClient

  beforeEach(() => {
    transition.mockReset()
    updateChecklistItem.mockReset()
    transition.mockResolvedValue({ data: { data: { id: 'd-1' } } })
    updateChecklistItem.mockResolvedValue({ data: { data: [] } })
    queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
    })
  })

  function wrapper({ children }: { children: ReactNode }) {
    return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  }

  it('invalidates dossier, list and historique after transition', async () => {
    const invalidate = vi.spyOn(queryClient, 'invalidateQueries')
    const { result } = renderHook(() => useDossierTransition('d-1'), { wrapper })

    await result.current.mutateAsync({ statut: 'VALIDE' })

    expect(transition).toHaveBeenCalledWith('d-1', { statut: 'VALIDE' })
    await waitFor(() => {
      expect(invalidate).toHaveBeenCalledWith({ queryKey: ['dossier', 'd-1'] })
      expect(invalidate).toHaveBeenCalledWith({ queryKey: ['dossiers'] })
      expect(invalidate).toHaveBeenCalledWith({ queryKey: ['dossier-historique', 'd-1'] })
    })
  })

  it('invalidates checklist after item update', async () => {
    const invalidate = vi.spyOn(queryClient, 'invalidateQueries')
    const { result } = renderHook(() => useUpdateChecklistItem('d-1'), { wrapper })

    await result.current.mutateAsync({ code: 'PASSEPORT', valide: true })

    expect(updateChecklistItem).toHaveBeenCalledWith('d-1', 'PASSEPORT', true)
    await waitFor(() => {
      expect(invalidate).toHaveBeenCalledWith({ queryKey: ['dossier-checklist', 'd-1'] })
    })
  })
})
