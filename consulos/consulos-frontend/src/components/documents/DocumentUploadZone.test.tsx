import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { DocumentUploadZone } from '@/components/documents/DocumentUploadZone'

const { uploadMutate } = vi.hoisted(() => ({
  uploadMutate: vi.fn(),
}))

vi.mock('@/hooks/useDocuments', () => ({
  useUploadDocument: () => ({ mutate: uploadMutate, isPending: false }),
}))

vi.mock('@/hooks/use-toast', () => ({
  toast: vi.fn(),
}))

function pickFile(input: HTMLInputElement, file: File) {
  fireEvent.change(input, { target: { files: [file] } })
}

describe('DocumentUploadZone', () => {
  beforeEach(() => {
    uploadMutate.mockReset()
  })

  it('keeps submit disabled without file', () => {
    render(<DocumentUploadZone dossierId="d1" />)
    expect(screen.getByText(/Glissez-déposez/i)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /^Envoyer$/i })).toBeDisabled()
  })

  it('rejects unsupported mime type', () => {
    render(<DocumentUploadZone dossierId="d1" />)
    const input = document.querySelector('input[type="file"]') as HTMLInputElement
    pickFile(input, new File(['x'], 'notes.txt', { type: 'text/plain' }))
    expect(screen.getByText(/Type de fichier non autorisé/i)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /^Envoyer$/i })).toBeDisabled()
  })

  it('rejects oversized file', () => {
    render(<DocumentUploadZone dossierId="d1" />)
    const input = document.querySelector('input[type="file"]') as HTMLInputElement
    pickFile(
      input,
      new File([new Uint8Array(10_485_761)], 'big.pdf', { type: 'application/pdf' }),
    )
    expect(screen.getByText(/Fichier trop volumineux/i)).toBeInTheDocument()
  })

  it('uploads valid pdf', async () => {
    uploadMutate.mockImplementation((_payload, options?: { onSuccess?: () => void }) => {
      options?.onSuccess?.()
    })
    const user = userEvent.setup()
    render(<DocumentUploadZone dossierId="d1" usagerId="u1" />)
    const input = document.querySelector('input[type="file"]') as HTMLInputElement
    pickFile(input, new File(['%PDF'], 'doc.pdf', { type: 'application/pdf' }))
    expect(await screen.findByText('doc.pdf')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: /^Envoyer$/i }))
    await waitFor(() => {
      expect(uploadMutate).toHaveBeenCalledWith(
        expect.objectContaining({
          dossierId: 'd1',
          usagerId: 'u1',
          file: expect.any(File),
        }),
        expect.any(Object),
      )
    })
  })
})
