import { apiClient } from '@/lib/apiClient'
import type { ApiResponse } from '@/types/api'
import type {
  DocumentItem,
  DocumentListItem,
  DocumentListParams,
  DocumentUploadParams,
  DownloadUrl,
  PageResponse,
  StatutControleUpdate,
} from '@/types/document'

export const documentService = {
  list: (params: DocumentListParams) =>
    apiClient.get<ApiResponse<PageResponse<DocumentListItem>>>('/document-queries', { params }),

  listByDossier: (dossierId: string) =>
    apiClient.get<ApiResponse<DocumentItem[]>>(`/dossiers/${dossierId}/documents`),

  getById: (id: string) =>
    apiClient.get<ApiResponse<DocumentItem>>(`/documents/${id}`),

  getDownloadUrl: (id: string) =>
    apiClient.get<ApiResponse<DownloadUrl>>(`/documents/${id}/download-url`),

  /** Contenu binaire via l'API (évite le XML MinIO en cas de fichier absent). */
  getContent: (id: string, disposition: 'inline' | 'attachment' = 'inline') =>
    apiClient.get<Blob>(`/documents/${id}/content`, {
      params: { disposition },
      responseType: 'blob',
    }),

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

  replaceFile: (id: string, file: File) => {
    const formData = new FormData()
    formData.append('file', withInferredMime(file))
    return apiClient.post<ApiResponse<DocumentItem>>(`/documents/${id}/replace`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },

  updateStatut: (id: string, dto: StatutControleUpdate) =>
    apiClient.put<ApiResponse<DocumentItem>>(`/documents/${id}/statut-controle`, dto),

  delete: (id: string) => apiClient.delete(`/documents/${id}`),
}

function withInferredMime(file: File): File {
  if (file.type) {
    return file
  }
  const name = file.name.toLowerCase()
  if (name.endsWith('.pdf')) {
    return new File([file], file.name, { type: 'application/pdf' })
  }
  if (name.endsWith('.png')) {
    return new File([file], file.name, { type: 'image/png' })
  }
  if (name.endsWith('.jpg') || name.endsWith('.jpeg')) {
    return new File([file], file.name, { type: 'image/jpeg' })
  }
  if (name.endsWith('.webp')) {
    return new File([file], file.name, { type: 'image/webp' })
  }
  return file
}
