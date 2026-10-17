import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ChecklistViewer } from '@/components/dossiers/ChecklistViewer'
import type { ChecklistItem } from '@/types/dossier'

const items: ChecklistItem[] = [
  { code: 'ID', libelle: 'Pièce d’identité' },
  { code: 'PHOTO', libelle: 'Photo d’identité' },
]

const { checklistState, updateMutate, authState } = vi.hoisted(() => ({
  checklistState: {
    data: undefined as ChecklistItem[] | undefined,
    isLoading: false,
  },
  updateMutate: vi.fn(),
  authState: { user: { role: 'ROLE_AGENT' } as { role: string } | null },
}))

vi.mock('@/hooks/useDossiers', () => ({
  useDossierChecklist: () => checklistState,
  useUpdateChecklistItem: () => ({ mutate: updateMutate, isPending: false }),
}))

vi.mock('@/stores/authStore', () => ({
  useAuthStore: (selector: (state: typeof authState) => unknown) => selector(authState),
}))

function renderViewer(props: { items?: ChecklistItem[] | null; dossierId?: string } = {}) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })
  return render(
    <QueryClientProvider client={queryClient}>
      <ChecklistViewer
        items={'items' in props ? props.items : items}
        dossierId={props.dossierId}
      />
    </QueryClientProvider>,
  )
}

describe('ChecklistViewer', () => {
  beforeEach(() => {
    checklistState.data = undefined
    checklistState.isLoading = false
    updateMutate.mockReset()
    authState.user = { role: 'ROLE_AGENT' }
  })

  it('shows empty state', () => {
    renderViewer({ items: [] })
    expect(screen.getByText(/Aucune pièce requise/i)).toBeInTheDocument()
  })

  it('treats null as empty', () => {
    renderViewer({ items: null })
    expect(screen.getByText(/Aucune pièce requise/i)).toBeInTheDocument()
  })

  it('renders items and progress without persisted flags', () => {
    renderViewer()
    expect(screen.getByText('0/2 pièces validées')).toBeInTheDocument()
    expect(screen.getByText('Pièce d’identité')).toBeInTheDocument()
    expect(screen.getByLabelText(/Pièce d’identité/i)).toBeDisabled()
    expect(screen.getAllByRole('checkbox')).toHaveLength(2)
  })

  it('counts validated items from the API overlay', () => {
    checklistState.data = [
      { code: 'ID', libelle: 'Pièce d’identité', valide: true },
      { code: 'PHOTO', libelle: 'Photo d’identité', valide: false },
    ]
    renderViewer({ dossierId: 'd1' })
    expect(screen.getByText('1/2 pièces validées')).toBeInTheDocument()
    expect(screen.getByLabelText(/Pièce d’identité/i)).toBeChecked()
    expect(screen.getByLabelText(/Pièce d’identité/i)).toBeEnabled()
  })

  it('toggles an item when the agent clicks', async () => {
    checklistState.data = [
      { code: 'ID', libelle: 'Pièce d’identité', valide: false },
      { code: 'PHOTO', libelle: 'Photo d’identité', valide: false },
    ]
    const user = userEvent.setup()
    renderViewer({ dossierId: 'd1' })
    await user.click(screen.getByLabelText(/Pièce d’identité/i))
    expect(updateMutate).toHaveBeenCalledWith({ code: 'ID', valide: true })
  })

  it('keeps checkboxes read-only for caissier', () => {
    authState.user = { role: 'ROLE_CAISSIER' }
    checklistState.data = [{ code: 'ID', libelle: 'Pièce d’identité', valide: false }]
    renderViewer({ dossierId: 'd1', items: [{ code: 'ID', libelle: 'Pièce d’identité' }] })
    expect(screen.getByLabelText(/Pièce d’identité/i)).toBeDisabled()
  })
})
