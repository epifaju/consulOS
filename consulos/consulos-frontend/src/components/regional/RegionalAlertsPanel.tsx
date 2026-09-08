import { AlertTriangle, XCircle } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { useRegionalAlerts } from '@/hooks/useRegional'

interface RegionalAlertsPanelProps {
  reseauId: string | null
  enabled: boolean
}

export function RegionalAlertsPanel({ reseauId, enabled }: RegionalAlertsPanelProps) {
  const { t } = useTranslation('regional')
  const alertsQuery = useRegionalAlerts(reseauId, enabled && Boolean(reseauId))
  const alertes = alertsQuery.data ?? []

  return (
    <Card>
      <CardHeader className="pb-2">
        <CardTitle className="text-lg text-primary">{t('alerts.title')}</CardTitle>
      </CardHeader>
      <CardContent className="space-y-3">
        {alertsQuery.isLoading ? (
          <p className="text-muted-foreground text-sm">{t('loading')}</p>
        ) : alertes.length === 0 ? (
          <p className="text-muted-foreground text-sm">{t('alerts.empty')}</p>
        ) : (
          alertes.map((alerte) => {
            const isCritical = alerte.severite === 'critical'
            const Icon = isCritical ? XCircle : AlertTriangle
            return (
              <div
                key={alerte.type}
                className={`rounded-md border p-3 ${isCritical ? 'border-red-200 bg-red-50' : 'border-amber-200 bg-amber-50'}`}
              >
                <div className="flex items-start gap-2">
                  <Icon className={`mt-0.5 h-4 w-4 ${isCritical ? 'text-danger' : 'text-warning'}`} />
                  <div className="flex-1">
                    <p className="text-sm font-medium">
                      {t(`alerts.types.${alerte.type}`, { defaultValue: alerte.type })}
                    </p>
                    <p className="text-muted-foreground text-xs">{alerte.message}</p>
                    <p className="mt-1 text-xs font-semibold">
                      {t('alerts.count', { count: alerte.count })}
                    </p>
                    {alerte.posteCodes.length > 0 && (
                      <p className="mt-1 text-xs">
                        {t('alerts.postes')}: {alerte.posteCodes.join(', ')}
                      </p>
                    )}
                  </div>
                </div>
              </div>
            )
          })
        )}
      </CardContent>
    </Card>
  )
}

export default RegionalAlertsPanel
