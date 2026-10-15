import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { UsersFormModal } from '@/components/administration/UsersFormModal'
import type { Role, UserAdmin } from '@/types/admin'

const ROLE_AGENT_ID = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'
const ROLE_ADMIN_ID = 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'

const roles = [
  {
    id: ROLE_AGENT_ID,
    code: 'ROLE_AGENT',
    libelle: 'Agent',
    permissions: ['USAGER_READ'],
  },
  {
    id: ROLE_ADMIN_ID,
    code: 'ROLE_ADMIN',
    libelle: 'Administrateur',
    permissions: ['ADMIN_FULL'],
  },
] satisfies Role[]

const existingUser = {
  id: 'user-1',
  email: 'agent@consulos.local',
  nom: 'Diallo',
  prenom: 'Awa',
  roleCode: 'ROLE_AGENT',
  roleLibelle: 'Agent',
  actif: true,
  derniereConnexion: null,
  createdAt: '2026-01-01T00:00:00Z',
} satisfies UserAdmin

const { createMutate, updateMutate } = vi.hoisted(() => ({
  createMutate: vi.fn(),
  updateMutate: vi.fn(),
}))

vi.mock('@/hooks/useAdmin', () => ({
  useAdminRoles: () => ({ data: roles, isLoading: false }),
  useCreateUser: () => ({ mutate: createMutate, isPending: false }),
  useUpdateUser: () => ({ mutate: updateMutate, isPending: false }),
}))

describe('UsersFormModal', () => {
  beforeEach(() => {
    createMutate.mockReset()
    updateMutate.mockReset()
  })

  it('creates an agent and returns temporary password', async () => {
    const onOpenChange = vi.fn()
    const onCreated = vi.fn()
    createMutate.mockImplementation((_values, options?: { onSuccess?: (res: unknown) => void }) => {
      options?.onSuccess?.({ data: { data: { temporaryPassword: 'Tmp-Pass-99' } } })
    })
    const user = userEvent.setup()
    render(
      <UsersFormModal open onOpenChange={onOpenChange} onCreated={onCreated} />,
    )

    expect(screen.getByText(/Créer un agent/i)).toBeInTheDocument()
    const [email, prenom, nom] = screen.getAllByRole('textbox')
    fireEvent.change(email, { target: { value: 'new@consulos.local' } })
    fireEvent.change(prenom, { target: { value: 'Fatou' } })
    fireEvent.change(nom, { target: { value: 'Bah' } })
    await user.click(screen.getByRole('combobox'))
    await user.click(await screen.findByRole('option', { name: 'Agent' }, { timeout: 8000 }))
    await user.click(screen.getByRole('button', { name: /Enregistrer/i }))

    await waitFor(() => {
      expect(createMutate).toHaveBeenCalledWith(
        {
          email: 'new@consulos.local',
          prenom: 'Fatou',
          nom: 'Bah',
          roleId: ROLE_AGENT_ID,
          actif: true,
        },
        expect.any(Object),
      )
    })
    expect(onCreated).toHaveBeenCalledWith('Tmp-Pass-99')
    expect(onOpenChange).toHaveBeenCalledWith(false)
    expect(updateMutate).not.toHaveBeenCalled()
  }, 15000)

  it('hydrates and updates an existing agent', async () => {
    const onOpenChange = vi.fn()
    updateMutate.mockImplementation((_values, options?: { onSuccess?: () => void }) => {
      options?.onSuccess?.()
    })
    const user = userEvent.setup()
    render(
      <UsersFormModal open onOpenChange={onOpenChange} user={existingUser} />,
    )

    expect(screen.getByText(/Modifier l'agent/i)).toBeInTheDocument()
    expect(screen.getByDisplayValue('agent@consulos.local')).toBeInTheDocument()
    expect(screen.getByDisplayValue('Awa')).toBeInTheDocument()
    expect(screen.getByDisplayValue('Diallo')).toBeInTheDocument()
    expect(screen.getByRole('checkbox')).toBeChecked()

    const prenom = screen.getByDisplayValue('Awa')
    await user.clear(prenom)
    await user.type(prenom, 'Aissatou')
    await user.click(screen.getByRole('button', { name: /Enregistrer/i }))

    await waitFor(() => {
      expect(updateMutate).toHaveBeenCalledWith(
        expect.objectContaining({
          email: 'agent@consulos.local',
          prenom: 'Aissatou',
          nom: 'Diallo',
          roleId: ROLE_AGENT_ID,
          actif: true,
        }),
        expect.any(Object),
      )
    })
    expect(onOpenChange).toHaveBeenCalledWith(false)
    expect(createMutate).not.toHaveBeenCalled()
  })

  it('does not submit invalid form', async () => {
    const user = userEvent.setup()
    render(<UsersFormModal open onOpenChange={vi.fn()} />)
    await user.click(screen.getByRole('button', { name: /Enregistrer/i }))
    expect(createMutate).not.toHaveBeenCalled()
  })

  it('closes on cancel', async () => {
    const onOpenChange = vi.fn()
    const user = userEvent.setup()
    render(<UsersFormModal open onOpenChange={onOpenChange} />)
    await user.click(screen.getByRole('button', { name: /Annuler/i }))
    expect(onOpenChange).toHaveBeenCalledWith(false)
  })

  it('toggles actif checkbox', async () => {
    const user = userEvent.setup()
    render(<UsersFormModal open onOpenChange={vi.fn()} user={existingUser} />)
    const checkbox = screen.getByRole('checkbox')
    expect(checkbox).toBeChecked()
    await user.click(checkbox)
    expect(checkbox).not.toBeChecked()
  })
})
