import { useMutation, useQuery } from '@tanstack/react-query'
import { downloadBlob, parseFilenameFromDisposition } from '@/lib/downloadBlob'
import { reportingService } from '@/services/reportingService'
import { reportingMensuelSchema } from '@/types/reporting'

export const reportingKeys = {
  mensuel: (annee: number, mois: number) => ['reporting', 'mensuel', annee, mois] as const,
}

export function useReportingMensuel(annee: number, mois: number, enabled = true) {
  return useQuery({
    queryKey: reportingKeys.mensuel(annee, mois),
    queryFn: async () => {
      const response = await reportingService.mensuel(annee, mois)
      return reportingMensuelSchema.parse(response.data.data)
    },
    enabled,
  })
}

export function useExportReportingExcel() {
  return useMutation({
    mutationFn: async ({ annee, mois }: { annee: number; mois: number }) => {
      const response = await reportingService.mensuelExcel(annee, mois)
      const filename = parseFilenameFromDisposition(
        response.headers['content-disposition'] as string | undefined,
        `reporting-mensuel-${annee}-${String(mois).padStart(2, '0')}.xlsx`,
      )
      downloadBlob(response.data, filename)
    },
  })
}

export function useExportReportingDossiersCsv() {
  return useMutation({
    mutationFn: async ({ annee, mois }: { annee: number; mois: number }) => {
      const response = await reportingService.dossiersCsv(annee, mois)
      const filename = parseFilenameFromDisposition(
        response.headers['content-disposition'] as string | undefined,
        `dossiers-${annee}-${String(mois).padStart(2, '0')}.csv`,
      )
      downloadBlob(response.data, filename)
    },
  })
}

export function useExportReportingRdvCsv() {
  return useMutation({
    mutationFn: async ({ annee, mois }: { annee: number; mois: number }) => {
      const response = await reportingService.rdvCsv(annee, mois)
      const filename = parseFilenameFromDisposition(
        response.headers['content-disposition'] as string | undefined,
        `rdv-${annee}-${String(mois).padStart(2, '0')}.csv`,
      )
      downloadBlob(response.data, filename)
    },
  })
}
