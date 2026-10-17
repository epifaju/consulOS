import { hasPermission, RBAC_NAV, type NavPath } from '@/utils/rbacMatrix'

export type { NavPath }

export function canWriteUsagers(role: string | undefined): boolean {
  return hasPermission(role, 'usagers:write')
}

export function canDeleteUsagers(role: string | undefined): boolean {
  return hasPermission(role, 'usagers:delete')
}

export function canExportUsagerRgpd(role: string | undefined): boolean {
  return hasPermission(role, 'usagers:rgpd-export')
}

export function canExportUsagerFichePdf(role: string | undefined): boolean {
  return canExportUsagerRgpd(role)
}

export function canAssignDossierAgent(role: string | undefined): boolean {
  return hasPermission(role, 'dossiers:assign')
}

export function canViewDashboardSupervision(role: string | undefined): boolean {
  return hasPermission(role, 'dashboard:read')
}

export function canViewDashboardRecettes(role: string | undefined): boolean {
  return hasPermission(role, 'dashboard:recettes')
}

export function canAccessReseau(role: string | undefined): boolean {
  return hasPermission(role, 'reseau:read')
}

export function canAccessNav(path: NavPath, role: string | undefined): boolean {
  if (role == null) {
    return false
  }
  return RBAC_NAV[path].includes(role)
}

export function canAccessAdministration(role: string | undefined): boolean {
  return canAccessNav('/administration', role)
}
