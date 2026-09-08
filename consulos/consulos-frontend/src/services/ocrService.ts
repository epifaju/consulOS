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

export interface OcrFieldSuggestion {
  documentId: string
  provider: string
  status: string
  nom: string | null
  prenom: string | null
  dateNaissance: string | null
  lieuNaissance: string | null
  nationalite: string | null
  numeroDocument: string | null
  typeDocument: string | null
  confidence: number
}

export const ocrService = {
  getDocumentOcr: (documentId: string) =>
    apiClient.get<ApiResponse<DocumentOcrResult>>(`/ocr/documents/${documentId}`),

  getSuggestions: (documentId: string) =>
    apiClient.get<ApiResponse<OcrFieldSuggestion>>(`/ocr/documents/${documentId}/suggestions`),
}
