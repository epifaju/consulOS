import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { ChecklistViewer } from '@/components/dossiers/ChecklistViewer'

describe('ChecklistViewer', () => {
  it('shows empty state', () => {
    render(<ChecklistViewer items={[]} />)
    expect(screen.getByText(/Aucune pièce requise/i)).toBeInTheDocument()
  })

  it('treats null as empty', () => {
    render(<ChecklistViewer items={null} />)
    expect(screen.getByText(/Aucune pièce requise/i)).toBeInTheDocument()
  })

  it('renders items and progress', () => {
    render(
      <ChecklistViewer
        items={[
          { code: 'ID', libelle: 'Pièce d’identité' },
          { code: 'PHOTO', libelle: 'Photo d’identité' },
        ]}
      />,
    )
    expect(screen.getByText('0/2 pièces validées')).toBeInTheDocument()
    expect(screen.getByText('Pièce d’identité')).toBeInTheDocument()
    expect(screen.getByText('ID')).toBeInTheDocument()
    expect(screen.getByText('Photo d’identité')).toBeInTheDocument()
    expect(screen.getAllByRole('checkbox')).toHaveLength(2)
    expect(screen.getAllByRole('checkbox')[0]).toBeDisabled()
  })
})
