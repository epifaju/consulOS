import { useMutation, useQuery } from '@tanstack/react-query'
import { downloadBlob, parseFilenameFromDisposition } from '@/lib/downloadBlob'
import { regionalReportingService } from '@/services/regionalReportingService'
import { regionalReportingMensuelSchema } from '@/types/regionalReporting'

export function useRegionalReportingMensuel(
  annee: number,
  mois: number,
  reseauId: string | null | undefined,
  enabled = true,
) {
  return useQuery({
    queryKey: ['regional', 'reporting', 'mensuel', annee, mois, reseauId ?? 'all'],
    queryFn: async () => {
      const response = await regionalReportingService.mensuel(annee, mois, reseauId ?? undefined)
      return regionalReportingMensuelSchema.parse(response.data.data)
    },
    enabled,
  })
}

export function useExportRegionalReportingExcel() {
  return useMutation({
    mutationFn: async ({
      annee,
      mois,
      reseauId,
    }: {
      annee: number
      mois: number
      reseauId?: string | null
    }) => {
      const response = await regionalReportingService.mensuelExcel(
        annee,
        mois,
        reseauId ?? undefined,
      )
      const filename = parseFilenameFromDisposition(
        response.headers['content-disposition'] as string | undefined,
        `reporting-regional-${annee}-${String(mois).padStart(2, '0')}.xlsx`,
      )
      downloadBlob(response.data, filename)
    },
  })
}
