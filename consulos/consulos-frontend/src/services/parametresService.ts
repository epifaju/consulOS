import { apiClient } from '@/lib/apiClient'
import type { ApiResponse } from '@/types/api'

export interface ConfigCreneau {
  id: string
  typeServiceId: string | null
  jourSemaine: number
  heureDebut: string
  heureFin: string
  dureeMinutes: number
  quotaMax: number
  actif: boolean
}

export interface JourFermeture {
  id: string
  dateFermeture: string
  motif: string | null
}

export const parametresService = {
  listCreneaux: () =>
    apiClient.get<ApiResponse<ConfigCreneau[]>>('/parametres/creneaux'),

  listJoursFermeture: () =>
    apiClient.get<ApiResponse<JourFermeture[]>>('/parametres/jours-fermeture'),
}
