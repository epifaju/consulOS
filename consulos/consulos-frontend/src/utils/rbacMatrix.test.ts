import { describe, expect, it } from 'vitest'
import { hasPermission, permissionsFor, RBAC_NAV, RBAC_PERMISSIONS } from '@/utils/rbacMatrix'

describe('rbacMatrix', () => {
  it('defines six roles including reseau', () => {
    expect(Object.keys(RBAC_PERMISSIONS)).toEqual(
      expect.arrayContaining([
        'ROLE_ADMIN',
        'ROLE_SUPERVISEUR',
        'ROLE_AGENT',
        'ROLE_CAISSIER',
        'ROLE_ACCUEIL',
        'ROLE_ADMIN_RESEAU',
      ]),
    )
  })

  it('gives caissier usagers:read for the till', () => {
    expect(hasPermission('ROLE_CAISSIER', 'usagers:read')).toBe(true)
    expect(hasPermission('ROLE_CAISSIER', 'usagers:write')).toBe(false)
  })

  it('keeps agent without dashboard supervision', () => {
    expect(hasPermission('ROLE_AGENT', 'dashboard:read')).toBe(false)
    expect(hasPermission('ROLE_AGENT', 'dashboard:limited')).toBe(true)
  })

  it('returns empty permissions for unknown roles', () => {
    expect(permissionsFor('ROLE_UNKNOWN')).toEqual([])
    expect(hasPermission(undefined, 'admin:read')).toBe(false)
  })

  it('matches staff nav paths', () => {
    expect(Object.keys(RBAC_NAV)).toHaveLength(9)
    expect(RBAC_NAV['/administration']).toEqual(['ROLE_ADMIN'])
  })
})
