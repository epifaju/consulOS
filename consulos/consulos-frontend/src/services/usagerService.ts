import { apiClient } from '@/lib/apiClient'
import type { ApiResponse } from '@/types/api'
import type {
  DoublonGroup,
  DoublonParams,
  PageResponse,
  UsagerActivityItem,
  UsagerCreateRequest,
  UsagerCreateResponse,
  UsagerDetail,
  UsagerDocumentSummary,
  UsagerLinkedDossier,
  UsagerListParams,
  UsagerSummary,
  UsagerUpdateRequest,
} from '@/types/usager'

export const usagerService = {
  list: (params: UsagerListParams) =>
    apiClient.get<ApiResponse<PageResponse<UsagerSummary>>>('/usagers', { params }),

  getById: (id: string) =>
    apiClient.get<ApiResponse<UsagerDetail>>(`/usagers/${id}`),

  create: (dto: UsagerCreateRequest) =>
    apiClient.post<ApiResponse<UsagerCreateResponse>>('/usagers', dto),

  update: (id: string, dto: UsagerUpdateRequest) =>
    apiClient.put<ApiResponse<UsagerDetail>>(`/usagers/${id}`, dto),

  delete: (id: string) => apiClient.delete(`/usagers/${id}`),

  search: (q: string) =>
    apiClient.get<ApiResponse<UsagerSummary[]>>('/usagers/search', { params: { q } }),

  doublons: (params: DoublonParams) =>
    apiClient.get<ApiResponse<DoublonGroup[]>>('/usagers/doublons', { params }),

  exportRgpd: (id: string) =>
    apiClient.get<Blob>(`/usagers/${id}/export-rgpd`, { responseType: 'blob' }),

  exportFichePdf: (id: string) =>
    apiClient.get<Blob>(`/usagers/${id}/fiche/pdf`, { responseType: 'blob' }),

  listDossiers: (id: string) =>
    apiClient.get<ApiResponse<UsagerLinkedDossier[]>>(`/usager-queries/${id}/dossiers`),

  listActivity: (id: string) =>
    apiClient.get<ApiResponse<UsagerActivityItem[]>>(`/usager-queries/${id}/activity`),

  listDocuments: (id: string) =>
    apiClient.get<ApiResponse<UsagerDocumentSummary[]>>(`/usager-queries/${id}/documents`),
}
