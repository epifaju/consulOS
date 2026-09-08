import { apiClient } from '@/lib/apiClient'
import type { ApiResponse } from '@/types/api'

export interface RegionalStats {
  reseauId: string | null
  postesCount: number
  dossiersOuverts: number
  enRevue: number
  rdvJour: number
  dossiersEnRetard: number
  recettesJour: number | string
  recettesPeriode: number | string
}

export interface RegionalPosteKpi {
  organisationId: string
  code: string
  nom: string
  dossiersOuverts: number
  enRevue: number
  rdvJour: number
  recettesJour: number | string
}

export interface RegionalAlerte {
  type: string
  severite: string
  message: string
  count: number
  posteCodes: string[]
  dossiersIds: string[]
}

export const regionalService = {
  stats: (reseauId?: string) =>
    apiClient.get<ApiResponse<RegionalStats>>('/regional/stats', {
      params: reseauId ? { reseauId } : undefined,
    }),

  postes: (reseauId?: string) =>
    apiClient.get<ApiResponse<RegionalPosteKpi[]>>('/regional/postes', {
      params: reseauId ? { reseauId } : undefined,
    }),

  alerts: (reseauId?: string) =>
    apiClient.get<ApiResponse<RegionalAlerte[]>>('/regional/alerts', {
      params: reseauId ? { reseauId } : undefined,
    }),

  posteStats: (organisationId: string, reseauId?: string) =>
    apiClient.get<ApiResponse<RegionalPosteKpi>>(`/regional/postes/${organisationId}/stats`, {
      params: reseauId ? { reseauId } : undefined,
    }),
}
