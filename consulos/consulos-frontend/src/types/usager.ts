export type TypeUsager = 'DIASPORA' | 'MANDATAIRE' | 'AUTRE'
export type TypeDocument = 'PASSEPORT' | 'CNI' | 'TITRE_SEJOUR' | 'AUTRE'

export interface UsagerSummary {
  id: string
  numero: string
  nom: string
  prenom: string
  nationalite: string | null
  typeUsager: TypeUsager
  telephone: string | null
  email: string | null
  actif: boolean
}

export interface UsagerDetail extends UsagerSummary {
  dateNaissance: string | null
  lieuNaissance: string | null
  adresse: string | null
  numeroDocument: string | null
  typeDocument: TypeDocument | null
  metadata: Record<string, unknown>
  organisationId: string
  createdAt: string
  updatedAt: string
}

export interface UsagerCreateRequest {
  nom: string
  prenom: string
  dateNaissance?: string
  lieuNaissance?: string
  nationalite?: string
  typeUsager?: TypeUsager
  telephone?: string
  email?: string
  adresse?: string
  numeroDocument?: string
  typeDocument?: TypeDocument
}

export interface UsagerUpdateRequest {
  nom?: string
  prenom?: string
  dateNaissance?: string
  lieuNaissance?: string
  nationalite?: string
  typeUsager?: TypeUsager
  telephone?: string
  email?: string
  adresse?: string
  numeroDocument?: string
  typeDocument?: TypeDocument
}

export interface DoublonGroup {
  similaires: UsagerSummary[]
  motif: string
}

export interface UsagerCreateResponse {
  usager: UsagerDetail
  doublonsPotentiels: DoublonGroup[]
}

export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  page: number
  size: number
  first: boolean
  last: boolean
}

export interface UsagerListParams {
  q?: string
  page?: number
  size?: number
  sort?: string
  typeUsager?: TypeUsager | ''
  nationalite?: string
}

export interface DoublonParams {
  nom: string
  dateNaissance: string
  nationalite: string
  excludeId?: string
}
