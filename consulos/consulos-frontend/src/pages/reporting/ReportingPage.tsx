import { useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Download, FileSpreadsheet, Loader2, RefreshCw } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import {
  useExportReportingDossiersCsv,
  useExportReportingExcel,
  useExportReportingRdvCsv,
  useReportingMensuel,
} from '@/hooks/useReporting'
import { useAuthStore } from '@/stores/authStore'
import { canViewDashboardSupervision } from '@/utils/rolePermissions'

function formatAmount(value: number | string): string {
  const num = typeof value === 'number' ? value : Number(value)
  if (Number.isNaN(num)) return String(value)
  return new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'EUR' }).format(num)
}

export function ReportingPage() {
  const { t } = useTranslation('reporting')
  const role = useAuthStore((s) => s.user?.role)
  const now = useMemo(() => new Date(), [])
  const [annee, setAnnee] = useState(now.getFullYear())
  const [mois, setMois] = useState(now.getMonth() + 1)

  const canView = canViewDashboardSupervision(role)
  const query = useReportingMensuel(annee, mois, canView)
  const exportExcel = useExportReportingExcel()
  const exportDossiers = useExportReportingDossiersCsv()
  const exportRdv = useExportReportingRdvCsv()

  if (!canView) {
    return (
      <div className="rounded-lg border bg-white p-8 text-center text-muted-foreground">
        {t('forbidden')}
      </div>
    )
  }

  const report = query.data
  const period = { annee, mois }

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-primary">{t('title')}</h1>
          <p className="text-muted-foreground text-sm">{t('subtitle')}</p>
        </div>
        <div className="flex flex-wrap items-end gap-3">
          <div>
            <label className="mb-1 block text-xs font-medium">{t('year')}</label>
            <Input
              type="number"
              className="w-28"
              value={annee}
              min={2000}
              max={2100}
              onChange={(e) => setAnnee(Number(e.target.value))}
            />
          </div>
          <div>
            <label className="mb-1 block text-xs font-medium">{t('month')}</label>
            <Input
              type="number"
              className="w-20"
              value={mois}
              min={1}
              max={12}
              onChange={(e) => setMois(Number(e.target.value))}
            />
          </div>
          <Button variant="outline" onClick={() => void query.refetch()} disabled={query.isFetching}>
            <RefreshCw className={`mr-2 h-4 w-4 ${query.isFetching ? 'animate-spin' : ''}`} />
            {t('refresh')}
          </Button>
        </div>
      </div>

      <div className="flex flex-wrap gap-2">
        <Button
          className="bg-primary"
          disabled={exportExcel.isPending}
          onClick={() => exportExcel.mutate(period)}
        >
          {exportExcel.isPending ? (
            <Loader2 className="mr-2 h-4 w-4 animate-spin" />
          ) : (
            <FileSpreadsheet className="mr-2 h-4 w-4" />
          )}
          {t('exports.excel')}
        </Button>
        <Button
          variant="outline"
          disabled={exportDossiers.isPending}
          onClick={() => exportDossiers.mutate(period)}
        >
          <Download className="mr-2 h-4 w-4" />
          {t('exports.dossiersCsv')}
        </Button>
        <Button variant="outline" disabled={exportRdv.isPending} onClick={() => exportRdv.mutate(period)}>
          <Download className="mr-2 h-4 w-4" />
          {t('exports.rdvCsv')}
        </Button>
      </div>

      {query.isLoading ? (
        <div className="flex items-center gap-2 text-muted-foreground">
          <Loader2 className="h-5 w-5 animate-spin" />
          {t('loading')}
        </div>
      ) : !report ? (
        <p className="text-muted-foreground">{t('empty')}</p>
      ) : (
        <>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            <KpiCard label={t('kpis.dossiers')} value={String(report.dossiersCrees)} />
            <KpiCard label={t('kpis.documents')} value={String(report.documentsDeposes)} />
            <KpiCard label={t('kpis.rdv')} value={String(report.rendezVous.total)} />
            <KpiCard label={t('kpis.encaissements')} value={String(report.encaissements.nbTransactions)} />
            <KpiCard
              label={t('kpis.montant')}
              value={formatAmount(report.encaissements.montantPercuTotal)}
            />
            <KpiCard label={t('kpis.delai')} value={String(report.delaiMoyenHeures)} />
          </div>

          <div className="grid gap-4 lg:grid-cols-2">
            <CountTable
              title={t('sections.dossiersByStatus')}
              rows={report.dossiersParStatut.map((r) => [r.statut, String(r.count)])}
              empty={t('empty')}
            />
            <CountTable
              title={t('sections.rdvByStatus')}
              rows={report.rendezVous.parStatut.map((r) => [r.statut, String(r.count)])}
              empty={t('empty')}
            />
            <CountTable
              title={t('sections.encaissementsByMode')}
              rows={report.encaissements.parMode.map((r) => [
                r.mode,
                `${r.count} · ${formatAmount(r.montant)}`,
              ])}
              empty={t('empty')}
            />
            <CountTable
              title={t('sections.encaissementsByService')}
              rows={report.encaissements.parService.map((r) => [
                r.typeService,
                `${r.nbTransactions} · ${formatAmount(r.montantTotal)}`,
              ])}
              empty={t('empty')}
            />
          </div>
        </>
      )}
    </div>
  )
}

function KpiCard({ label, value }: { label: string; value: string }) {
  return (
    <Card>
      <CardHeader className="pb-2">
        <CardTitle className="text-muted-foreground text-sm font-medium">{label}</CardTitle>
      </CardHeader>
      <CardContent>
        <p className="text-2xl font-semibold text-primary">{value}</p>
      </CardContent>
    </Card>
  )
}

function CountTable({
  title,
  rows,
  empty,
}: {
  title: string
  rows: string[][]
  empty: string
}) {
  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-base text-primary">{title}</CardTitle>
      </CardHeader>
      <CardContent>
        {rows.length === 0 ? (
          <p className="text-muted-foreground text-sm">{empty}</p>
        ) : (
          <ul className="divide-y">
            {rows.map(([key, value]) => (
              <li key={key} className="flex items-center justify-between py-2 text-sm">
                <span className="font-medium">{key}</span>
                <span className="text-muted-foreground">{value}</span>
              </li>
            ))}
          </ul>
        )}
      </CardContent>
    </Card>
  )
}

export default ReportingPage
