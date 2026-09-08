import { useState } from 'react'
import { Link, Outlet, useLocation } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import {
  BarChart3,
  Calendar,
  FileText,
  FolderOpen,
  LayoutDashboard,
  Menu,
  Network,
  Settings,
  Users,
  Wallet,
  X,
} from 'lucide-react'
import { cn } from '@/lib/utils'
import { useAuthStore } from '@/stores/authStore'
import { canAccessNav, type NavPath } from '@/utils/rolePermissions'
import { NotificationBell } from '@/components/notifications/NotificationBell'
import { Button } from '@/components/ui/button'
import { Separator } from '@/components/ui/separator'
import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { Sheet, SheetContent, SheetTrigger } from '@/components/ui/sheet'

const navItems: { to: NavPath; labelKey: string; icon: typeof LayoutDashboard }[] = [
  { to: '/', labelKey: 'nav.dashboard', icon: LayoutDashboard },
  { to: '/usagers', labelKey: 'nav.usagers', icon: Users },
  { to: '/dossiers', labelKey: 'nav.dossiers', icon: FolderOpen },
  { to: '/rendezvous', labelKey: 'nav.rendezvous', icon: Calendar },
  { to: '/documents', labelKey: 'nav.documents', icon: FileText },
  { to: '/caisse', labelKey: 'nav.caisse', icon: Wallet },
  { to: '/reporting', labelKey: 'nav.reporting', icon: BarChart3 },
  { to: '/regional', labelKey: 'nav.regional', icon: Network },
  { to: '/administration', labelKey: 'nav.administration', icon: Settings },
]

function SidebarNav({ onNavigate }: { onNavigate?: () => void }) {
  const { t } = useTranslation('common')
  const location = useLocation()
  const role = useAuthStore((state) => state.user?.role)
  const visibleItems = navItems.filter((item) => canAccessNav(item.to, role))

  return (
    <nav className="flex flex-col gap-1 p-4">
      {visibleItems.map(({ to, labelKey, icon: Icon }) => {
        const isActive = location.pathname === to || (to !== '/' && location.pathname.startsWith(to))
        return (
          <Link
            key={to}
            to={to}
            onClick={onNavigate}
            className={cn(
              'flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium transition-colors',
              isActive
                ? 'bg-primary text-white'
                : 'text-primary hover:bg-primary-lighter',
            )}
          >
            <Icon className="h-4 w-4" />
            {t(labelKey)}
          </Link>
        )
      })}
    </nav>
  )
}

function Breadcrumb() {
  const location = useLocation()
  const { t } = useTranslation('common')
  const segments = location.pathname.split('/').filter(Boolean)

  if (segments.length === 0) {
    return <span className="text-sm font-medium">{t('nav.dashboard')}</span>
  }

  return (
    <span className="text-sm font-medium capitalize">
      {segments.join(' / ')}
    </span>
  )
}

export function AppShell() {
  const { t } = useTranslation('common')
  const user = useAuthStore((state) => state.user)
  const logout = useAuthStore((state) => state.logout)
  const [mobileOpen, setMobileOpen] = useState(false)

  const initials = user
    ? `${user.prenom.charAt(0)}${user.nom.charAt(0)}`.toUpperCase()
    : 'CO'

  return (
    <div className="flex min-h-screen bg-surface">
      <aside className="hidden w-64 shrink-0 border-r bg-white lg:flex lg:flex-col">
        <div className="flex h-16 items-center px-6">
          <span className="text-lg font-bold text-primary">{t('appName')}</span>
        </div>
        <Separator />
        <SidebarNav />
      </aside>

      <div className="flex flex-1 flex-col">
        <header className="sticky top-0 z-10 flex h-16 items-center justify-between border-b bg-white px-4 lg:px-6">
          <div className="flex items-center gap-3">
            <Sheet open={mobileOpen} onOpenChange={setMobileOpen}>
              <SheetTrigger asChild>
                <Button variant="ghost" size="icon" className="lg:hidden">
                  <Menu className="h-5 w-5" />
                </Button>
              </SheetTrigger>
              <SheetContent side="left" className="w-64 p-0">
                <div className="flex h-16 items-center justify-between px-4">
                  <span className="text-lg font-bold text-primary">{t('appName')}</span>
                  <Button variant="ghost" size="icon" onClick={() => setMobileOpen(false)}>
                    <X className="h-5 w-5" />
                  </Button>
                </div>
                <Separator />
                <SidebarNav onNavigate={() => setMobileOpen(false)} />
              </SheetContent>
            </Sheet>
            <Breadcrumb />
          </div>

          <div className="flex items-center gap-2">
            <NotificationBell />

            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button variant="ghost" className="flex items-center gap-2 px-2">
                  <Avatar className="h-8 w-8">
                    <AvatarFallback className="bg-primary-lighter text-primary text-xs">
                      {initials}
                    </AvatarFallback>
                  </Avatar>
                  <span className="hidden text-sm font-medium md:inline">
                    {user ? `${user.prenom} ${user.nom}` : t('profile')}
                  </span>
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end">
                <DropdownMenuItem onClick={() => logout()}>
                  {t('logout')}
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          </div>
        </header>

        <main className="flex-1 p-4 md:p-6 lg:p-8">
          <Outlet />
        </main>
      </div>
    </div>
  )
}

export default AppShell

