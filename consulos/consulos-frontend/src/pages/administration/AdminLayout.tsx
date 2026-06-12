import { NavLink, Outlet } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { cn } from '@/lib/utils'

const tabs = [
  { to: '/administration/users', labelKey: 'nav.users' },
  { to: '/administration/roles', labelKey: 'nav.roles' },
  { to: '/administration/parametres', labelKey: 'nav.parametres' },
  { to: '/administration/audit', labelKey: 'nav.audit' },
] as const

export function AdminLayout() {
  const { t } = useTranslation('admin')

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-semibold text-primary">{t('title')}</h1>
      <nav className="flex flex-wrap gap-2 border-b pb-2">
        {tabs.map(({ to, labelKey }) => (
          <NavLink
            key={to}
            to={to}
            className={({ isActive }) =>
              cn(
                'rounded-md px-3 py-2 text-sm font-medium',
                isActive ? 'bg-primary text-white' : 'text-primary hover:bg-light',
              )
            }
          >
            {t(labelKey)}
          </NavLink>
        ))}
      </nav>
      <Outlet />
    </div>
  )
}

export default AdminLayout
