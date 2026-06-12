import { useEffect, useState, type ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { bootstrapAuthSession } from '@/lib/authBootstrap'
import { useAuthStore } from '@/stores/authStore'

interface AuthBootstrapProps {
  children: ReactNode
}

export function AuthBootstrap({ children }: AuthBootstrapProps) {
  const { t } = useTranslation('common')
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)
  const [ready, setReady] = useState(isAuthenticated)

  useEffect(() => {
    if (isAuthenticated) {
      setReady(true)
      return
    }

    let cancelled = false
    bootstrapAuthSession().finally(() => {
      if (!cancelled) {
        setReady(true)
      }
    })

    return () => {
      cancelled = true
    }
  }, [isAuthenticated])

  if (!ready) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-surface text-muted-foreground">
        {t('loading')}
      </div>
    )
  }

  return <>{children}</>
}

export default AuthBootstrap
