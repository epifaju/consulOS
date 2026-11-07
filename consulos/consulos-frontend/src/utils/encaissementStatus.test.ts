import { describe, expect, it } from 'vitest'
import {
  formatMontant,
  getStatutBadgeClass,
  localDayIsoRange,
  MODES_PAIEMENT,
  resteDu,
  STATUTS_ENCAISSEMENT,
} from '@/utils/encaissementStatus'
import type { StatutEncaissement } from '@/types/encaissement'

describe('encaissementStatus', () => {
  it('exposes modes and statuts catalogues', () => {
    expect(MODES_PAIEMENT).toEqual(['ESPECES', 'CARTE', 'VIREMENT', 'EXTERNE', 'EXONERE'])
    expect(STATUTS_ENCAISSEMENT).toEqual(['DU', 'PARTIEL', 'PAYE', 'EXONERE', 'REMBOURSE'])
  })

  it('maps each statut to a distinct badge class', () => {
    const classes = STATUTS_ENCAISSEMENT.map((statut) => getStatutBadgeClass(statut))
    expect(new Set(classes).size).toBe(STATUTS_ENCAISSEMENT.length)
    expect(getStatutBadgeClass('PAYE')).toContain('green')
    expect(getStatutBadgeClass('DU')).toContain('red')
    expect(getStatutBadgeClass('UNKNOWN' as StatutEncaissement)).toContain('gray')
  })

  it('formats montant with fr-FR currency', () => {
    expect(formatMontant(86)).toMatch(/86[,.]00/)
    expect(formatMontant(null)).toMatch(/0[,.]00/)
    expect(formatMontant(undefined, 'XOF')).toMatch(/F\s*CFA|XOF/)
  })

  it('computes reste dû without going negative', () => {
    expect(resteDu(100, 40)).toBe(60)
    expect(resteDu(100, null)).toBe(100)
    expect(resteDu(50, 80)).toBe(0)
  })

  it('maps a local calendar day to ISO instants covering local midnight', () => {
    const { dateDebut, dateFin } = localDayIsoRange('2026-11-07')
    const start = new Date(dateDebut)
    const end = new Date(dateFin)
    expect(start.getFullYear()).toBe(2026)
    expect(start.getMonth()).toBe(10)
    expect(start.getDate()).toBe(7)
    expect(start.getHours()).toBe(0)
    expect(end.getDate()).toBe(7)
    expect(end.getHours()).toBe(23)
  })
})
