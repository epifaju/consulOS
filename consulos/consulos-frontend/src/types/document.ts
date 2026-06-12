export type StatutControle = 'RECU' | 'LISIBLE' | 'COMPLET' | 'A_CORRIGER' | 'VALIDE'

export type TypeDocument =
  | 'PIECE_IDENTITE'
  | 'JUSTIFICATIF'
  | 'PHOTO'
  | 'FORMULAIRE'
  | 'AUTRE'

export interface DocumentItem {
  id: string
  dossierId: string | null
  usagerId: string | null
  nomFichier: string
  nomOriginal: string
  typeDocument: string
  mimeType: string | null
  tailleOctets: number | null
  statutControle: StatutControle
  version: number
  notes: string | null
  createdAt: string
}

export interface DownloadUrl {
  url: string
  expiresAt: string
}

export interface DocumentUploadParams {
  file: File
  dossierId?: string
  usagerId?: string
  typeDocument: TypeDocument
  notes?: string
  onProgress?: (percent: number) => void
}

export interface StatutControleUpdate {
  statut: StatutControle
  notes?: string
}
