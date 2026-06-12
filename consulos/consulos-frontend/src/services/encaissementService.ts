import { apiClient } from '@/lib/apiClient'
import type { ApiResponse } from '@/types/api'
import type {
  EncaissementCreateRequest,
  EncaissementDetail,
  EncaissementListParams,
  EncaissementSummary,
  JournalCaisse,
  PageResponse,
  PaiementRequest,
  Tarif,
} from '@/types/encaissement'

export const encaissementService = {
  list: (params: EncaissementListParams) =>
    apiClient.get<ApiResponse<PageResponse<EncaissementSummary>>>('/encaissements', { params }),

  getById: (id: string) =>
    apiClient.get<ApiResponse<EncaissementDetail>>(`/encaissements/${id}`),

  create: (dto: EncaissementCreateRequest) =>
    apiClient.post<ApiResponse<EncaissementDetail>>('/encaissements', dto),

  enregistrerPaiement: (id: string, dto: PaiementRequest) =>
    apiClient.post<ApiResponse<EncaissementDetail>>(`/encaissements/${id}/paiement`, dto),

  getJournalCaisse: (date: string) =>
    apiClient.get<ApiResponse<JournalCaisse>>('/encaissements/journal-caisse', { params: { date } }),

  getRecuPdf: (id: string) =>
    apiClient.get<Blob>(`/encaissements/${id}/recu/pdf`, { responseType: 'blob' }),

  listTarifs: () =>
    apiClient.get<ApiResponse<Tarif[]>>('/tarifs'),

  exportExcel: (dateDebut: string, dateFin?: string) =>
    apiClient.get<Blob>('/encaissements/export/excel', {
      params: { dateDebut, dateFin: dateFin ?? dateDebut },
      responseType: 'blob',
    }),
}
