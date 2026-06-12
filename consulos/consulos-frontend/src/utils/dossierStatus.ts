import type { DossierStatut } from '@/types/dossier'

export const DOSSIER_STATUTS: DossierStatut[] = [
  'BROUILLON',
  'SOUMIS',
  'INCOMPLET',
  'EN_REVUE',
  'VALIDE',
  'REJETE',
  'CLOTURE',
]

export function getStatutBadgeClass(statut: DossierStatut): string {
  switch (statut) {
    case 'BROUILLON':
      return 'bg-gray-200 text-gray-800'
    case 'SOUMIS':
      return 'bg-blue-100 text-blue-800'
    case 'INCOMPLET':
      return 'bg-orange-100 text-orange-800'
    case 'EN_REVUE':
      return 'bg-purple-100 text-purple-800'
    case 'VALIDE':
      return 'bg-green-100 text-success'
    case 'REJETE':
      return 'bg-red-100 text-danger'
    case 'CLOTURE':
      return 'bg-gray-700 text-white'
    default:
      return 'bg-gray-100 text-gray-800'
  }
}

export function getStatutTimelineColor(statut: DossierStatut): string {
  switch (statut) {
    case 'BROUILLON':
      return 'bg-gray-400'
    case 'SOUMIS':
      return 'bg-blue-500'
    case 'INCOMPLET':
      return 'bg-orange-500'
    case 'EN_REVUE':
      return 'bg-purple-500'
    case 'VALIDE':
      return 'bg-success'
    case 'REJETE':
      return 'bg-danger'
    case 'CLOTURE':
      return 'bg-gray-700'
    default:
      return 'bg-gray-400'
  }
}
