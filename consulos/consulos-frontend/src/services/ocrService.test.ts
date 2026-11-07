import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ocrService } from '@/services/ocrService'

const { get } = vi.hoisted(() => ({
  get: vi.fn(),
}))

vi.mock('@/lib/apiClient', () => ({
  apiClient: { get },
}))

describe('ocrService', () => {
  beforeEach(() => {
    get.mockReset()
    get.mockResolvedValue({ data: { success: true, data: null } })
  })

  it('gets OCR result by document id', async () => {
    await ocrService.getDocumentOcr('doc-1')

    expect(get).toHaveBeenCalledWith('/ocr/documents/doc-1')
  })

  it('gets field suggestions by document id', async () => {
    await ocrService.getSuggestions('doc-1')

    expect(get).toHaveBeenCalledWith('/ocr/documents/doc-1/suggestions')
  })
})
