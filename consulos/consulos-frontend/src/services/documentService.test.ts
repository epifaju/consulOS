import { beforeEach, describe, expect, it, vi } from 'vitest'
import { documentService } from '@/services/documentService'

const { post } = vi.hoisted(() => ({
  post: vi.fn(),
}))

vi.mock('@/lib/apiClient', () => ({
  apiClient: {
    post,
    get: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}))

describe('documentService.replaceFile', () => {
  beforeEach(() => {
    post.mockReset()
    post.mockResolvedValue({ data: { success: true, data: { id: 'doc-1' } } })
  })

  it('posts FormData multipart and infers pdf mime when File.type is empty', async () => {
    const file = new File(['%PDF'], 'scan.pdf', { type: '' })

    await documentService.replaceFile('doc-1', file)

    expect(post).toHaveBeenCalledTimes(1)
    const [url, body, config] = post.mock.calls[0] as [string, FormData, { headers: { 'Content-Type': string } }]
    expect(url).toBe('/documents/doc-1/replace')
    expect(body).toBeInstanceOf(FormData)
    const uploaded = body.get('file') as File
    expect(uploaded).toBeInstanceOf(File)
    expect(uploaded.name).toBe('scan.pdf')
    expect(uploaded.type).toBe('application/pdf')
    expect(config.headers['Content-Type']).toBe('multipart/form-data')
  })

  it('keeps existing mime type', async () => {
    const file = new File(['x'], 'photo.png', { type: 'image/png' })

    await documentService.replaceFile('doc-1', file)

    const body = post.mock.calls[0][1] as FormData
    expect((body.get('file') as File).type).toBe('image/png')
  })
})

describe('documentService.upload', () => {
  beforeEach(() => {
    post.mockReset()
    post.mockResolvedValue({ data: { success: true, data: { id: 'doc-1' } } })
  })

  it('posts multipart with typeDocument and optional dossier/usager', async () => {
    const file = new File(['x'], 'scan.pdf', { type: 'application/pdf' })

    await documentService.upload({
      file,
      dossierId: 'd-1',
      usagerId: 'u-1',
      typeDocument: 'PASSEPORT',
      notes: 'verso',
    })

    const [url, body, config] = post.mock.calls[0] as [
      string,
      FormData,
      { headers: { 'Content-Type': string } },
    ]
    expect(url).toBe('/documents/upload')
    expect(body.get('typeDocument')).toBe('PASSEPORT')
    expect(body.get('dossierId')).toBe('d-1')
    expect(body.get('usagerId')).toBe('u-1')
    expect(body.get('notes')).toBe('verso')
    expect(body.get('file')).toBeInstanceOf(File)
    expect(config.headers['Content-Type']).toBe('multipart/form-data')
  })
})

