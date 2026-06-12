import { apiClient } from '@/lib/apiClient'
import type { ApiResponse } from '@/types/api'
import type {
  AgendaParams,
  Creneau,
  DisponibilitesParams,
  RdvAgendaItem,
  RdvCreateRequest,
  RdvDetail,
  RdvStatutUpdateRequest,
} from '@/types/rendezvous'

export const rdvService = {
  getDisponibilites: (params: DisponibilitesParams) =>
    apiClient.get<ApiResponse<Creneau[]>>('/rendezvous/disponibilites', { params }),

  getAgenda: (params: AgendaParams) =>
    apiClient.get<ApiResponse<RdvAgendaItem[]>>('/rendezvous/agenda', { params }),

  getById: (id: string) =>
    apiClient.get<ApiResponse<RdvDetail>>(`/rendezvous/${id}`),

  create: (dto: RdvCreateRequest) =>
    apiClient.post<ApiResponse<RdvDetail>>('/rendezvous', dto),

  updateStatut: (id: string, dto: RdvStatutUpdateRequest) =>
    apiClient.post<ApiResponse<RdvDetail>>(`/rendezvous/${id}/statut`, dto),
}
