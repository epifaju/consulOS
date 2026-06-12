import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { encaissementService } from '@/services/encaissementService'
import type {
  EncaissementCreateRequest,
  EncaissementListParams,
  PaiementRequest,
} from '@/types/encaissement'

export function useEncaissements(params: EncaissementListParams) {
  return useQuery({
    queryKey: ['encaissements', params],
    queryFn: async () => {
      const response = await encaissementService.list(params)
      return response.data.data
    },
  })
}

export function useEncaissement(id: string) {
  return useQuery({
    queryKey: ['encaissement', id],
    queryFn: async () => {
      const response = await encaissementService.getById(id)
      return response.data.data
    },
    enabled: Boolean(id),
  })
}

export function useJournalCaisse(date: string) {
  return useQuery({
    queryKey: ['journal-caisse', date],
    queryFn: async () => {
      const response = await encaissementService.getJournalCaisse(date)
      return response.data.data
    },
    enabled: Boolean(date),
  })
}

export function useTarifs() {
  return useQuery({
    queryKey: ['tarifs'],
    queryFn: async () => {
      const response = await encaissementService.listTarifs()
      return response.data.data
    },
  })
}

export function useCreateEncaissement() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (dto: EncaissementCreateRequest) => encaissementService.create(dto),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['encaissements'] })
      void queryClient.invalidateQueries({ queryKey: ['journal-caisse'] })
    },
  })
}

export function useEnregistrerPaiement(encaissementId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (dto: PaiementRequest) =>
      encaissementService.enregistrerPaiement(encaissementId, dto),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['encaissements'] })
      void queryClient.invalidateQueries({ queryKey: ['encaissement', encaissementId] })
      void queryClient.invalidateQueries({ queryKey: ['journal-caisse'] })
    },
  })
}
