import { describe, expect, it } from 'vitest'
import { reportingMensuelSchema } from '@/types/reporting'
import { regionalReportingMensuelSchema } from '@/types/regionalReporting'

const baseReporting = {
  annee: 2026,
  mois: 9,
  dateDebut: '2026-09-01',
  dateFin: '2026-09-30',
  dossiersCrees: 12,
  dossiersParStatut: [{ statut: 'VALIDE', count: 5 }],
  encaissements: {
    montantPercuTotal: '1250.50',
    nbTransactions: 8,
    parMode: [{ mode: 'ESPECES', count: 3, montant: 400 }],
    parService: [{ typeService: 'Passeport', montantTotal: 850, nbTransactions: 5 }],
  },
  rendezVous: {
    total: 20,
    parStatut: [{ statut: 'CONFIRME', count: 15 }],
  },
  documentsDeposes: 30,
  delaiMoyenHeures: 48,
}

describe('reportingMensuelSchema', () => {
  it('accepts number and string unions for montants', () => {
    expect(reportingMensuelSchema.parse(baseReporting).encaissements.montantPercuTotal).toBe(
      '1250.50',
    )
    expect(
      reportingMensuelSchema.parse({
        ...baseReporting,
        encaissements: { ...baseReporting.encaissements, montantPercuTotal: 1250.5 },
        delaiMoyenHeures: '36.5',
      }).delaiMoyenHeures,
    ).toBe('36.5')
  })

  it('rejects missing required fields', () => {
    const { annee: _annee, ...incomplete } = baseReporting
    expect(reportingMensuelSchema.safeParse(incomplete).success).toBe(false)
  })
})

describe('regionalReportingMensuelSchema', () => {
  it('extends mensuel with postes', () => {
    const regional = {
      ...baseReporting,
      reseauId: 'reseau-1',
      postesCount: 1,
      parPoste: [
        {
          organisationId: 'org-1',
          code: 'GNB-FR',
          nom: 'Paris',
          dossiersCrees: 4,
          montantPercu: 200,
          nbEncaissements: 2,
          rdvTotal: 3,
        },
      ],
    }
    expect(regionalReportingMensuelSchema.parse(regional).postesCount).toBe(1)
  })
})
