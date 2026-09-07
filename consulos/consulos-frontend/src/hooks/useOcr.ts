import { useQuery } from '@tanstack/react-query'
import { ocrService, type DocumentOcrResult } from '@/services/ocrService'

export function useDocumentOcr(documentId: string | null, enabled = true) {
  return useQuery({
    queryKey: ['ocr', 'document', documentId],
    queryFn: async (): Promise<DocumentOcrResult | null> => {
      try {
        const response = await ocrService.getDocumentOcr(documentId!)
        return response.data.data
      } catch {
        return null
      }
    },
    enabled: enabled && Boolean(documentId),
    retry: false,
  })
}
