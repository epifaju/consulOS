import { beforeEach, describe, expect, it } from 'vitest'
import { usePortalStore, type PortalDossierSummary } from '@/stores/portalStore'

const dossier = {
  id: 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
  reference: 'DOS-2026-00001',
  statut: 'SOUMIS',
  typeServiceLibelle: 'Passeport',
  dateSoumission: '2026-09-01',
  dateEcheance: null,
  usagerPrenom: 'Awa',
  usagerNom: 'Diallo',
} satisfies PortalDossierSummary

describe('portalStore', () => {
  beforeEach(() => {
    localStorage.clear()
    usePortalStore.setState({ sessionToken: null, dossier: null })
  })

  it('setSession stores token and dossier summary', () => {
    usePortalStore.getState().setSession('portal-token', dossier)

    const state = usePortalStore.getState()
    expect(state.sessionToken).toBe('portal-token')
    expect(state.dossier).toEqual(dossier)
  })

  it('clearSession resets portal state', () => {
    usePortalStore.getState().setSession('portal-token', dossier)
    usePortalStore.getState().clearSession()

    expect(usePortalStore.getState().sessionToken).toBeNull()
    expect(usePortalStore.getState().dossier).toBeNull()
  })

  it('persists under consulos-portal-session key', () => {
    usePortalStore.getState().setSession('portal-token', dossier)

    const raw = localStorage.getItem('consulos-portal-session')
    expect(raw).toBeTruthy()
    expect(raw).toContain('portal-token')
    expect(raw).toContain('DOS-2026-00001')
  })
})
