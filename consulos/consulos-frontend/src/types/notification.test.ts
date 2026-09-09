import { describe, expect, it } from 'vitest'
import {
  notificationPageSchema,
  notificationSchema,
  unreadCountSchema,
} from '@/types/notification'

const validNotification = {
  id: '11111111-1111-1111-1111-111111111111',
  typeNotification: 'DOSSIER_ASSIGNED',
  titre: 'Dossier assigné',
  message: 'Un dossier vous a été assigné',
  lien: '/dossiers/1',
  lu: false,
  createdAt: '2026-09-09T10:00:00',
}

describe('notification schemas', () => {
  it('accepts a valid notification', () => {
    expect(notificationSchema.parse(validNotification)).toEqual(validNotification)
  })

  it('rejects invalid uuid', () => {
    const result = notificationSchema.safeParse({ ...validNotification, id: 'not-a-uuid' })
    expect(result.success).toBe(false)
  })

  it('accepts non-negative unread count only', () => {
    expect(unreadCountSchema.parse({ unreadCount: 3 })).toEqual({ unreadCount: 3 })
    expect(unreadCountSchema.safeParse({ unreadCount: -1 }).success).toBe(false)
    expect(unreadCountSchema.safeParse({ unreadCount: 1.5 }).success).toBe(false)
  })

  it('parses a notification page payload', () => {
    const page = {
      content: [validNotification],
      totalElements: 1,
      totalPages: 1,
      page: 0,
      size: 20,
      first: true,
      last: true,
    }
    expect(notificationPageSchema.parse(page).content).toHaveLength(1)
  })
})
