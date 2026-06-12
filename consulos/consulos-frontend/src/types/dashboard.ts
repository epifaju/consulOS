export interface Kpi {
  label: string
  valeur: number
  valeurPrecedente: number
  tendancePourcent: number
}

export interface DossierStats {
  totalOuverts: number
  totalEnCours: number
  totalValides: number
  totalRejetes: number
  delaiMoyenHeures: number
  dossiersEnRetard: number
}

export interface RdvStats {
  total: number
  confirmes: number
  presents: number
  absents: number
  tauxPresence: number
}

export interface DashboardStats {
  dossiersOuverts: Kpi
  enAttenteValidation: Kpi
  rdvJour: Kpi
  recettesJour: Kpi
  dossierStats: DossierStats
  rdvStats: RdvStats
  recettesTotalPeriode: number
}

export interface Alerte {
  type: string
  severite: string
  message: string
  count: number
  dossiersIds: string[]
}

export interface AgentCharge {
  agentId: string
  agentNom: string
  nbDossiersEnCours: number
  nbRdvAujourdhui: number
}

export interface DossierChartRow {
  typeService: string
  statut: string
  count: number
}

export interface RecetteChartPoint {
  date: string
  modePaiement: string
  montant: number
}

export type DashboardPeriode = 'SEMAINE' | 'MOIS' | 'TRIMESTRE'
