import { beforeEach, describe, expect, it, vi } from 'vitest'
import { encaissementService } from '@/services/encaissementService'

const { get, post } = vi.hoisted(() => ({
  get: vi.fn(),
  post: vi.fn(),
}))

vi.mock('@/lib/apiClient', () => ({
  apiClient: { get, post },
}))

describe('encaissementService', () => {
  beforeEach(() => {
    get.mockReset()
    post.mockReset()
    get.mockResolvedValue({ data: new Blob(['pdf']) })
    post.mockResolvedValue({ data: { success: true, data: null } })
  })

  it('records a payment', async () => {
    await encaissementService.enregistrerPaiement('enc-1', {
      montant: 50,
      modePaiement: 'ESPECES',
    })

    expect(post).toHaveBeenCalledWith('/encaissements/enc-1/paiement', {
      montant: 50,
      modePaiement: 'ESPECES',
    })
  })

  it('fetches receipt pdf as blob', async () => {
    await encaissementService.getRecuPdf('enc-1')

    expect(get).toHaveBeenCalledWith('/encaissements/enc-1/recu/pdf', { responseType: 'blob' })
  })

  it('exports excel blob and defaults dateFin to dateDebut', async () => {
    await encaissementService.exportExcel('2024-03-01')
    await encaissementService.exportExcel('2024-03-01', '2024-03-31')

    expect(get).toHaveBeenNthCalledWith(1, '/encaissements/export/excel', {
      params: { dateDebut: '2024-03-01', dateFin: '2024-03-01' },
      responseType: 'blob',
    })
    expect(get).toHaveBeenNthCalledWith(2, '/encaissements/export/excel', {
      params: { dateDebut: '2024-03-01', dateFin: '2024-03-31' },
      responseType: 'blob',
    })
  })

  it('loads journal de caisse for a date', async () => {
    get.mockResolvedValue({ data: { success: true, data: null } })

    await encaissementService.getJournalCaisse('2024-03-15')

    expect(get).toHaveBeenCalledWith('/encaissements/journal-caisse', {
      params: { date: '2024-03-15' },
    })
  })
})
