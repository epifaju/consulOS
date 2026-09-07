import axios from 'axios'
import { usePortalStore } from '@/stores/portalStore'
import type { ApiResponse } from '@/types/api'
import type { PortalDossierSummary } from '@/stores/portalStore'

export interface PortalDocument {
  id: string
  nomOriginal: string
  typeDocument: string
  statutControle: string
  tailleOctets: number
  createdAt: string | null
}

export interface PortalCreneau {
  heureDebut: string
  heureFin: string
  placesRestantes: number
  quotaMax: number
}

export interface PortalSessionPayload {
  sessionToken: string
  dossier: PortalDossierSummary
}

export const portalApiClient = axios.create({
  baseURL: '/api/v1',
  withCredentials: false,
})

portalApiClient.interceptors.request.use((config) => {
  const token = usePortalStore.getState().sessionToken
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

export const portalService = {
  requestAccess: (reference: string, email: string) =>
    portalApiClient.post<ApiResponse<{ status: string }>>('/portal/access', { reference, email }),

  openSession: (token: string) =>
    portalApiClient.post<ApiResponse<PortalSessionPayload>>('/portal/session', { token }),

  getDossier: () =>
    portalApiClient.get<ApiResponse<PortalDossierSummary>>('/portal/dossier'),

  listDocuments: () =>
    portalApiClient.get<ApiResponse<PortalDocument[]>>('/portal/documents'),

  uploadDocument: (file: File, typeDocument: string, notes?: string) => {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('typeDocument', typeDocument)
    if (notes) formData.append('notes', notes)
    return portalApiClient.post<ApiResponse<PortalDocument>>('/portal/documents', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },

  disponibilites: (date: string) =>
    portalApiClient.get<ApiResponse<PortalCreneau[]>>('/portal/disponibilites', {
      params: { date },
    }),

  createRendezVous: (dateHeure: string, notes?: string) =>
    portalApiClient.post<ApiResponse<unknown>>('/portal/rendezvous', {
      dateHeure,
      dureeMinutes: 30,
      notes,
    }),
}
