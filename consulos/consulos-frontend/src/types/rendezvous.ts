export type RdvStatut =
  | 'CONFIRME'
  | 'EN_ATTENTE'
  | 'PRESENT'
  | 'ABSENT'
  | 'ANNULE'
  | 'REPORTE'

export interface Creneau {
  heureDebut: string
  heureFin: string
  placesRestantes: number
  quotaMax: number
}

export interface RdvAgendaItem {
  id: string
  usagerId: string
  usagerNom: string
  usagerPrenom: string
  typeServiceId: string | null
  typeServiceLibelle: string | null
  agentId: string | null
  agentNom: string | null
  dateHeure: string
  dureeMinutes: number
  statut: RdvStatut
}

export interface RdvDetail {
  id: string
  usagerId: string
  usagerNom: string
  usagerPrenom: string
  usagerEmail: string | null
  dossierId: string | null
  dossierReference: string | null
  typeServiceId: string | null
  typeServiceLibelle: string | null
  agentId: string | null
  agentNom: string | null
  dateHeure: string
  dureeMinutes: number
  statut: RdvStatut
  motif: string | null
  notes: string | null
  rappelEnvoye: boolean
  createdAt: string
  updatedAt: string
}

export interface RdvCreateRequest {
  usagerId: string
  dateHeure: string
  typeServiceId?: string
  dossierId?: string
  agentId?: string
  dureeMinutes?: number
  notes?: string
}

export interface RdvStatutUpdateRequest {
  statut: RdvStatut
  motif?: string
}

export interface DisponibilitesParams {
  date: string
  typeServiceId?: string
}

export interface AgendaParams {
  dateDebut: string
  dateFin: string
  agentId?: string
}
