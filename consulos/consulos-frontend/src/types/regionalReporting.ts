import { z } from 'zod'
import { reportingMensuelSchema } from '@/types/reporting'

export const regionalPosteMensuelSchema = z.object({
  organisationId: z.string(),
  code: z.string(),
  nom: z.string(),
  dossiersCrees: z.number(),
  montantPercu: z.union([z.number(), z.string()]),
  nbEncaissements: z.number(),
  rdvTotal: z.number(),
})

export const regionalReportingMensuelSchema = reportingMensuelSchema.extend({
  reseauId: z.string().nullable().optional(),
  postesCount: z.number(),
  parPoste: z.array(regionalPosteMensuelSchema),
})

export type RegionalReportingMensuel = z.infer<typeof regionalReportingMensuelSchema>
export type RegionalPosteMensuel = z.infer<typeof regionalPosteMensuelSchema>
