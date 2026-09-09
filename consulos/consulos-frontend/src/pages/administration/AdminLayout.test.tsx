import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import { AdminLayout } from '@/pages/administration/AdminLayout'

function renderLayout(path = '/administration/users') {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path="/administration" element={<AdminLayout />}>
          <Route path="users" element={<div>Users outlet</div>} />
          <Route path="roles" element={<div>Roles outlet</div>} />
          <Route path="parametres" element={<div>Parametres outlet</div>} />
          <Route path="audit" element={<div>Audit outlet</div>} />
        </Route>
      </Routes>
    </MemoryRouter>,
  )
}

describe('AdminLayout', () => {
  it('renders title and tab navigation', () => {
    renderLayout()
    expect(screen.getByRole('heading', { name: /Administration/i })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /^Agents$/i })).toHaveAttribute(
      'href',
      '/administration/users',
    )
    expect(screen.getByRole('link', { name: /^Rôles$/i })).toHaveAttribute(
      'href',
      '/administration/roles',
    )
    expect(screen.getByRole('link', { name: /^Paramètres$/i })).toHaveAttribute(
      'href',
      '/administration/parametres',
    )
    expect(screen.getByRole('link', { name: /Journal d'audit/i })).toHaveAttribute(
      'href',
      '/administration/audit',
    )
    expect(screen.getByText('Users outlet')).toBeInTheDocument()
  })

  it('shows active tab content for roles', () => {
    renderLayout('/administration/roles')
    expect(screen.getByText('Roles outlet')).toBeInTheDocument()
  })
})
