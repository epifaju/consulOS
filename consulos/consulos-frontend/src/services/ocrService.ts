import { apiClient } from '@/lib/apiClient'
import type { ApiResponse } from '@/types/api'

export interface DocumentOcrResult {
  documentId: string
  provider: string
  status: string
  extractedText: string | null
  errorMessage: string | null
  updatedAt: string | null
}

export const ocrService = {
  getDocumentOcr: (documentId: string) =>
    apiClient.get<ApiResponse<DocumentOcrResult>>(`/ocr/documents/${documentId}`),
}
