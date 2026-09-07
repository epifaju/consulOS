import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { documentService } from '@/services/documentService'
import type { DocumentListParams, DocumentUploadParams, StatutControleUpdate } from '@/types/document'

export function useDocumentsList(params: DocumentListParams) {
  return useQuery({
    queryKey: ['documents-list', params],
    queryFn: async () => {
      const response = await documentService.list(params)
      return response.data.data
    },
  })
}

export function useDossierDocuments(dossierId: string) {
  return useQuery({
    queryKey: ['dossier-documents', dossierId],
    queryFn: async () => {
      const response = await documentService.listByDossier(dossierId)
      return response.data.data
    },
    enabled: Boolean(dossierId),
  })
}

function invalidateDocumentQueries(queryClient: ReturnType<typeof useQueryClient>, dossierId?: string) {
  void queryClient.invalidateQueries({ queryKey: ['documents-list'] })
  void queryClient.invalidateQueries({ queryKey: ['usager-documents'] })
  if (dossierId) {
    void queryClient.invalidateQueries({ queryKey: ['dossier-documents', dossierId] })
  }
}

export function useUploadDocument(dossierId?: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (params: DocumentUploadParams) => documentService.upload(params),
    onSuccess: () => {
      invalidateDocumentQueries(queryClient, dossierId)
    },
  })
}

export function useUpdateDocumentStatut(dossierId?: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, dto }: { id: string; dto: StatutControleUpdate }) =>
      documentService.updateStatut(id, dto),
    onSuccess: () => {
      invalidateDocumentQueries(queryClient, dossierId)
    },
  })
}

export function useDeleteDocument(dossierId?: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => documentService.delete(id),
    onSuccess: () => {
      invalidateDocumentQueries(queryClient, dossierId)
    },
  })
}
