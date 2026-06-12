import { Download } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { Button } from '@/components/ui/button'
import { useExportJournalExcel } from '@/hooks/useEncaissementExports'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { useJournalCaisse } from '@/hooks/useEncaissements'
import { formatMontant } from '@/utils/encaissementStatus'

interface JournalCaissePanelProps {
  date: string
}

export function JournalCaissePanel({ date }: JournalCaissePanelProps) {
  const { t } = useTranslation('encaissements')
  const journalQuery = useJournalCaisse(date)
  const exportExcel = useExportJournalExcel()
  const journal = journalQuery.data

  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
        <CardTitle className="text-base">{t('journal.title')}</CardTitle>
        <Button
          variant="outline"
          size="sm"
          disabled={exportExcel.isPending}
          onClick={() => exportExcel.mutate({ dateDebut: date, dateFin: date })}
        >
          <Download className="mr-2 h-4 w-4" />
          {exportExcel.isPending ? t('journal.exportExcelPending') : t('journal.exportExcel')}
        </Button>
      </CardHeader>
      <CardContent>
        {journalQuery.isLoading && (
          <p className="text-muted-foreground text-sm">{t('loading')}</p>
        )}
        {journal && (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>{t('journal.mode')}</TableHead>
                <TableHead className="text-right">{t('journal.count')}</TableHead>
                <TableHead className="text-right">{t('journal.total')}</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {journal.parMode.length === 0 && (
                <TableRow>
                  <TableCell colSpan={3} className="text-muted-foreground text-center text-sm">
                    {t('journal.empty')}
                  </TableCell>
                </TableRow>
              )}
              {journal.parMode.map((row) => (
                <TableRow key={row.modePaiement}>
                  <TableCell>{t(`modes.${row.modePaiement}`)}</TableCell>
                  <TableCell className="text-right">{row.nombreTransactions}</TableCell>
                  <TableCell className="text-right">{formatMontant(row.total)}</TableCell>
                </TableRow>
              ))}
              {journal.parMode.length > 0 && (
                <TableRow className="font-semibold">
                  <TableCell>{t('journal.grandTotal')}</TableCell>
                  <TableCell className="text-right">{journal.nombreTransactions}</TableCell>
                  <TableCell className="text-right">
                    {formatMontant(journal.montantTotalPercu)}
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        )}
      </CardContent>
    </Card>
  )
}

export default JournalCaissePanel
