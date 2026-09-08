import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { reseauService, type ReseauMembre } from '@/services/reseauService'

export function useReseauStatus(enabled = true) {
  return useQuery({
    queryKey: ['reseau', 'status'],
    queryFn: async () => {
      try {
        const response = await reseauService.status()
        return response.data.data
      } catch {
        return { enabled: false }
      }
    },
    enabled,
    retry: false,
  })
}

export function useMyReseaux(enabled = true) {
  return useQuery({
    queryKey: ['reseau', 'me'],
    queryFn: async () => {
      const response = await reseauService.me()
      return response.data.data
    },
    enabled,
    retry: false,
  })
}

export function useReseauMembres(reseauId: string | null, enabled = true) {
  return useQuery({
    queryKey: ['reseau', 'membres', reseauId],
    queryFn: async (): Promise<ReseauMembre[]> => {
      const response = await reseauService.listMembres(reseauId!)
      return response.data.data
    },
    enabled: enabled && Boolean(reseauId),
    retry: false,
  })
}

export function useAttachReseauPoste() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ reseauId, organisationCode }: { reseauId: string; organisationCode: string }) =>
      reseauService.attachPoste(reseauId, organisationCode),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['reseau'] })
      await queryClient.invalidateQueries({ queryKey: ['regional'] })
    },
  })
}

export function useDetachReseauPoste() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ reseauId, organisationId }: { reseauId: string; organisationId: string }) =>
      reseauService.detachPoste(reseauId, organisationId),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['reseau'] })
      await queryClient.invalidateQueries({ queryKey: ['regional'] })
    },
  })
}
