import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { useAuthStore } from '@/stores/authStore'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'

interface ProtectedRouteProps {
  roles?: string[]
}

export function ProtectedRoute({ roles }: ProtectedRouteProps) {
  const { t } = useTranslation('auth')
  const location = useLocation()
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated)
  const user = useAuthStore((state) => state.user)

  if (!isAuthenticated) {
    const returnUrl = encodeURIComponent(location.pathname + location.search)
    return <Navigate to={`/login?returnUrl=${returnUrl}`} replace />
  }

  if (roles && roles.length > 0 && user && !roles.includes(user.role)) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-surface p-4">
        <Card className="w-full max-w-md">
          <CardHeader>
            <CardTitle className="text-danger">{t('forbiddenTitle')}</CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-muted-foreground text-sm">{t('forbiddenMessage')}</p>
          </CardContent>
        </Card>
      </div>
    )
  }

  return <Outlet />
}

export default ProtectedRoute
