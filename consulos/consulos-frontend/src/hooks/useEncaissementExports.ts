import { useMutation } from '@tanstack/react-query'
import { downloadBlob, parseFilenameFromDisposition } from '@/lib/downloadBlob'
import { encaissementService } from '@/services/encaissementService'

export function useExportJournalExcel() {
  return useMutation({
    mutationFn: async ({ dateDebut, dateFin }: { dateDebut: string; dateFin?: string }) => {
      const response = await encaissementService.exportExcel(dateDebut, dateFin)
      const filename = parseFilenameFromDisposition(
        response.headers['content-disposition'] as string | undefined,
        `journal-caisse-${dateDebut}.xlsx`,
      )
      downloadBlob(response.data, filename)
    },
  })
}
