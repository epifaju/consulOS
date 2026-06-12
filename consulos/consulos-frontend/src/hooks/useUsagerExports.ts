import { useMutation } from '@tanstack/react-query'
import { downloadBlob, parseFilenameFromDisposition } from '@/lib/downloadBlob'
import { usagerService } from '@/services/usagerService'

export function useExportUsagerRgpd() {
  return useMutation({
    mutationFn: async (usagerId: string) => {
      const response = await usagerService.exportRgpd(usagerId)
      const filename = parseFilenameFromDisposition(
        response.headers['content-disposition'] as string | undefined,
        `rgpd-${usagerId}.json`,
      )
      downloadBlob(response.data, filename)
    },
  })
}
