export type DossierStatut =
  | 'BROUILLON'
  | 'SOUMIS'
  | 'INCOMPLET'
  | 'EN_REVUE'
  | 'VALIDE'
  | 'REJETE'
  | 'CLOTURE'

export type DossierPriorite = 'URGENTE' | 'NORMALE' | 'FAIBLE'

export interface ChecklistItem {
  code: string
  libelle: string
}

export interface DossierSummary {
  id: string
  reference: string
  usagerId: string
  usagerNom: string
  usagerPrenom: string
  typeServiceLibelle: string
  statut: DossierStatut
  agentId: string | null
  agentNom: string | null
  priorite: DossierPriorite
  dateEcheance: string | null
}

export interface Commentaire {
  id: string
  auteurId: string | null
  auteurNom: string | null
  contenu: string
  createdAt: string
}

export interface DossierDetail {
  id: string
  reference: string
  usagerId: string
  usagerNom: string
  usagerPrenom: string
  typeServiceId: string
  typeServiceCode: string
  typeServiceLibelle: string
  statut: DossierStatut
  agentId: string | null
  agentNom: string | null
  priorite: DossierPriorite
  dateSoumission: string | null
  dateEcheance: string | null
  motifRejet: string | null
  notesInternes: string | null
  metadata: Record<string, unknown> | null
  checklist: ChecklistItem[] | null
  commentaires: Commentaire[]
  createdAt: string
  updatedAt: string
}

export interface TypeService {
  id: string
  code: string
  libelle: string
  delaiJours: number
  tarifDefaut: number | null
  checklist: ChecklistItem[] | null
}

export interface HistoriqueEntry {
  id: string
  statutDe: DossierStatut | null
  statutVers: DossierStatut
  commentaire: string | null
  userId: string | null
  userNom: string | null
  createdAt: string
}

export interface DossierListParams {
  page?: number
  size?: number
  sort?: string
  statut?: DossierStatut[]
  typeService?: string
  agentId?: string
  usagerId?: string
  priorite?: DossierPriorite
  dateDebut?: string
  dateFin?: string
}

export interface DossierCreateRequest {
  usagerId: string
  typeServiceId: string
  priorite?: DossierPriorite
  notesInternes?: string
}

export interface DossierTransitionRequest {
  statut: DossierStatut
  motif?: string
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}
