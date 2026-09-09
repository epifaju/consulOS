import { describe, expect, it, vi } from 'vitest'
import { downloadBlob, parseFilenameFromDisposition } from '@/lib/downloadBlob'

describe('parseFilenameFromDisposition', () => {
  it('returns fallback when header is missing', () => {
    expect(parseFilenameFromDisposition(undefined, 'report.pdf')).toBe('report.pdf')
  })

  it('parses quoted and unquoted filename', () => {
    expect(parseFilenameFromDisposition('attachment; filename="recu-1.pdf"', 'x.pdf')).toBe(
      'recu-1.pdf',
    )
    expect(parseFilenameFromDisposition('attachment; filename=export.csv', 'x.csv')).toBe(
      'export.csv',
    )
  })

  it('returns fallback when filename token is absent', () => {
    expect(parseFilenameFromDisposition('inline', 'fallback.bin')).toBe('fallback.bin')
  })
})

describe('downloadBlob', () => {
  it('creates an object URL, clicks an anchor, then revokes', () => {
    const createObjectURL = vi.fn(() => 'blob:mock-url')
    const revokeObjectURL = vi.fn()
    vi.stubGlobal('URL', { createObjectURL, revokeObjectURL })

    const click = vi.fn()
    const anchor = { href: '', download: '', click } as unknown as HTMLAnchorElement
    const createElement = vi.spyOn(document, 'createElement').mockReturnValue(anchor)

    downloadBlob(new Blob(['pdf']), 'recu.pdf')

    expect(createObjectURL).toHaveBeenCalledOnce()
    expect(anchor.href).toBe('blob:mock-url')
    expect(anchor.download).toBe('recu.pdf')
    expect(click).toHaveBeenCalledOnce()
    expect(revokeObjectURL).toHaveBeenCalledWith('blob:mock-url')

    createElement.mockRestore()
    vi.unstubAllGlobals()
  })
})
