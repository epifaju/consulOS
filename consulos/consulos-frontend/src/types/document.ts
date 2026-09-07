export type StatutControle = 'RECU' | 'LISIBLE' | 'COMPLET' | 'A_CORRIGER' | 'VALIDE'

export type TypeDocument =
  | 'PIECE_IDENTITE'
  | 'JUSTIFICATIF'
  | 'PHOTO'
  | 'FORMULAIRE'
  | 'AUTRE'

export const DOCUMENT_TYPES: readonly TypeDocument[] = [
  'PIECE_IDENTITE',
  'JUSTIFICATIF',
  'PHOTO',
  'FORMULAIRE',
  'AUTRE',
] as const

export const STATUTS_CONTROLE: readonly StatutControle[] = [
  'RECU',
  'LISIBLE',
  'COMPLET',
  'A_CORRIGER',
  'VALIDE',
] as const

export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  page: number
  size: number
  first: boolean
  last: boolean
}

export interface DocumentListItem extends DocumentItem {
  dossierReference: string | null
  usagerNom: string | null
  usagerPrenom: string | null
}

export interface DocumentListParams {
  page?: number
  size?: number
  sort?: string
  typeDocument?: TypeDocument
  statutControle?: StatutControle[]
  search?: string
  dossierReference?: string
  dateDebut?: string
  dateFin?: string
}

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
