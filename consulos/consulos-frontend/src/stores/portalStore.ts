import { create } from 'zustand'
import { persist } from 'zustand/middleware'

export interface PortalDossierSummary {
  id: string
  reference: string
  statut: string
  typeServiceLibelle: string
  dateSoumission: string | null
  dateEcheance: string | null
  usagerPrenom: string
  usagerNom: string
}

interface PortalState {
  sessionToken: string | null
  dossier: PortalDossierSummary | null
  setSession: (token: string, dossier: PortalDossierSummary) => void
  clearSession: () => void
}

export const usePortalStore = create<PortalState>()(
  persist(
    (set) => ({
      sessionToken: null,
      dossier: null,
      setSession: (sessionToken, dossier) => set({ sessionToken, dossier }),
      clearSession: () => set({ sessionToken: null, dossier: null }),
    }),
    {
      name: 'consulos-portal-session',
      partialize: (state) => ({
        sessionToken: state.sessionToken,
        dossier: state.dossier,
      }),
    },
  ),
)
