import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { portalService } from '@/services/portalService'
import { usePortalStore } from '@/stores/portalStore'

export const portalKeys = {
  dossier: ['portal', 'dossier'] as const,
  documents: ['portal', 'documents'] as const,
  disponibilites: (date: string) => ['portal', 'disponibilites', date] as const,
}

export function usePortalDossier(enabled: boolean) {
  return useQuery({
    queryKey: portalKeys.dossier,
    queryFn: async () => {
      const response = await portalService.getDossier()
      return response.data.data
    },
    enabled,
  })
}

export function usePortalDocuments(enabled: boolean) {
  return useQuery({
    queryKey: portalKeys.documents,
    queryFn: async () => {
      const response = await portalService.listDocuments()
      return response.data.data
    },
    enabled,
  })
}

export function usePortalDisponibilites(date: string, enabled: boolean) {
  return useQuery({
    queryKey: portalKeys.disponibilites(date),
    queryFn: async () => {
      const response = await portalService.disponibilites(date)
      return response.data.data
    },
    enabled: enabled && Boolean(date),
  })
}

export function useRequestPortalAccess() {
  return useMutation({
    mutationFn: ({ reference, email }: { reference: string; email: string }) =>
      portalService.requestAccess(reference, email),
  })
}

export function useOpenPortalSession() {
  const setSession = usePortalStore((s) => s.setSession)
  return useMutation({
    mutationFn: (token: string) => portalService.openSession(token),
    onSuccess: (response) => {
      const payload = response.data.data
      setSession(payload.sessionToken, payload.dossier)
    },
  })
}

export function usePortalUploadDocument() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({
      file,
      typeDocument,
      notes,
    }: {
      file: File
      typeDocument: string
      notes?: string
    }) => portalService.uploadDocument(file, typeDocument, notes),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: portalKeys.documents })
    },
  })
}

export function usePortalCreateRdv() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ dateHeure, notes }: { dateHeure: string; notes?: string }) =>
      portalService.createRendezVous(dateHeure, notes),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['portal', 'disponibilites'] })
    },
  })
}
