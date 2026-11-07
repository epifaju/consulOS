import type { ModePaiement, StatutEncaissement } from '@/types/encaissement'

export function getStatutBadgeClass(statut: StatutEncaissement): string {
  switch (statut) {
    case 'DU':
      return 'bg-red-100 text-red-800 border-red-200'
    case 'PARTIEL':
      return 'bg-amber-100 text-amber-800 border-amber-200'
    case 'PAYE':
      return 'bg-green-100 text-green-800 border-green-200'
    case 'EXONERE':
      return 'bg-sky-100 text-sky-800 border-sky-200'
    case 'REMBOURSE':
      return 'bg-purple-100 text-purple-800 border-purple-200'
    default:
      return 'bg-gray-100 text-gray-800'
  }
}

export const MODES_PAIEMENT: ModePaiement[] = [
  'ESPECES',
  'CARTE',
  'VIREMENT',
  'EXTERNE',
  'EXONERE',
]

export const STATUTS_ENCAISSEMENT: StatutEncaissement[] = [
  'DU',
  'PARTIEL',
  'PAYE',
  'EXONERE',
  'REMBOURSE',
]

export function formatMontant(montant: number | null | undefined, devise = 'EUR'): string {
  const value = montant ?? 0
  return new Intl.NumberFormat('fr-FR', {
    style: 'currency',
    currency: devise,
  }).format(value)
}

export function resteDu(montantTotal: number, montantPercu: number | null | undefined): number {
  return Math.max(0, montantTotal - (montantPercu ?? 0))
}

/** Bornes ISO d'un jour civil local (évite le filtre UTC qui masque 0h–1h en CET). */
export function localDayIsoRange(isoDate: string): { dateDebut: string; dateFin: string } {
  const start = new Date(`${isoDate}T00:00:00`)
  const end = new Date(`${isoDate}T23:59:59.999`)
  return {
    dateDebut: start.toISOString(),
    dateFin: end.toISOString(),
  }
}
