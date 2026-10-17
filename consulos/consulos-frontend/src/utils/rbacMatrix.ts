/**
 * Matrice RBAC unique (Vague 3) — miroir de com.consulos.rbac.MvpRbacMatrix.
 * Les gardes UI dérivent uniquement de cette table.
 */
export const RBAC_PERMISSIONS: Record<string, readonly string[]> = {
  ROLE_ADMIN: [
    'usagers:read',
    'usagers:write',
    'usagers:delete',
    'usagers:rgpd-export',
    'dossiers:read',
    'dossiers:write',
    'dossiers:validate',
    'dossiers:assign',
    'documents:read',
    'documents:write',
    'documents:delete',
    'rendezvous:read',
    'rendezvous:write',
    'encaissements:read',
    'encaissements:write',
    'encaissements:export',
    'dashboard:read',
    'dashboard:recettes',
    'reporting:read',
    'reseau:read',
    'reseau:write',
    'admin:read',
    'admin:write',
    'audit:read',
    'tarifs:write',
    'parametres:write',
    'notifications:read',
    'ocr:read',
  ],
  ROLE_SUPERVISEUR: [
    'usagers:read',
    'usagers:rgpd-export',
    'dossiers:read',
    'dossiers:validate',
    'dossiers:assign',
    'documents:read',
    'documents:write',
    'rendezvous:read',
    'encaissements:read',
    'encaissements:export',
    'dashboard:read',
    'dashboard:recettes',
    'reporting:read',
    'reseau:read',
    'audit:read',
    'parametres:read',
    'notifications:read',
    'ocr:read',
  ],
  ROLE_AGENT: [
    'usagers:read',
    'usagers:write',
    'usagers:rgpd-export',
    'dossiers:read',
    'dossiers:write',
    'dossiers:assign',
    'documents:read',
    'documents:write',
    'rendezvous:read',
    'rendezvous:write',
    'encaissements:read',
    'encaissements:write',
    'dashboard:limited',
    'notifications:read',
    'ocr:read',
  ],
  ROLE_CAISSIER: [
    'usagers:read',
    'dossiers:read',
    'documents:read',
    'encaissements:read',
    'encaissements:write',
    'encaissements:export',
    'dashboard:recettes',
    'notifications:read',
  ],
  ROLE_ACCUEIL: [
    'usagers:read',
    'dossiers:read',
    'rendezvous:read',
    'rendezvous:write',
    'notifications:read',
  ],
  ROLE_ADMIN_RESEAU: [
    'dashboard:read',
    'reporting:read',
    'reseau:read',
    'reseau:write',
    'notifications:read',
  ],
}

export type NavPath =
  | '/'
  | '/usagers'
  | '/dossiers'
  | '/rendezvous'
  | '/documents'
  | '/caisse'
  | '/reporting'
  | '/regional'
  | '/administration'

export const RBAC_NAV: Record<NavPath, readonly string[]> = {
  '/': ['ROLE_ADMIN', 'ROLE_SUPERVISEUR', 'ROLE_AGENT', 'ROLE_CAISSIER', 'ROLE_ACCUEIL', 'ROLE_ADMIN_RESEAU'],
  '/usagers': ['ROLE_ADMIN', 'ROLE_SUPERVISEUR', 'ROLE_AGENT', 'ROLE_CAISSIER', 'ROLE_ACCUEIL'],
  '/dossiers': ['ROLE_ADMIN', 'ROLE_SUPERVISEUR', 'ROLE_AGENT', 'ROLE_CAISSIER', 'ROLE_ACCUEIL'],
  '/rendezvous': ['ROLE_ADMIN', 'ROLE_SUPERVISEUR', 'ROLE_AGENT', 'ROLE_ACCUEIL'],
  '/documents': ['ROLE_ADMIN', 'ROLE_SUPERVISEUR', 'ROLE_AGENT', 'ROLE_CAISSIER'],
  '/caisse': ['ROLE_ADMIN', 'ROLE_SUPERVISEUR', 'ROLE_CAISSIER'],
  '/reporting': ['ROLE_ADMIN', 'ROLE_SUPERVISEUR'],
  '/regional': ['ROLE_ADMIN_RESEAU', 'ROLE_ADMIN', 'ROLE_SUPERVISEUR'],
  '/administration': ['ROLE_ADMIN'],
}

export function normalizeRole(role: string | undefined): string {
  if (!role) {
    return ''
  }
  return role.startsWith('ROLE_') ? role : `ROLE_${role}`
}

export function permissionsFor(role: string | undefined): readonly string[] {
  return RBAC_PERMISSIONS[normalizeRole(role)] ?? []
}

export function hasPermission(role: string | undefined, permission: string): boolean {
  return permissionsFor(role).includes(permission)
}
