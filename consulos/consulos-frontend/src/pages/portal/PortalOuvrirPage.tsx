import { useEffect, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { Loader2 } from 'lucide-react'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { useOpenPortalSession } from '@/hooks/usePortal'

export function PortalOuvrirPage() {
  const { t } = useTranslation('portal')
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const openSession = useOpenPortalSession()
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const token = searchParams.get('token')
    if (!token) {
      setError(t('ouvrir.error'))
      return
    }
    openSession.mutate(token, {
      onSuccess: () => navigate('/portal/dossier', { replace: true }),
      onError: () => setError(t('ouvrir.error')),
    })
    // eslint-disable-next-line react-hooks/exhaustive-deps -- exchange token once on mount
  }, [])

  return (
    <div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-primary to-accent p-4">
      <Card className="w-full max-w-md shadow-xl">
        <CardHeader className="text-center">
          <CardTitle className="text-primary">{t('access.title')}</CardTitle>
        </CardHeader>
        <CardContent className="flex flex-col items-center gap-4 py-6">
          {error ? (
            <>
              <p className="text-danger text-center text-sm">{error}</p>
              <Link to="/portal" className="text-accent text-sm hover:underline">
                {t('access.submit')}
              </Link>
            </>
          ) : (
            <>
              <Loader2 className="h-8 w-8 animate-spin text-primary" />
              <p className="text-muted-foreground text-sm">{t('ouvrir.loading')}</p>
            </>
          )}
        </CardContent>
      </Card>
    </div>
  )
}

export default PortalOuvrirPage
