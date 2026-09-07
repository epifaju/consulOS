import { apiClient } from '@/lib/apiClient'
import type { ApiResponse } from '@/types/api'
import type { NotificationItem, NotificationPage, UnreadCount } from '@/types/notification'

export const notificationService = {
  list: (page = 0, size = 20) =>
    apiClient.get<ApiResponse<NotificationPage>>('/notifications', {
      params: { page, size, sort: 'createdAt,desc' },
    }),

  unreadCount: () =>
    apiClient.get<ApiResponse<UnreadCount>>('/notifications/unread-count'),

  markRead: (id: string) =>
    apiClient.post<ApiResponse<NotificationItem>>(`/notifications/${id}/read`),

  markAllRead: () =>
    apiClient.post<ApiResponse<UnreadCount>>('/notifications/read-all'),
}
