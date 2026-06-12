import type { StatutControle } from '@/types/document'

export function getStatutControleBadgeClass(statut: StatutControle): string {
  switch (statut) {
    case 'RECU':
      return 'bg-gray-200 text-gray-800'
    case 'LISIBLE':
      return 'bg-blue-100 text-blue-800'
    case 'COMPLET':
      return 'bg-green-50 text-green-700'
    case 'A_CORRIGER':
      return 'bg-orange-100 text-orange-800'
    case 'VALIDE':
      return 'bg-green-100 text-success'
    default:
      return 'bg-gray-100 text-gray-800'
  }
}

export function formatFileSize(bytes: number | null): string {
  if (bytes == null) return '—'
  if (bytes < 1024) return `${bytes} o`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} Ko`
  return `${(bytes / (1024 * 1024)).toFixed(1)} Mo`
}
