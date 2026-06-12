import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { AlertTriangle, XCircle } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import type { Alerte } from '@/types/dashboard'

const IGNORE_KEY = 'consulos-alertes-ignore'

interface AlertesPanelProps {
  alertes: Alerte[]
}

function getFilterForAlerte(type: string): string {
  switch (type) {
    case 'ECHEANCES_DEPASSEES':
      return 'EN_REVUE,VALIDE,SOUMIS'
    case 'DOSSIERS_BLOQUES':
      return 'EN_REVUE'
    default:
      return ''
  }
}

export function AlertesPanel({ alertes }: AlertesPanelProps) {
  const { t } = useTranslation('dashboard')
  const [ignoreVersion, setIgnoreVersion] = useState(0)

  const visibleAlertes = useMemo(() => {
    const ignored = JSON.parse(localStorage.getItem(IGNORE_KEY) ?? '{}') as Record<string, number>
    const now = Date.now()
    return alertes.filter((a) => {
      const until = ignored[a.type]
      return !until || until < now
    })
  }, [alertes, ignoreVersion])

  const ignoreAlert = (type: string) => {
    const ignored = JSON.parse(localStorage.getItem(IGNORE_KEY) ?? '{}') as Record<string, number>
    ignored[type] = Date.now() + 24 * 60 * 60 * 1000
    localStorage.setItem(IGNORE_KEY, JSON.stringify(ignored))
    setIgnoreVersion((v) => v + 1)
  }

  return (
    <Card className="h-full">
      <CardHeader>
        <CardTitle className="text-base">{t('alerts.title')}</CardTitle>
      </CardHeader>
      <CardContent className="space-y-3">
        {visibleAlertes.length === 0 && (
          <p className="text-muted-foreground text-sm">{t('alerts.empty')}</p>
        )}
        {visibleAlertes.map((alerte) => {
          const isCritical = alerte.severite === 'critical'
          const Icon = isCritical ? XCircle : AlertTriangle
          const filter = getFilterForAlerte(alerte.type)
          return (
            <div
              key={alerte.type}
              className={`rounded-md border p-3 ${isCritical ? 'border-red-200 bg-red-50' : 'border-amber-200 bg-amber-50'}`}
            >
              <div className="flex items-start gap-2">
                <Icon className={`mt-0.5 h-4 w-4 ${isCritical ? 'text-danger' : 'text-warning'}`} />
                <div className="flex-1">
                  <p className="text-sm font-medium">{t(`alerts.types.${alerte.type}`, alerte.message)}</p>
                  <p className="text-muted-foreground text-xs">{alerte.message}</p>
                  <p className="mt-1 text-xs font-semibold">{alerte.count} élément(s)</p>
                  {filter && (
                    <Link
                      to={`/dossiers?statut=${filter}`}
                      className="text-accent mt-1 inline-block text-xs underline"
                    >
                      {t('alerts.viewDossiers', { count: alerte.count })}
                    </Link>
                  )}
                </div>
              </div>
              <Button
                variant="ghost"
                size="sm"
                className="mt-2 h-7 text-xs"
                onClick={() => ignoreAlert(alerte.type)}
              >
                {t('alerts.ignore')}
              </Button>
            </div>
          )
        })}
      </CardContent>
    </Card>
  )
}

export default AlertesPanel
