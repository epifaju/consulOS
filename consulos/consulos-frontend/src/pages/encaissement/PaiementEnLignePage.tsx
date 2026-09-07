import { useSearchParams, Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { CheckCircle2, Loader2 } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { useCompletePaymentSession, usePaymentSession } from '@/hooks/usePayments'
import { formatMontant } from '@/utils/encaissementStatus'

export function PaiementEnLignePage() {
  const { t } = useTranslation('encaissements')
  const [params] = useSearchParams()
  const sessionId = params.get('session')
  const sessionQuery = usePaymentSession(sessionId)
  const completeMutation = useCompletePaymentSession()

  if (!sessionId) {
    return (
      <div className="mx-auto max-w-lg p-8">
        <Card>
          <CardContent className="pt-6 text-center text-sm text-muted-foreground">
            {t('online.missingSession')}
            <div className="mt-4">
              <Button asChild variant="outline">
                <Link to="/caisse">{t('online.back')}</Link>
              </Button>
            </div>
          </CardContent>
        </Card>
      </div>
    )
  }

  const session = sessionQuery.data
  const isCompleted = session?.status === 'COMPLETED'

  return (
    <div className="mx-auto max-w-lg space-y-4 p-4 md:p-8">
      <Card>
        <CardHeader>
          <CardTitle className="text-primary">{t('online.title')}</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          {sessionQuery.isLoading ? (
            <div className="flex items-center gap-2 text-muted-foreground">
              <Loader2 className="h-4 w-4 animate-spin" />
              {t('loading')}
            </div>
          ) : !session ? (
            <p className="text-danger text-sm">{t('online.notFound')}</p>
          ) : (
            <>
              <div className="rounded-md bg-light p-3 text-sm">
                <p>
                  <span className="font-medium">{t('online.amount')} :</span>{' '}
                  {formatMontant(Number(session.amount), session.currency)}
                </p>
                <p>
                  <span className="font-medium">{t('online.provider')} :</span> {session.provider}
                </p>
                <p>
                  <span className="font-medium">{t('online.status')} :</span> {session.status}
                </p>
              </div>

              {isCompleted ? (
                <div className="flex items-center gap-2 text-success">
                  <CheckCircle2 className="h-5 w-5" />
                  {t('online.success')}
                </div>
              ) : (
                <Button
                  className="w-full bg-primary"
                  disabled={completeMutation.isPending}
                  onClick={() => completeMutation.mutate(sessionId)}
                >
                  {completeMutation.isPending ? (
                    <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                  ) : null}
                  {t('online.confirm')}
                </Button>
              )}

              <Button asChild variant="outline" className="w-full">
                <Link to="/caisse">{t('online.back')}</Link>
              </Button>
            </>
          )}
        </CardContent>
      </Card>
    </div>
  )
}

export default PaiementEnLignePage
