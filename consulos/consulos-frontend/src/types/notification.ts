import { z } from 'zod'

export const notificationSchema = z.object({
  id: z.string().uuid(),
  typeNotification: z.string(),
  titre: z.string(),
  message: z.string(),
  lien: z.string().nullable().optional(),
  lu: z.boolean(),
  createdAt: z.string().nullable().optional(),
})

export type NotificationItem = z.infer<typeof notificationSchema>

export const unreadCountSchema = z.object({
  unreadCount: z.number().int().nonnegative(),
})

export type UnreadCount = z.infer<typeof unreadCountSchema>

export const notificationPageSchema = z.object({
  content: z.array(notificationSchema),
  totalElements: z.number(),
  totalPages: z.number(),
  page: z.number(),
  size: z.number(),
  first: z.boolean(),
  last: z.boolean(),
})

export type NotificationPage = z.infer<typeof notificationPageSchema>
