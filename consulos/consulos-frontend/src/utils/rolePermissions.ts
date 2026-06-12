/** Aligné sur @PreAuthorize des contrôleurs usagers (backend). */
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
