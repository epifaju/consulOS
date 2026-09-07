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

export interface AgentOption {
  id: string
  nom: string
  prenom: string
  email: string
  roleCode: string | null
}

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

  listAssignableAgents: () =>
    apiClient.get<ApiResponse<AgentOption[]>>('/dossier-assign/agents'),

  assignAgent: (id: string, agentId: string) =>
    apiClient.post<ApiResponse<DossierDetail>>(`/dossiers/${id}/assign-agent`, { agentId }),
}
