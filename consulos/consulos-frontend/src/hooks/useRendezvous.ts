import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { rdvService } from '@/services/rdvService'
import type {
  AgendaParams,
  DisponibilitesParams,
  RdvCreateRequest,
  RdvStatutUpdateRequest,
} from '@/types/rendezvous'

export function useDisponibilites(params: DisponibilitesParams | null) {
  return useQuery({
    queryKey: ['rdv-disponibilites', params],
    queryFn: async () => {
      if (!params) return []
      const response = await rdvService.getDisponibilites(params)
      return response.data.data
    },
    enabled: Boolean(params?.date),
  })
}

export function useAgenda(params: AgendaParams) {
  return useQuery({
    queryKey: ['rdv-agenda', params],
    queryFn: async () => {
      const response = await rdvService.getAgenda(params)
      return response.data.data
    },
  })
}

export function useRdvDetail(id: string | null) {
  return useQuery({
    queryKey: ['rdv', id],
    queryFn: async () => {
      const response = await rdvService.getById(id ?? '')
      return response.data.data
    },
    enabled: Boolean(id),
  })
}

export function useCreateRdv() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (dto: RdvCreateRequest) => rdvService.create(dto),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['rdv-agenda'] })
      void queryClient.invalidateQueries({ queryKey: ['rdv-disponibilites'] })
    },
  })
}

export function useUpdateRdvStatut(id: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (dto: RdvStatutUpdateRequest) => rdvService.updateStatut(id, dto),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['rdv-agenda'] })
      void queryClient.invalidateQueries({ queryKey: ['rdv', id] })
    },
  })
}
