import { FolderOpen, Calendar, Wallet, Clock } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { KpiCard } from '@/components/dashboard/KpiCard'
import { DossiersBarChart } from '@/components/dashboard/DossiersBarChart'
import { RecettesLineChart } from '@/components/dashboard/RecettesLineChart'
import { AlertesPanel } from '@/components/dashboard/AlertesPanel'
import { AgentsChargeTable } from '@/components/dashboard/AgentsChargeTable'
import {
  useAgentsCharge,
  useDashboardAlerts,
  useDashboardStats,
  useDossiersChart,
  useRecettesChart,
} from '@/hooks/useDashboard'
import { useAuthStore } from '@/stores/authStore'
import {
  canViewDashboardRecettes,
  canViewDashboardSupervision,
} from '@/utils/rolePermissions'
import { formatMontant } from '@/utils/encaissementStatus'

export function DashboardPage() {
  const { t } = useTranslation('dashboard')
  const userRole = useAuthStore((state) => state.user?.role)
  const showSupervision = canViewDashboardSupervision(userRole)
  const showRecettes = canViewDashboardRecettes(userRole)

  const statsQuery = useDashboardStats(undefined, undefined, showSupervision)
  const dossiersChartQuery = useDossiersChart('MOIS', showSupervision)
  const recettesChartQuery = useRecettesChart('MOIS', showRecettes)
  const alertsQuery = useDashboardAlerts(showSupervision)
  const agentsQuery = useAgentsCharge(showSupervision)

  const stats = statsQuery.data

  if (showSupervision && statsQuery.isLoading) {
    return <p className="text-muted-foreground">{t('loading')}</p>
  }

  if (!showSupervision && !showRecettes) {
    return (
      <div className="space-y-4">
        <h1 className="text-2xl font-semibold text-primary">{t('title')}</h1>
        <p className="text-muted-foreground text-sm">{t('welcomeAgent')}</p>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-semibold text-primary">{t('title')}</h1>

      {showSupervision && (
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          {stats && (
            <>
              <KpiCard kpi={stats.dossiersOuverts} icon={FolderOpen} label={t('kpis.dossiersOuverts')} />
              <KpiCard kpi={stats.enAttenteValidation} icon={Clock} label={t('kpis.enAttente')} />
              <KpiCard kpi={stats.rdvJour} icon={Calendar} label={t('kpis.rdvJour')} />
              <KpiCard
                kpi={stats.recettesJour}
                icon={Wallet}
                label={t('kpis.recettesJour')}
                formatValue={(v) => formatMontant(v)}
              />
            </>
          )}
        </div>
      )}

      <div className="grid gap-4 lg:grid-cols-2">
        {showSupervision && <DossiersBarChart data={dossiersChartQuery.data ?? []} />}
        {showRecettes && <RecettesLineChart data={recettesChartQuery.data ?? []} />}
      </div>

      {showSupervision && (
        <div className="grid gap-4 lg:grid-cols-2">
          <AlertesPanel alertes={alertsQuery.data ?? []} />
          <AgentsChargeTable agents={agentsQuery.data ?? []} />
        </div>
      )}
    </div>
  )
}

export default DashboardPage
