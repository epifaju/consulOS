import { apiClient } from '@/lib/apiClient'
import type { ApiResponse } from '@/types/api'
import type {
  DocumentItem,
  DocumentUploadParams,
  DownloadUrl,
  StatutControleUpdate,
} from '@/types/document'

export const documentService = {
  listByDossier: (dossierId: string) =>
    apiClient.get<ApiResponse<DocumentItem[]>>(`/dossiers/${dossierId}/documents`),

  getById: (id: string) =>
    apiClient.get<ApiResponse<DocumentItem>>(`/documents/${id}`),

  getDownloadUrl: (id: string) =>
    apiClient.get<ApiResponse<DownloadUrl>>(`/documents/${id}/download-url`),

  upload: ({ file, dossierId, usagerId, typeDocument, notes, onProgress }: DocumentUploadParams) => {
    const formData = new FormData()
    formData.append('file', file)
    if (dossierId) formData.append('dossierId', dossierId)
    if (usagerId) formData.append('usagerId', usagerId)
    formData.append('typeDocument', typeDocument)
    if (notes) formData.append('notes', notes)

    return apiClient.post<ApiResponse<DocumentItem>>('/documents/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: (event) => {
        if (event.total && onProgress) {
          onProgress(Math.round((event.loaded * 100) / event.total))
        }
      },
    })
  },

  updateStatut: (id: string, dto: StatutControleUpdate) =>
    apiClient.put<ApiResponse<DocumentItem>>(`/documents/${id}/statut-controle`, dto),

  delete: (id: string) => apiClient.delete(`/documents/${id}`),
}
