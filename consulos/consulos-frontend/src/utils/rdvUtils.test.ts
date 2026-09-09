import { describe, expect, it } from 'vitest'
import {
  formatDateISO,
  getRdvStatutBadgeClass,
  getWeekDays,
  getWeekStart,
  TIME_SLOTS,
} from '@/utils/rdvUtils'
import type { RdvStatut } from '@/types/rendezvous'

describe('rdvUtils', () => {
  it('maps rdv statut badges and falls back for unknown', () => {
    expect(getRdvStatutBadgeClass('CONFIRME')).toContain('blue')
    expect(getRdvStatutBadgeClass('PRESENT')).toContain('green')
    expect(getRdvStatutBadgeClass('ANNULE')).toContain('red')
    expect(getRdvStatutBadgeClass('UNKNOWN' as RdvStatut)).toContain('gray')
  })

  it('returns Monday as week start including Sunday input', () => {
    const sunday = new Date(2026, 8, 13, 15, 0, 0)
    const monday = getWeekStart(sunday)
    expect(monday.getDay()).toBe(1)
    expect(monday.getHours()).toBe(0)
    expect(monday.getFullYear()).toBe(2026)
    expect(monday.getMonth()).toBe(8)
    expect(monday.getDate()).toBe(7)
  })

  it('returns five weekdays from week start', () => {
    const weekStart = getWeekStart(new Date(2026, 8, 9, 10, 0, 0))
    const days = getWeekDays(weekStart)
    expect(days).toHaveLength(5)
    expect(days.map((d) => d.getDay())).toEqual([1, 2, 3, 4, 5])
  })

  it('builds half-hour TIME_SLOTS from 08:00 to 17:30', () => {
    expect(TIME_SLOTS).toHaveLength(20)
    expect(TIME_SLOTS[0]).toBe('08:00')
    expect(TIME_SLOTS[1]).toBe('08:30')
    expect(TIME_SLOTS.at(-1)).toBe('17:30')
  })

  it('formats date as ISO yyyy-mm-dd', () => {
    expect(formatDateISO(new Date('2026-03-15T12:00:00.000Z'))).toBe('2026-03-15')
  })
})
