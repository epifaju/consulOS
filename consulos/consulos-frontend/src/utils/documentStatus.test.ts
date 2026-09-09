import { describe, expect, it } from 'vitest'
import { formatFileSize, getStatutControleBadgeClass } from '@/utils/documentStatus'
import type { StatutControle } from '@/types/document'

describe('documentStatus', () => {
  it('maps statut controle badges', () => {
    expect(getStatutControleBadgeClass('RECU')).toContain('gray')
    expect(getStatutControleBadgeClass('LISIBLE')).toContain('blue')
    expect(getStatutControleBadgeClass('COMPLET')).toContain('green')
    expect(getStatutControleBadgeClass('A_CORRIGER')).toContain('orange')
    expect(getStatutControleBadgeClass('VALIDE')).toContain('green')
    expect(getStatutControleBadgeClass('UNKNOWN' as StatutControle)).toContain('gray')
  })

  it('formats file size with o / Ko / Mo units', () => {
    expect(formatFileSize(null)).toBe('—')
    expect(formatFileSize(512)).toBe('512 o')
    expect(formatFileSize(2048)).toBe('2.0 Ko')
    expect(formatFileSize(2 * 1024 * 1024)).toBe('2.0 Mo')
  })
})
