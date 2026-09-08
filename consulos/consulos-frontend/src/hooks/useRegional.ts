import { useQuery } from '@tanstack/react-query'
import { regionalService } from '@/services/regionalService'

export function useRegionalStats(reseauId: string | null | undefined, enabled = true) {
  return useQuery({
    queryKey: ['regional', 'stats', reseauId ?? 'all'],
    queryFn: async () => {
      const response = await regionalService.stats(reseauId ?? undefined)
      return response.data.data
    },
    enabled,
    retry: false,
  })
}

export function useRegionalPostes(reseauId: string | null | undefined, enabled = true) {
  return useQuery({
    queryKey: ['regional', 'postes', reseauId ?? 'all'],
    queryFn: async () => {
      const response = await regionalService.postes(reseauId ?? undefined)
      return response.data.data
    },
    enabled,
    retry: false,
  })
}

export function useRegionalPosteStats(
  organisationId: string | null,
  reseauId: string | null | undefined,
  enabled = true,
) {
  return useQuery({
    queryKey: ['regional', 'poste', organisationId, reseauId ?? 'all'],
    queryFn: async () => {
      const response = await regionalService.posteStats(organisationId!, reseauId ?? undefined)
      return response.data.data
    },
    enabled: enabled && Boolean(organisationId),
    retry: false,
  })
}

export function useRegionalAlerts(reseauId: string | null | undefined, enabled = true) {
  return useQuery({
    queryKey: ['regional', 'alerts', reseauId ?? 'all'],
    queryFn: async () => {
      const response = await regionalService.alerts(reseauId ?? undefined)
      return response.data.data
    },
    enabled,
    retry: false,
  })
}
