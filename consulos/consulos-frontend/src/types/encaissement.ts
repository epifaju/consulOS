export type ModePaiement = 'ESPECES' | 'CARTE' | 'VIREMENT' | 'EXTERNE' | 'EXONERE'

export type StatutEncaissement = 'DU' | 'PARTIEL' | 'PAYE' | 'EXONERE' | 'REMBOURSE'

export interface EncaissementSummary {
  id: string
  numeroRecu: string
  usagerId: string
  usagerNom: string | null
  usagerPrenom: string | null
  dossierId: string | null
  dossierReference: string | null
  typeServiceLibelle: string | null
  montantTotal: number
  montantPercu: number | null
  devise: string
  modePaiement: ModePaiement | null
  statut: StatutEncaissement
  createdAt: string
}

export interface EncaissementDetail extends EncaissementSummary {
  usagerNumero: string
  datePaiement: string | null
  caissierId: string | null
  caissierNom: string | null
  notes: string | null
  updatedAt: string
}

export interface EncaissementListParams {
  page?: number
  size?: number
  sort?: string
  statut?: StatutEncaissement
  modePaiement?: ModePaiement
  caissier?: string
  dateDebut?: string
  dateFin?: string
}

export interface EncaissementCreateRequest {
  usagerId: string
  dossierId?: string
  montantTotal: number
  devise?: string
  modePaiement?: ModePaiement
  notes?: string
}

export interface PaiementRequest {
  montant: number
  modePaiement: ModePaiement
  notes?: string
}

export interface ModeTotal {
  modePaiement: string
  nombreTransactions: number
  total: number
}

export interface JournalCaisse {
  date: string
  parMode: ModeTotal[]
  nombreTransactions: number
  montantTotalPercu: number
}

export interface Tarif {
  id: string
  typeServiceId: string | null
  libelle: string
  montant: number
  devise: string
  actif: boolean
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}
