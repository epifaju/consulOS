import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { NotificationBell } from '@/components/notifications/NotificationBell'
import type { NotificationItem } from '@/types/notification'

const { unreadState, listState, markReadMutate, markAllMutate } = vi.hoisted(() => ({
  unreadState: { data: 0 as number | undefined },
  listState: {
    data: null as {
      content: NotificationItem[]
      totalElements: number
      totalPages: number
      page: number
      size: number
      first: boolean
      last: boolean
    } | null,
    isLoading: false,
  },
  markReadMutate: vi.fn(),
  markAllMutate: vi.fn(),
}))

vi.mock('@/hooks/useNotifications', () => ({
  useUnreadNotificationCount: () => unreadState,
  useNotifications: () => listState,
  useMarkNotificationRead: () => ({ mutate: markReadMutate, isPending: false }),
  useMarkAllNotificationsRead: () => ({ mutate: markAllMutate, isPending: false }),
}))

vi.mock('@/hooks/useNotificationSocket', () => ({
  useNotificationSocket: vi.fn(),
}))

vi.mock('@/stores/authStore', () => ({
  useAuthStore: (selector: (s: { accessToken: string | null }) => unknown) =>
    selector({ accessToken: 'token' }),
}))

const unreadItem = {
  id: '11111111-1111-1111-1111-111111111111',
  typeNotification: 'INFO',
  titre: 'Nouveau RDV',
  message: 'Un rendez-vous a été créé',
  lien: '/rendezvous',
  lu: false,
  createdAt: '2024-06-01T10:00:00Z',
} satisfies NotificationItem

function renderBell() {
  return render(
    <MemoryRouter>
      <NotificationBell />
    </MemoryRouter>,
  )
}

describe('NotificationBell', () => {
  beforeEach(() => {
    unreadState.data = 0
    listState.data = {
      content: [],
      totalElements: 0,
      totalPages: 0,
      page: 0,
      size: 20,
      first: true,
      last: true,
    }
    listState.isLoading = false
    markReadMutate.mockReset()
    markAllMutate.mockReset()
  })

  it('shows empty state without badge', async () => {
    const user = userEvent.setup()
    renderBell()
    await user.click(screen.getByRole('button', { name: /Notifications/i }))
    expect(screen.getByText(/Aucune notification/i)).toBeInTheDocument()
    expect(screen.queryByText('2')).not.toBeInTheDocument()
  })

  it('shows unread badge and marks all read', async () => {
    unreadState.data = 2
    listState.data = {
      content: [unreadItem],
      totalElements: 1,
      totalPages: 1,
      page: 0,
      size: 20,
      first: true,
      last: true,
    }
    const user = userEvent.setup()
    renderBell()
    expect(screen.getByText('2')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: /Notifications/i }))
    expect(screen.getByText('Nouveau RDV')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: /Tout marquer lu/i }))
    expect(markAllMutate).toHaveBeenCalled()
  })

  it('marks notification as read on select', async () => {
    unreadState.data = 1
    listState.data = {
      content: [{ ...unreadItem, lien: null }],
      totalElements: 1,
      totalPages: 1,
      page: 0,
      size: 20,
      first: true,
      last: true,
    }
    const user = userEvent.setup()
    renderBell()
    await user.click(screen.getByRole('button', { name: /Notifications/i }))
    await user.click(screen.getByRole('menuitem'))
    expect(markReadMutate).toHaveBeenCalledWith(unreadItem.id)
  })

  it('caps badge at 99+', () => {
    unreadState.data = 120
    renderBell()
    expect(screen.getByText('99+')).toBeInTheDocument()
  })
})
