import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { documentService } from '@/services/documentService'
import type { DocumentUploadParams, StatutControleUpdate } from '@/types/document'

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

export function useUploadDocument(dossierId?: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (params: DocumentUploadParams) => documentService.upload(params),
    onSuccess: () => {
      if (dossierId) {
        void queryClient.invalidateQueries({ queryKey: ['dossier-documents', dossierId] })
      }
    },
  })
}

export function useUpdateDocumentStatut(dossierId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, dto }: { id: string; dto: StatutControleUpdate }) =>
      documentService.updateStatut(id, dto),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['dossier-documents', dossierId] })
    },
  })
}

export function useDeleteDocument(dossierId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => documentService.delete(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['dossier-documents', dossierId] })
    },
  })
}
