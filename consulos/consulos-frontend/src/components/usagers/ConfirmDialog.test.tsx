import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { ConfirmDialog } from '@/components/usagers/ConfirmDialog'

describe('ConfirmDialog', () => {
  it('confirms and cancels', async () => {
    const onConfirm = vi.fn()
    const onCancel = vi.fn()
    const user = userEvent.setup()
    render(
      <ConfirmDialog
        open
        title="Supprimer l'usager"
        description="Cette action est irréversible."
        confirmLabel="Supprimer"
        cancelLabel="Annuler"
        onConfirm={onConfirm}
        onCancel={onCancel}
      />,
    )

    expect(screen.getByText(/Supprimer l'usager/i)).toBeInTheDocument()
    expect(screen.getByText(/irréversible/i)).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: /^Annuler$/i }))
    expect(onCancel).toHaveBeenCalled()

    await user.click(screen.getByRole('button', { name: /^Supprimer$/i }))
    expect(onConfirm).toHaveBeenCalled()
  })

  it('disables actions while loading', () => {
    render(
      <ConfirmDialog
        open
        title="Confirmer"
        description="Patientez"
        confirmLabel="OK"
        cancelLabel="Annuler"
        onConfirm={vi.fn()}
        onCancel={vi.fn()}
        loading
      />,
    )
    expect(screen.getByRole('button', { name: /^OK$/i })).toBeDisabled()
    expect(screen.getByRole('button', { name: /^Annuler$/i })).toBeDisabled()
  })
})
