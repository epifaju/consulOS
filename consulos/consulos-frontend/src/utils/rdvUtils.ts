import type { RdvStatut } from '@/types/rendezvous'

export function getRdvStatutBadgeClass(statut: RdvStatut): string {
  switch (statut) {
    case 'CONFIRME':
      return 'bg-blue-100 text-blue-800 border-blue-200'
    case 'EN_ATTENTE':
      return 'bg-yellow-100 text-yellow-800 border-yellow-200'
    case 'PRESENT':
      return 'bg-green-100 text-success border-green-200'
    case 'ABSENT':
      return 'bg-orange-100 text-orange-800 border-orange-200'
    case 'ANNULE':
      return 'bg-red-100 text-danger border-red-200'
    case 'REPORTE':
      return 'bg-purple-100 text-purple-800 border-purple-200'
    default:
      return 'bg-gray-100 text-gray-800'
  }
}

export function getWeekStart(date: Date): Date {
  const d = new Date(date)
  const day = d.getDay()
  const diff = day === 0 ? -6 : 1 - day
  d.setDate(d.getDate() + diff)
  d.setHours(0, 0, 0, 0)
  return d
}

export function formatDateISO(date: Date): string {
  return date.toISOString().slice(0, 10)
}

export const TIME_SLOTS: string[] = Array.from({ length: 20 }, (_, i) => {
  const hour = 8 + Math.floor(i / 2)
  const minute = i % 2 === 0 ? '00' : '30'
  return `${String(hour).padStart(2, '0')}:${minute}`
})

export function getWeekDays(weekStart: Date): Date[] {
  return Array.from({ length: 5 }, (_, i) => {
    const d = new Date(weekStart)
    d.setDate(d.getDate() + i)
    return d
  })
}
