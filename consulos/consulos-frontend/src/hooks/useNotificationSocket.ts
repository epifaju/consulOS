import { useEffect } from 'react'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { useQueryClient } from '@tanstack/react-query'
import { toast } from '@/hooks/use-toast'
import { notificationKeys } from '@/hooks/useNotifications'
import { useAuthStore } from '@/stores/authStore'
import type { NotificationItem } from '@/types/notification'

/**
 * Maintient une connexion STOMP (/api/v1/ws) tant que l'utilisateur est authentifié.
 */
export function useNotificationSocket(enabled: boolean): void {
  const queryClient = useQueryClient()
  const accessToken = useAuthStore((s) => s.accessToken)

  useEffect(() => {
    if (!enabled || !accessToken) {
      return
    }

    const client = new Client({
      webSocketFactory: () => new SockJS('/api/v1/ws'),
      connectHeaders: {
        Authorization: `Bearer ${accessToken}`,
      },
      reconnectDelay: 5_000,
      heartbeatIncoming: 10_000,
      heartbeatOutgoing: 10_000,
      onConnect: () => {
        client.subscribe('/user/queue/notifications', (frame) => {
          try {
            const payload = JSON.parse(frame.body) as NotificationItem
            void queryClient.invalidateQueries({ queryKey: notificationKeys.all })
            toast({
              title: payload.titre,
              description: payload.message,
            })
          } catch {
            void queryClient.invalidateQueries({ queryKey: notificationKeys.unread })
          }
        })
      },
    })

    client.activate()

    return () => {
      void client.deactivate()
    }
  }, [accessToken, enabled, queryClient])
}
