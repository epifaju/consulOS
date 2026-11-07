import { beforeEach, describe, expect, it, vi } from 'vitest'
import { dossierService } from '@/services/dossierService'

const { get, post, put } = vi.hoisted(() => ({
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
}))

vi.mock('@/lib/apiClient', () => ({
  apiClient: { get, post, put },
}))

describe('dossierService', () => {
  beforeEach(() => {
    get.mockReset()
    post.mockReset()
    put.mockReset()
    get.mockResolvedValue({ data: { success: true, data: null } })
    post.mockResolvedValue({ data: { success: true, data: null } })
    put.mockResolvedValue({ data: { success: true, data: null } })
  })

  it('lists dossiers with query params', async () => {
    const params = { page: 0, size: 20, statut: ['SOUMIS' as const], typeService: 'PASSEPORT' }
    await dossierService.list(params)

    expect(get).toHaveBeenCalledWith('/dossiers', { params })
  })

  it('creates a dossier and transitions statut', async () => {
    await dossierService.create({
      usagerId: 'u-1',
      typeServiceId: 'ts-1',
      priorite: 'NORMALE',
    })
    await dossierService.transition('d-1', { statut: 'EN_REVUE', motif: 'ok' })

    expect(post).toHaveBeenCalledWith('/dossiers', {
      usagerId: 'u-1',
      typeServiceId: 'ts-1',
      priorite: 'NORMALE',
    })
    expect(post).toHaveBeenCalledWith('/dossiers/d-1/transition', { statut: 'EN_REVUE', motif: 'ok' })
  })

  it('encodes checklist item code and assigns agent', async () => {
    await dossierService.updateChecklistItem('d-1', 'PIECE ID', true)
    await dossierService.assignAgent('d-1', 'agent-9')

    expect(put).toHaveBeenCalledWith('/dossiers/d-1/checklist/PIECE%20ID', { valide: true })
    expect(post).toHaveBeenCalledWith('/dossiers/d-1/assign-agent', { agentId: 'agent-9' })
  })
})
