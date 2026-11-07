import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { renderHook, waitFor } from '@testing-library/react'
import { type ReactNode } from 'react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { useExportReportingDossiersCsv, useExportReportingExcel } from '@/hooks/useReporting'

const { mensuelExcel, dossiersCsv, downloadBlob } = vi.hoisted(() => ({
  mensuelExcel: vi.fn(),
  dossiersCsv: vi.fn(),
  downloadBlob: vi.fn(),
}))

vi.mock('@/services/reportingService', () => ({
  reportingService: {
    mensuelExcel: (...args: unknown[]) => mensuelExcel(...args),
    dossiersCsv: (...args: unknown[]) => dossiersCsv(...args),
  },
}))

vi.mock('@/lib/downloadBlob', async () => {
  const actual = await vi.importActual<typeof import('@/lib/downloadBlob')>('@/lib/downloadBlob')
  return {
    ...actual,
    downloadBlob: (...args: unknown[]) => downloadBlob(...args),
  }
})

describe('useReporting exports', () => {
  let queryClient: QueryClient

  beforeEach(() => {
    mensuelExcel.mockReset()
    dossiersCsv.mockReset()
    downloadBlob.mockReset()
    queryClient = new QueryClient({
      defaultOptions: { mutations: { retry: false } },
    })
  })

  function wrapper({ children }: { children: ReactNode }) {
    return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  }

  it('downloads excel using Content-Disposition filename', async () => {
    const blob = new Blob(['xlsx'])
    mensuelExcel.mockResolvedValue({
      data: blob,
      headers: { 'content-disposition': 'attachment; filename="reporting-mensuel-2024-03.xlsx"' },
    })

    const { result } = renderHook(() => useExportReportingExcel(), { wrapper })
    await result.current.mutateAsync({ annee: 2024, mois: 3 })

    expect(mensuelExcel).toHaveBeenCalledWith(2024, 3)
    expect(downloadBlob).toHaveBeenCalledWith(blob, 'reporting-mensuel-2024-03.xlsx')
  })

  it('falls back to generated csv filename when header is missing', async () => {
    const blob = new Blob(['csv'])
    dossiersCsv.mockResolvedValue({
      data: blob,
      headers: {},
    })

    const { result } = renderHook(() => useExportReportingDossiersCsv(), { wrapper })
    await result.current.mutateAsync({ annee: 2024, mois: 3 })

    await waitFor(() => {
      expect(downloadBlob).toHaveBeenCalledWith(blob, 'dossiers-2024-03.csv')
    })
  })
})
