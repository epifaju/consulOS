import { Fragment, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Download, ChevronDown, ChevronRight } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { useAuditLogs } from '@/hooks/useAudit'
import { auditService } from '@/services/auditService'

export function AuditLogsPage() {
  const { t } = useTranslation('admin')
  const [page, setPage] = useState(0)
  const [action, setAction] = useState('')
  const [dateDebut, setDateDebut] = useState('')
  const [dateFin, setDateFin] = useState('')
  const [expandedId, setExpandedId] = useState<string | null>(null)

  const logsQuery = useAuditLogs({
    page,
    size: 20,
    action: action || undefined,
    dateDebut: dateDebut ? `${dateDebut}T00:00:00Z` : undefined,
    dateFin: dateFin ? `${dateFin}T23:59:59Z` : undefined,
  })

  const logs = logsQuery.data?.content ?? []
  const totalPages = logsQuery.data?.totalPages ?? 1

  const handleExport = async () => {
    const response = await auditService.exportCsv({
      action: action || undefined,
      dateDebut: dateDebut ? `${dateDebut}T00:00:00Z` : undefined,
      dateFin: dateFin ? `${dateFin}T23:59:59Z` : undefined,
    })
    const url = URL.createObjectURL(response.data)
    const a = document.createElement('a')
    a.href = url
    a.download = 'audit-logs.csv'
    a.click()
    URL.revokeObjectURL(url)
  }

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h2 className="text-lg font-semibold">{t('audit.title')}</h2>
        <Button variant="outline" onClick={() => void handleExport()}>
          <Download className="mr-2 h-4 w-4" />
          {t('audit.export')}
        </Button>
      </div>

      <div className="flex flex-wrap gap-2">
        <Input
          placeholder={t('audit.filters.action')}
          value={action}
          onChange={(e) => { setAction(e.target.value); setPage(0) }}
          className="w-48"
        />
        <Input type="date" value={dateDebut} onChange={(e) => { setDateDebut(e.target.value); setPage(0) }} />
        <Input type="date" value={dateFin} onChange={(e) => { setDateFin(e.target.value); setPage(0) }} />
      </div>

      <Table>
        <TableHeader>
          <TableRow>
            <TableHead className="w-8" />
            <TableHead>{t('audit.columns.date')}</TableHead>
            <TableHead>{t('audit.columns.user')}</TableHead>
            <TableHead>{t('audit.columns.action')}</TableHead>
            <TableHead>{t('audit.columns.entite')}</TableHead>
            <TableHead>{t('audit.columns.ip')}</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {logs.map((log) => (
            <Fragment key={log.id}>
              <TableRow>
                <TableCell>
                  <button type="button" onClick={() => setExpandedId(expandedId === log.id ? null : log.id)}>
                    {expandedId === log.id ? <ChevronDown className="h-4 w-4" /> : <ChevronRight className="h-4 w-4" />}
                  </button>
                </TableCell>
                <TableCell className="text-xs">{new Date(log.createdAt).toLocaleString()}</TableCell>
                <TableCell>{log.userPrenom} {log.userNom}</TableCell>
                <TableCell className="font-mono text-xs">{log.action}</TableCell>
                <TableCell>{log.entiteType}</TableCell>
                <TableCell className="text-xs">{log.ipAddress}</TableCell>
              </TableRow>
              {expandedId === log.id && (
                <TableRow>
                  <TableCell colSpan={6}>
                    <pre className="max-h-40 overflow-auto rounded bg-gray-50 p-2 text-xs">
                      {JSON.stringify(
                        { ancien: log.ancienEtat, nouveau: log.nouvelEtat },
                        null,
                        2,
                      )}
                    </pre>
                  </TableCell>
                </TableRow>
              )}
            </Fragment>
          ))}
        </TableBody>
      </Table>

      {totalPages > 1 && (
        <div className="flex justify-center gap-2">
          <Button variant="outline" size="sm" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
            {t('pagination.prev')}
          </Button>
          <span className="text-sm">{page + 1} / {totalPages}</span>
          <Button variant="outline" size="sm" disabled={page >= totalPages - 1} onClick={() => setPage((p) => p + 1)}>
            {t('pagination.next')}
          </Button>
        </div>
      )}
    </div>
  )
}

export default AuditLogsPage
