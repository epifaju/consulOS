import { z } from 'zod'

export const statutCountSchema = z.object({
  statut: z.string(),
  count: z.number(),
})

export const reportingMensuelSchema = z.object({
  annee: z.number(),
  mois: z.number(),
  dateDebut: z.string(),
  dateFin: z.string(),
  dossiersCrees: z.number(),
  dossiersParStatut: z.array(statutCountSchema),
  encaissements: z.object({
    montantPercuTotal: z.union([z.number(), z.string()]),
    nbTransactions: z.number(),
    parMode: z.array(
      z.object({
        mode: z.string(),
        count: z.number(),
        montant: z.union([z.number(), z.string()]),
      }),
    ),
    parService: z.array(
      z.object({
        typeService: z.string(),
        montantTotal: z.union([z.number(), z.string()]),
        nbTransactions: z.number(),
      }),
    ),
  }),
  rendezVous: z.object({
    total: z.number(),
    parStatut: z.array(statutCountSchema),
  }),
  documentsDeposes: z.number(),
  delaiMoyenHeures: z.union([z.number(), z.string()]),
})

export type ReportingMensuel = z.infer<typeof reportingMensuelSchema>
