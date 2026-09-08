import { useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Building2, Calendar, FolderOpen, Clock, Wallet } from 'lucide-react'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { Button } from '@/components/ui/button'
import { useMyReseaux, useReseauStatus } from '@/hooks/useReseau'
import { useRegionalPosteStats, useRegionalPostes, useRegionalStats } from '@/hooks/useRegional'
import { RegionalReportingPanel } from '@/components/regional/RegionalReportingPanel'
import { RegionalAlertsPanel } from '@/components/regional/RegionalAlertsPanel'
import { RegionalAdminPanel } from '@/components/regional/RegionalAdminPanel'
import { useAuthStore } from '@/stores/authStore'
import { canAccessReseau } from '@/utils/rolePermissions'
import { formatMontant } from '@/utils/encaissementStatus'
import { cn } from '@/lib/utils'

function formatRecette(value: number | string): string {
  const num = typeof value === 'number' ? value : Number(value)
  if (Number.isNaN(num)) return String(value)
  return formatMontant(num, 'EUR')
}

export function RegionalDashboardPage() {
  const { t } = useTranslation('regional')
  const role = useAuthStore((s) => s.user?.role)
  const canView = canAccessReseau(role)

  const statusQuery = useReseauStatus(canView)
  const moduleEnabled = statusQuery.data?.enabled === true

  const meQuery = useMyReseaux(canView && moduleEnabled)
  const reseaux = meQuery.data?.reseaux ?? []
  const [selectedReseauId, setSelectedReseauId] = useState<string | null>(null)
  const [selectedPosteId, setSelectedPosteId] = useState<string | null>(null)

  const activeReseauId = useMemo(() => {
    if (selectedReseauId) return selectedReseauId
    return reseaux[0]?.id ?? null
  }, [selectedReseauId, reseaux])

  const statsQuery = useRegionalStats(activeReseauId, canView && moduleEnabled && Boolean(activeReseauId))
  const postesQuery = useRegionalPostes(activeReseauId, canView && moduleEnabled && Boolean(activeReseauId))
  const posteDetailQuery = useRegionalPosteStats(
    selectedPosteId,
    activeReseauId,
    canView && moduleEnabled && Boolean(selectedPosteId),
  )

  if (!canView) {
    return (
      <div className="rounded-lg border bg-white p-8 text-center text-muted-foreground">
        {t('forbidden')}
      </div>
    )
  }

  if (statusQuery.isLoading) {
    return <p className="text-muted-foreground">{t('loading')}</p>
  }

  if (!moduleEnabled) {
    return (
      <div className="rounded-lg border bg-white p-8 text-center text-muted-foreground">
        {t('disabled')}
      </div>
    )
  }

  if (meQuery.isLoading) {
    return <p className="text-muted-foreground">{t('loading')}</p>
  }

  if (reseaux.length === 0) {
    return (
      <div className="rounded-lg border bg-white p-8 text-center text-muted-foreground">
        {t('emptyMembership')}
      </div>
    )
  }

  const stats = statsQuery.data
  const postes = postesQuery.data ?? []
  const activeReseau = reseaux.find((r) => r.id === activeReseauId)
  const adminPostes = activeReseau?.postes ?? []

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-primary">{t('title')}</h1>
          <p className="text-muted-foreground text-sm">{t('subtitle')}</p>
        </div>
        <div className="flex flex-wrap gap-2">
          {reseaux.map((reseau) => (
            <Button
              key={reseau.id}
              variant={activeReseauId === reseau.id ? 'default' : 'outline'}
              className={cn(activeReseauId === reseau.id && 'bg-primary')}
              onClick={() => {
                setSelectedReseauId(reseau.id)
                setSelectedPosteId(null)
              }}
            >
              {reseau.code}
            </Button>
          ))}
        </div>
      </div>

      <RegionalAlertsPanel
        reseauId={activeReseauId}
        enabled={canView && moduleEnabled && Boolean(activeReseauId)}
      />

      {statsQuery.isLoading ? (
        <p className="text-muted-foreground">{t('loading')}</p>
      ) : stats ? (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-5">
          <KpiMini icon={Building2} label={t('kpis.postes')} value={String(stats.postesCount)} />
          <KpiMini icon={FolderOpen} label={t('kpis.dossiersOuverts')} value={String(stats.dossiersOuverts)} />
          <KpiMini icon={Clock} label={t('kpis.enRevue')} value={String(stats.enRevue)} />
          <KpiMini icon={Calendar} label={t('kpis.rdvJour')} value={String(stats.rdvJour)} />
          <KpiMini
            icon={Wallet}
            label={t('kpis.recettesJour')}
            value={formatRecette(stats.recettesJour)}
          />
        </div>
      ) : null}

      {stats && (
        <p className="text-muted-foreground text-sm">
          {t('kpis.enRetard')}: <span className="font-medium text-danger">{stats.dossiersEnRetard}</span>
          {' · '}
          {t('kpis.recettesPeriode')}:{' '}
          <span className="font-medium text-primary">{formatRecette(stats.recettesPeriode)}</span>
        </p>
      )}

      <Card>
        <CardHeader className="pb-2">
          <CardTitle className="text-lg text-primary">{t('postes.title')}</CardTitle>
        </CardHeader>
        <CardContent>
          {postesQuery.isLoading ? (
            <p className="text-muted-foreground text-sm">{t('loading')}</p>
          ) : postes.length === 0 ? (
            <p className="text-muted-foreground text-sm">{t('postes.empty')}</p>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>{t('postes.code')}</TableHead>
                  <TableHead>{t('postes.nom')}</TableHead>
                  <TableHead className="text-right">{t('postes.dossiers')}</TableHead>
                  <TableHead className="text-right">{t('postes.enRevue')}</TableHead>
                  <TableHead className="text-right">{t('postes.rdv')}</TableHead>
                  <TableHead className="text-right">{t('postes.recettes')}</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {postes.map((poste) => (
                  <TableRow
                    key={poste.organisationId}
                    className={cn(
                      'cursor-pointer',
                      selectedPosteId === poste.organisationId && 'bg-light',
                    )}
                    onClick={() => setSelectedPosteId(poste.organisationId)}
                  >
                    <TableCell className="font-medium">{poste.code}</TableCell>
                    <TableCell>{poste.nom}</TableCell>
                    <TableCell className="text-right">{poste.dossiersOuverts}</TableCell>
                    <TableCell className="text-right">{poste.enRevue}</TableCell>
                    <TableCell className="text-right">{poste.rdvJour}</TableCell>
                    <TableCell className="text-right">{formatRecette(poste.recettesJour)}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>

      {selectedPosteId && (
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-lg text-primary">{t('detail.title')}</CardTitle>
          </CardHeader>
          <CardContent>
            {posteDetailQuery.isLoading ? (
              <p className="text-muted-foreground text-sm">{t('loading')}</p>
            ) : posteDetailQuery.data ? (
              <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4 text-sm">
                <div>
                  <p className="text-muted-foreground">{t('postes.code')}</p>
                  <p className="font-medium">{posteDetailQuery.data.code}</p>
                </div>
                <div>
                  <p className="text-muted-foreground">{t('postes.nom')}</p>
                  <p className="font-medium">{posteDetailQuery.data.nom}</p>
                </div>
                <div>
                  <p className="text-muted-foreground">{t('postes.dossiers')}</p>
                  <p className="font-medium">{posteDetailQuery.data.dossiersOuverts}</p>
                </div>
                <div>
                  <p className="text-muted-foreground">{t('postes.recettes')}</p>
                  <p className="font-medium">{formatRecette(posteDetailQuery.data.recettesJour)}</p>
                </div>
              </div>
            ) : (
              <p className="text-danger text-sm">{t('detail.notFound')}</p>
            )}
          </CardContent>
        </Card>
      )}

      <RegionalReportingPanel
        reseauId={activeReseauId}
        enabled={canView && moduleEnabled && Boolean(activeReseauId)}
      />

      <RegionalAdminPanel
        reseauId={activeReseauId}
        roleReseau={activeReseau?.roleReseau}
        postes={adminPostes}
        enabled={canView && moduleEnabled && Boolean(activeReseauId)}
      />
    </div>
  )
}

function KpiMini({
  icon: Icon,
  label,
  value,
}: {
  icon: typeof FolderOpen
  label: string
  value: string
}) {
  return (
    <Card>
      <CardContent className="flex items-start justify-between p-4">
        <div>
          <p className="text-muted-foreground text-sm">{label}</p>
          <p className="text-2xl font-bold text-primary">{value}</p>
        </div>
        <div className="rounded-lg bg-light p-2">
          <Icon className="h-5 w-5 text-accent" />
        </div>
      </CardContent>
    </Card>
  )
}

export default RegionalDashboardPage
