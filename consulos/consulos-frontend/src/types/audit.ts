export interface AuditLog {
  id: string
  organisationId: string
  userId: string | null
  userNom: string | null
  userPrenom: string | null
  action: string
  entiteType: string | null
  entiteId: string | null
  ancienEtat: string | null
  nouvelEtat: string | null
  ipAddress: string | null
  userAgent: string | null
  createdAt: string
}

export interface AuditLogListParams {
  page?: number
  size?: number
  action?: string
  entiteType?: string
  userId?: string
  dateDebut?: string
  dateFin?: string
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}
