import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { Bell } from 'lucide-react'
import { Button } from '@/components/ui/button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import {
  useMarkAllNotificationsRead,
  useMarkNotificationRead,
  useNotifications,
  useUnreadNotificationCount,
} from '@/hooks/useNotifications'
import { useNotificationSocket } from '@/hooks/useNotificationSocket'
import { useAuthStore } from '@/stores/authStore'

export function NotificationBell() {
  const { t } = useTranslation('common')
  const isAuthenticated = Boolean(useAuthStore((s) => s.accessToken))
  useNotificationSocket(isAuthenticated)

  const unreadQuery = useUnreadNotificationCount(isAuthenticated)
  const listQuery = useNotifications(0)
  const markRead = useMarkNotificationRead()
  const markAll = useMarkAllNotificationsRead()

  const unread = unreadQuery.data ?? 0
  const items = listQuery.data?.content ?? []

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button variant="ghost" size="icon" aria-label={t('notifications')} className="relative">
          <Bell className="h-5 w-5 text-primary" />
          {unread > 0 && (
            <span className="absolute right-1 top-1 flex h-4 min-w-4 items-center justify-center rounded-full bg-danger px-1 text-[10px] font-semibold text-white">
              {unread > 99 ? '99+' : unread}
            </span>
          )}
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="w-80">
        <DropdownMenuLabel className="flex items-center justify-between gap-2">
          <span>{t('notifications')}</span>
          {unread > 0 && (
            <button
              type="button"
              className="text-accent text-xs font-medium hover:underline"
              onClick={() => markAll.mutate()}
              disabled={markAll.isPending}
            >
              {t('notificationPanel.markAllRead')}
            </button>
          )}
        </DropdownMenuLabel>
        <DropdownMenuSeparator />
        {listQuery.isLoading ? (
          <div className="text-muted-foreground px-2 py-4 text-center text-sm">{t('loading')}</div>
        ) : items.length === 0 ? (
          <div className="text-muted-foreground px-2 py-4 text-center text-sm">
            {t('notificationPanel.empty')}
          </div>
        ) : (
          items.slice(0, 8).map((item) => {
            const content = (
              <>
                <span className={`text-sm ${item.lu ? 'font-normal' : 'font-semibold'}`}>
                  {item.titre}
                </span>
                <span className="text-muted-foreground line-clamp-2 text-xs">{item.message}</span>
              </>
            )
            return (
              <DropdownMenuItem
                key={item.id}
                className="flex cursor-pointer flex-col items-start gap-0.5 py-2"
                onSelect={() => {
                  if (!item.lu) {
                    markRead.mutate(item.id)
                  }
                }}
              >
                {item.lien ? (
                  <Link to={item.lien} className="flex w-full flex-col items-start gap-0.5">
                    {content}
                  </Link>
                ) : (
                  content
                )}
              </DropdownMenuItem>
            )
          })
        )}
      </DropdownMenuContent>
    </DropdownMenu>
  )
}

export default NotificationBell
