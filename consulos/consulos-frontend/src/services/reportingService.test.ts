import { beforeEach, describe, expect, it, vi } from 'vitest'
import { reportingService } from '@/services/reportingService'

const { get } = vi.hoisted(() => ({
  get: vi.fn(),
}))

vi.mock('@/lib/apiClient', () => ({
  apiClient: { get },
}))

describe('reportingService', () => {
  beforeEach(() => {
    get.mockReset()
    get.mockResolvedValue({ data: { success: true, data: null } })
  })

  it('requests monthly json with year and month', async () => {
    await reportingService.mensuel(2024, 3)

    expect(get).toHaveBeenCalledWith('/reporting/mensuel', {
      params: { annee: 2024, mois: 3 },
    })
  })

  it('requests excel and csv as blobs', async () => {
    get.mockResolvedValue({ data: new Blob(['x']) })

    await reportingService.mensuelExcel(2024, 3)
    await reportingService.dossiersCsv(2024, 3)
    await reportingService.rdvCsv(2024, 3)

    expect(get).toHaveBeenCalledWith('/reporting/mensuel/excel', {
      params: { annee: 2024, mois: 3 },
      responseType: 'blob',
    })
    expect(get).toHaveBeenCalledWith('/reporting/dossiers/csv', {
      params: { annee: 2024, mois: 3 },
      responseType: 'blob',
    })
    expect(get).toHaveBeenCalledWith('/reporting/rdv/csv', {
      params: { annee: 2024, mois: 3 },
      responseType: 'blob',
    })
  })
})
