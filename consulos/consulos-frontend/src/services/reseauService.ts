import { apiClient } from '@/lib/apiClient'
import type { ApiResponse } from '@/types/api'

export interface ReseauPoste {
  organisationId: string
  code: string
  nom: string
  paysCode: string
  actif: boolean | null
}

export interface ReseauSummary {
  id: string
  code: string
  nom: string
  description: string | null
  roleReseau: string | null
  postes: ReseauPoste[]
}

export interface ReseauMe {
  reseaux: ReseauSummary[]
}

export interface ReseauMembre {
  userId: string
  email: string
  nom: string
  prenom: string
  roleReseau: string
  organisationId: string
}

export const reseauService = {
  status: () => apiClient.get<ApiResponse<{ enabled: boolean }>>('/reseau/status'),

  me: () => apiClient.get<ApiResponse<ReseauMe>>('/reseau/me'),

  listPostes: (reseauId: string) =>
    apiClient.get<ApiResponse<ReseauPoste[]>>(`/reseau/${reseauId}/postes`),

  listMembres: (reseauId: string) =>
    apiClient.get<ApiResponse<ReseauMembre[]>>(`/reseau/${reseauId}/membres`),

  attachPoste: (reseauId: string, organisationCode: string) =>
    apiClient.post<ApiResponse<ReseauPoste>>(`/reseau/${reseauId}/postes`, { organisationCode }),

  detachPoste: (reseauId: string, organisationId: string) =>
    apiClient.delete<ApiResponse<null>>(`/reseau/${reseauId}/postes/${organisationId}`),
}
