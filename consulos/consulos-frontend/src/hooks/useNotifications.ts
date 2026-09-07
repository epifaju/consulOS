import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { notificationService } from '@/services/notificationService'

export const notificationKeys = {
  all: ['notifications'] as const,
  list: (page: number) => ['notifications', 'list', page] as const,
  unread: ['notifications', 'unread'] as const,
}


export function useNotifications(page = 0) {
  return useQuery({
    queryKey: notificationKeys.list(page),
    queryFn: async () => {
      const response = await notificationService.list(page)
      return response.data.data
    },
  })
}

export function useUnreadNotificationCount(enabled = true) {
  return useQuery({
    queryKey: notificationKeys.unread,
    queryFn: async () => {
      const response = await notificationService.unreadCount()
      return response.data.data.unreadCount
    },
    enabled,
    refetchInterval: 60_000,
  })
}

export function useMarkNotificationRead() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => notificationService.markRead(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: notificationKeys.all })
    },
  })
}

export function useMarkAllNotificationsRead() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: () => notificationService.markAllRead(),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: notificationKeys.all })
    },
  })
}
