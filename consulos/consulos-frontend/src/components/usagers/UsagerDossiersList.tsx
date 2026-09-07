import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { Badge } from '@/components/ui/badge'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import type { UsagerLinkedDossier } from '@/types/usager'

interface UsagerDossiersListProps {
  dossiers: UsagerLinkedDossier[]
  isLoading: boolean
}

export function UsagerDossiersList({ dossiers, isLoading }: UsagerDossiersListProps) {
  const { t } = useTranslation('usagers')

  if (isLoading) {
    return <p className="text-muted-foreground text-sm">{t('loading')}</p>
  }

  if (dossiers.length === 0) {
    return <p className="text-muted-foreground text-sm">{t('dossiers.empty')}</p>
  }

  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>{t('dossiers.columns.reference')}</TableHead>
          <TableHead>{t('dossiers.columns.type')}</TableHead>
          <TableHead>{t('dossiers.columns.statut')}</TableHead>
          <TableHead>{t('dossiers.columns.date')}</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {dossiers.map((dossier) => (
          <TableRow key={dossier.id}>
            <TableCell>
              <Link className="font-medium text-primary hover:underline" to={`/dossiers/${dossier.id}`}>
                {dossier.numero}
              </Link>
            </TableCell>
            <TableCell>{dossier.typeService ?? '—'}</TableCell>
            <TableCell>
              {dossier.statut ? <Badge variant="secondary">{dossier.statut}</Badge> : '—'}
            </TableCell>
            <TableCell>
              {dossier.createdAt ? new Date(dossier.createdAt).toLocaleDateString() : '—'}
            </TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  )
}
