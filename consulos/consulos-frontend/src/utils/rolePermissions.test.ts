import { describe, expect, it } from 'vitest'
import {
  canAccessAdministration,
  canAccessNav,
  canAccessReseau,
  canAssignDossierAgent,
  canDeleteUsagers,
  canExportUsagerFichePdf,
  canExportUsagerRgpd,
  canViewDashboardRecettes,
  canViewDashboardSupervision,
  canWriteUsagers,
  type NavPath,
} from '@/utils/rolePermissions'

describe('rolePermissions', () => {
  it('returns false for undefined role on all checks', () => {
    expect(canWriteUsagers(undefined)).toBe(false)
    expect(canDeleteUsagers(undefined)).toBe(false)
    expect(canExportUsagerRgpd(undefined)).toBe(false)
    expect(canAssignDossierAgent(undefined)).toBe(false)
    expect(canViewDashboardSupervision(undefined)).toBe(false)
    expect(canViewDashboardRecettes(undefined)).toBe(false)
    expect(canAccessReseau(undefined)).toBe(false)
    expect(canAccessAdministration(undefined)).toBe(false)
    expect(canAccessNav('/usagers', undefined)).toBe(false)
  })

  it('allows agent to write usagers but not delete', () => {
    expect(canWriteUsagers('ROLE_AGENT')).toBe(true)
    expect(canDeleteUsagers('ROLE_AGENT')).toBe(false)
    expect(canDeleteUsagers('ROLE_ADMIN')).toBe(true)
  })

  it('aligns RGPD export and fiche PDF', () => {
    expect(canExportUsagerRgpd('ROLE_AGENT')).toBe(true)
    expect(canExportUsagerFichePdf('ROLE_SUPERVISEUR')).toBe(true)
    expect(canExportUsagerRgpd('ROLE_CAISSIER')).toBe(false)
  })

  it('gates dashboard and reseau by role', () => {
    expect(canViewDashboardSupervision('ROLE_AGENT')).toBe(false)
    expect(canViewDashboardSupervision('ROLE_SUPERVISEUR')).toBe(true)
    expect(canViewDashboardRecettes('ROLE_CAISSIER')).toBe(true)
    expect(canViewDashboardRecettes('ROLE_ACCUEIL')).toBe(false)
    expect(canAccessReseau('ROLE_ADMIN_RESEAU')).toBe(true)
    expect(canAccessReseau('ROLE_AGENT')).toBe(false)
  })

  it('restricts administration to ROLE_ADMIN', () => {
    expect(canAccessAdministration('ROLE_ADMIN')).toBe(true)
    expect(canAccessAdministration('ROLE_SUPERVISEUR')).toBe(false)
  })

  it('exposes nav matrix for staff roles', () => {
    const paths: NavPath[] = [
      '/',
      '/usagers',
      '/dossiers',
      '/rendezvous',
      '/documents',
      '/caisse',
      '/reporting',
      '/regional',
      '/administration',
    ]
    expect(canAccessNav('/', 'ROLE_ACCUEIL')).toBe(true)
    expect(canAccessNav('/caisse', 'ROLE_ACCUEIL')).toBe(false)
    expect(canAccessNav('/reporting', 'ROLE_SUPERVISEUR')).toBe(true)
    expect(canAccessNav('/regional', 'ROLE_ADMIN_RESEAU')).toBe(true)
    expect(canAccessNav('/administration', 'ROLE_AGENT')).toBe(false)
    for (const path of paths) {
      expect(canAccessNav(path, 'ROLE_ADMIN')).toBe(true)
    }
  })

  it('allows dossier assignment for agent/superviseur/admin', () => {
    expect(canAssignDossierAgent('ROLE_AGENT')).toBe(true)
    expect(canAssignDossierAgent('ROLE_CAISSIER')).toBe(false)
  })
})
