import { describe, expect, it } from 'vitest'
import {
  DOSSIER_STATUTS,
  getStatutBadgeClass,
  getStatutTimelineColor,
} from '@/utils/dossierStatus'
import type { DossierStatut } from '@/types/dossier'

describe('dossierStatus', () => {
  it('lists all dossier statuts in workflow order', () => {
    expect(DOSSIER_STATUTS).toEqual([
      'BROUILLON',
      'SOUMIS',
      'INCOMPLET',
      'EN_REVUE',
      'VALIDE',
      'REJETE',
      'CLOTURE',
    ])
  })

  it('maps badge and timeline colors for each statut', () => {
    for (const statut of DOSSIER_STATUTS) {
      expect(getStatutBadgeClass(statut)).toBeTruthy()
      expect(getStatutTimelineColor(statut)).toMatch(/^bg-/)
    }
    expect(getStatutBadgeClass('VALIDE')).toContain('green')
    expect(getStatutBadgeClass('REJETE')).toContain('red')
    expect(getStatutTimelineColor('EN_REVUE')).toContain('purple')
    expect(getStatutBadgeClass('UNKNOWN' as DossierStatut)).toContain('gray')
    expect(getStatutTimelineColor('UNKNOWN' as DossierStatut)).toBe('bg-gray-400')
  })
})
