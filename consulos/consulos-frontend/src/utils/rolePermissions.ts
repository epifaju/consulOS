/**
 * Aligné sur @PreAuthorize des contrôleurs et MvpRoleAuthorizationReference (backend).
 * Voir SECURITY-MVP.md pour la matrice complète.
 */
const USAGER_WRITE_ROLES = ['ROLE_AGENT', 'ROLE_ADMIN'] as const
const USAGER_DELETE_ROLES = ['ROLE_ADMIN'] as const
const USAGER_RGPD_EXPORT_ROLES = ['ROLE_AGENT', 'ROLE_SUPERVISEUR', 'ROLE_ADMIN'] as const

export function canWriteUsagers(role: string | undefined): boolean {
  return role != null && (USAGER_WRITE_ROLES as readonly string[]).includes(role)
}

export function canDeleteUsagers(role: string | undefined): boolean {
  return role != null && (USAGER_DELETE_ROLES as readonly string[]).includes(role)
}

export function canExportUsagerRgpd(role: string | undefined): boolean {
  return role != null && (USAGER_RGPD_EXPORT_ROLES as readonly string[]).includes(role)
}

export function canExportUsagerFichePdf(role: string | undefined): boolean {
  return canExportUsagerRgpd(role)
}

const DOSSIER_ASSIGN_ROLES = ['ROLE_AGENT', 'ROLE_SUPERVISEUR', 'ROLE_ADMIN'] as const

export function canAssignDossierAgent(role: string | undefined): boolean {
  return role != null && (DOSSIER_ASSIGN_ROLES as readonly string[]).includes(role)
}

const DASHBOARD_SUPERVISION_ROLES = ['ROLE_SUPERVISEUR', 'ROLE_ADMIN'] as const
const DASHBOARD_RECETTES_ROLES = ['ROLE_CAISSIER', 'ROLE_SUPERVISEUR', 'ROLE_ADMIN'] as const

export function canViewDashboardSupervision(role: string | undefined): boolean {
  return role != null && (DASHBOARD_SUPERVISION_ROLES as readonly string[]).includes(role)
}

export function canViewDashboardRecettes(role: string | undefined): boolean {
  return role != null && (DASHBOARD_RECETTES_ROLES as readonly string[]).includes(role)
}

export type NavPath =
  | '/'
  | '/usagers'
  | '/dossiers'
  | '/rendezvous'
  | '/documents'
  | '/caisse'
  | '/administration'

/** Rôles autorisés à voir chaque entrée du menu (aligné sur les APIs de lecture). */
const NAV_ROLES: Record<NavPath, readonly string[]> = {
  '/': ['ROLE_ADMIN', 'ROLE_SUPERVISEUR', 'ROLE_AGENT', 'ROLE_CAISSIER', 'ROLE_ACCUEIL'],
  '/usagers': ['ROLE_ADMIN', 'ROLE_SUPERVISEUR', 'ROLE_AGENT', 'ROLE_CAISSIER', 'ROLE_ACCUEIL'],
  '/dossiers': ['ROLE_ADMIN', 'ROLE_SUPERVISEUR', 'ROLE_AGENT', 'ROLE_CAISSIER', 'ROLE_ACCUEIL'],
  '/rendezvous': ['ROLE_ADMIN', 'ROLE_SUPERVISEUR', 'ROLE_AGENT', 'ROLE_ACCUEIL'],
  '/documents': ['ROLE_ADMIN', 'ROLE_SUPERVISEUR', 'ROLE_AGENT', 'ROLE_CAISSIER'],
  '/caisse': ['ROLE_ADMIN', 'ROLE_SUPERVISEUR', 'ROLE_CAISSIER'],
  '/administration': ['ROLE_ADMIN'],
}

export function canAccessNav(path: NavPath, role: string | undefined): boolean {
  if (role == null) return false
  return NAV_ROLES[path].includes(role)
}

export function canAccessAdministration(role: string | undefined): boolean {
  return canAccessNav('/administration', role)
}
