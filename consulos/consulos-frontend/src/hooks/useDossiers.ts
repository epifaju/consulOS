import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { dossierService } from '@/services/dossierService'
import type {
  DossierCreateRequest,
  DossierListParams,
  DossierTransitionRequest,
} from '@/types/dossier'

export function useDossiers(params: DossierListParams, enabled = true) {
  return useQuery({
    queryKey: ['dossiers', params],
    queryFn: async () => {
      const response = await dossierService.list(params)
      return response.data.data
    },
    enabled,
  })
}

export function useDossier(id: string) {
  return useQuery({
    queryKey: ['dossier', id],
    queryFn: async () => {
      const response = await dossierService.getById(id)
      return response.data.data
    },
    enabled: Boolean(id),
  })
}

export function useTypesService() {
  return useQuery({
    queryKey: ['types-service'],
    queryFn: async () => {
      const response = await dossierService.listTypesService()
      return response.data.data
    },
  })
}

export function useDossierHistorique(id: string) {
  return useQuery({
    queryKey: ['dossier-historique', id],
    queryFn: async () => {
      const response = await dossierService.getHistorique(id)
      return response.data.data
    },
    enabled: Boolean(id),
  })
}

export function useCreateDossier() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (dto: DossierCreateRequest) => dossierService.create(dto),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['dossiers'] })
    },
  })
}

export function useDossierTransition(id: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (dto: DossierTransitionRequest) => dossierService.transition(id, dto),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['dossier', id] })
      void queryClient.invalidateQueries({ queryKey: ['dossiers'] })
      void queryClient.invalidateQueries({ queryKey: ['dossier-historique', id] })
    },
  })
}

export function useAddDossierCommentaire(id: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (contenu: string) => dossierService.addCommentaire(id, contenu),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['dossier', id] })
    },
  })
}

export function useAssignableAgents() {
  return useQuery({
    queryKey: ['dossier-assign-agents'],
    queryFn: async () => {
      const response = await dossierService.listAssignableAgents()
      return response.data.data
    },
  })
}

export function useAssignDossierAgent(dossierId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (agentId: string) => dossierService.assignAgent(dossierId, agentId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['dossier', dossierId] })
      void queryClient.invalidateQueries({ queryKey: ['dossiers'] })
      void queryClient.invalidateQueries({ queryKey: ['dossier-historique', dossierId] })
    },
  })
}
