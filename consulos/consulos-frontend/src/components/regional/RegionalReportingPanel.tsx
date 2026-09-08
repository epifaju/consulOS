import { useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { FileSpreadsheet, Loader2, RefreshCw } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import {
  useExportRegionalReportingExcel,
  useRegionalReportingMensuel,
} from '@/hooks/useRegionalReporting'
import { formatMontant } from '@/utils/encaissementStatus'

function formatAmount(value: number | string): string {
  const num = typeof value === 'number' ? value : Number(value)
  if (Number.isNaN(num)) return String(value)
  return formatMontant(num, 'EUR')
}

interface RegionalReportingPanelProps {
  reseauId: string | null
  enabled: boolean
}

export function RegionalReportingPanel({ reseauId, enabled }: RegionalReportingPanelProps) {
  const { t } = useTranslation('regional')
  const now = useMemo(() => new Date(), [])
  const [annee, setAnnee] = useState(now.getFullYear())
  const [mois, setMois] = useState(now.getMonth() + 1)

  const query = useRegionalReportingMensuel(annee, mois, reseauId, enabled && Boolean(reseauId))
  const exportExcel = useExportRegionalReportingExcel()
  const report = query.data

  return (
    <Card>
      <CardHeader className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between pb-2">
        <div>
          <CardTitle className="text-lg text-primary">{t('reporting.title')}</CardTitle>
          <p className="text-muted-foreground text-sm">{t('reporting.subtitle')}</p>
        </div>
        <div className="flex flex-wrap items-end gap-2">
          <div>
            <label className="mb-1 block text-xs font-medium">{t('reporting.year')}</label>
            <Input
              type="number"
              className="w-24"
              value={annee}
              min={2000}
              max={2100}
              onChange={(e) => setAnnee(Number(e.target.value))}
            />
          </div>
          <div>
            <label className="mb-1 block text-xs font-medium">{t('reporting.month')}</label>
            <Input
              type="number"
              className="w-16"
              value={mois}
              min={1}
              max={12}
              onChange={(e) => setMois(Number(e.target.value))}
            />
          </div>
          <Button variant="outline" onClick={() => void query.refetch()} disabled={query.isFetching}>
            <RefreshCw className={`mr-2 h-4 w-4 ${query.isFetching ? 'animate-spin' : ''}`} />
            {t('reporting.refresh')}
          </Button>
          <Button
            className="bg-primary"
            disabled={exportExcel.isPending || !reseauId}
            onClick={() => exportExcel.mutate({ annee, mois, reseauId })}
          >
            {exportExcel.isPending ? (
              <Loader2 className="mr-2 h-4 w-4 animate-spin" />
            ) : (
              <FileSpreadsheet className="mr-2 h-4 w-4" />
            )}
            {t('reporting.excel')}
          </Button>
        </div>
      </CardHeader>
      <CardContent className="space-y-4">
        {query.isLoading ? (
          <p className="text-muted-foreground text-sm">{t('loading')}</p>
        ) : !report ? (
          <p className="text-muted-foreground text-sm">{t('reporting.empty')}</p>
        ) : (
          <>
            <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4 text-sm">
              <div>
                <p className="text-muted-foreground">{t('reporting.dossiers')}</p>
                <p className="text-lg font-semibold text-primary">{report.dossiersCrees}</p>
              </div>
              <div>
                <p className="text-muted-foreground">{t('reporting.documents')}</p>
                <p className="text-lg font-semibold text-primary">{report.documentsDeposes}</p>
              </div>
              <div>
                <p className="text-muted-foreground">{t('reporting.rdv')}</p>
                <p className="text-lg font-semibold text-primary">{report.rendezVous.total}</p>
              </div>
              <div>
                <p className="text-muted-foreground">{t('reporting.montant')}</p>
                <p className="text-lg font-semibold text-primary">
                  {formatAmount(report.encaissements.montantPercuTotal)}
                </p>
              </div>
            </div>

            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>{t('postes.code')}</TableHead>
                  <TableHead>{t('postes.nom')}</TableHead>
                  <TableHead className="text-right">{t('reporting.dossiers')}</TableHead>
                  <TableHead className="text-right">{t('reporting.rdv')}</TableHead>
                  <TableHead className="text-right">{t('reporting.montant')}</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {report.parPoste.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={5} className="text-muted-foreground text-center text-sm">
                      {t('reporting.empty')}
                    </TableCell>
                  </TableRow>
                ) : (
                  report.parPoste.map((poste) => (
                    <TableRow key={poste.organisationId}>
                      <TableCell className="font-medium">{poste.code}</TableCell>
                      <TableCell>{poste.nom}</TableCell>
                      <TableCell className="text-right">{poste.dossiersCrees}</TableCell>
                      <TableCell className="text-right">{poste.rdvTotal}</TableCell>
                      <TableCell className="text-right">{formatAmount(poste.montantPercu)}</TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </>
        )}
      </CardContent>
    </Card>
  )
}

export default RegionalReportingPanel
