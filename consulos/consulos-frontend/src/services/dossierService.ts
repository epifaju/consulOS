import { apiClient } from '@/lib/apiClient'
import type { ApiResponse } from '@/types/api'
import type {
  Commentaire,
  DossierCreateRequest,
  DossierDetail,
  DossierListParams,
  DossierSummary,
  DossierTransitionRequest,
  HistoriqueEntry,
  PageResponse,
  TypeService,
} from '@/types/dossier'

export const dossierService = {
  list: (params: DossierListParams) =>
    apiClient.get<ApiResponse<PageResponse<DossierSummary>>>('/dossiers', { params }),

  getById: (id: string) =>
    apiClient.get<ApiResponse<DossierDetail>>(`/dossiers/${id}`),

  create: (dto: DossierCreateRequest) =>
    apiClient.post<ApiResponse<DossierDetail>>('/dossiers', dto),

  transition: (id: string, dto: DossierTransitionRequest) =>
    apiClient.post<ApiResponse<DossierDetail>>(`/dossiers/${id}/transition`, dto),

  getHistorique: (id: string) =>
    apiClient.get<ApiResponse<HistoriqueEntry[]>>(`/dossiers/${id}/historique`),

  addCommentaire: (id: string, contenu: string) =>
    apiClient.post<ApiResponse<Commentaire>>(`/dossiers/${id}/commentaires`, { contenu }),

  listTypesService: () =>
    apiClient.get<ApiResponse<TypeService[]>>('/types-service'),
}
