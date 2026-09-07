import { useQuery } from '@tanstack/react-query'
import { usagerService } from '@/services/usagerService'

export function useUsagerDossiers(usagerId: string) {
  return useQuery({
    queryKey: ['usager-dossiers', usagerId],
    queryFn: async () => {
      const response = await usagerService.listDossiers(usagerId)
      return response.data.data
    },
    enabled: Boolean(usagerId),
  })
}

export function useUsagerActivity(usagerId: string) {
  return useQuery({
    queryKey: ['usager-activity', usagerId],
    queryFn: async () => {
      const response = await usagerService.listActivity(usagerId)
      return response.data.data
    },
    enabled: Boolean(usagerId),
  })
}

export function useUsagerDocuments(usagerId: string) {
  return useQuery({
    queryKey: ['usager-documents', usagerId],
    queryFn: async () => {
      const response = await usagerService.listDocuments(usagerId)
      return response.data.data
    },
    enabled: Boolean(usagerId),
  })
}
