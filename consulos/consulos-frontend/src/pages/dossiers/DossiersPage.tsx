import { useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { Eye, Plus } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { useDossiers, useTypesService } from '@/hooks/useDossiers'
import { DOSSIER_STATUTS, getStatutBadgeClass } from '@/utils/dossierStatus'
import type { DossierPriorite, DossierStatut } from '@/types/dossier'

type SortField = 'reference' | 'dateEcheance' | 'createdAt'
type SortDir = 'asc' | 'desc'

export function DossiersPage() {
  const { t } = useTranslation('dossiers')
  const navigate = useNavigate()
  const [page, setPage] = useState(0)
  const [selectedStatuts, setSelectedStatuts] = useState<DossierStatut[]>([])
  const [typeService, setTypeService] = useState<string>('')
  const [priorite, setPriorite] = useState<DossierPriorite | 'ALL'>('ALL')
  const [dateDebut, setDateDebut] = useState('')
  const [dateFin, setDateFin] = useState('')
  const [sortField, setSortField] = useState<SortField>('createdAt')
  const [sortDir, setSortDir] = useState<SortDir>('desc')

  const typesQuery = useTypesService()
  const listQuery = useDossiers({
    page,
    size: 20,
    sort: `${sortField},${sortDir}`,
    statut: selectedStatuts.length > 0 ? selectedStatuts : undefined,
    typeService: typeService || undefined,
    priorite: priorite !== 'ALL' ? priorite : undefined,
    dateDebut: dateDebut ? `${dateDebut}T00:00:00Z` : undefined,
    dateFin: dateFin ? `${dateFin}T23:59:59Z` : undefined,
  })

  const dossiers = listQuery.data?.content ?? []
  const totalPages = listQuery.data?.totalPages ?? 1

  const toggleStatut = (statut: DossierStatut) => {
    setSelectedStatuts((prev) =>
      prev.includes(statut) ? prev.filter((s) => s !== statut) : [...prev, statut],
    )
    setPage(0)
  }

  const toggleSort = (field: SortField) => {
    if (sortField === field) {
      setSortDir((d) => (d === 'asc' ? 'desc' : 'asc'))
    } else {
      setSortField(field)
      setSortDir('asc')
    }
  }

  const sortIndicator = useMemo(
    () => (field: SortField) => (sortField === field ? (sortDir === 'asc' ? ' ↑' : ' ↓') : ''),
    [sortField, sortDir],
  )

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <h1 className="text-2xl font-semibold text-primary">{t('title')}</h1>
        <Button onClick={() => navigate('/dossiers/new')} className="bg-primary">
          <Plus className="mr-2 h-4 w-4" />
          {t('actions.new')}
        </Button>
      </div>

      <div className="grid gap-3 md:grid-cols-2 lg:grid-cols-4">
        <Select
          value={typeService || 'ALL'}
          onValueChange={(v) => {
            setTypeService(v === 'ALL' ? '' : v)
            setPage(0)
          }}
        >
          <SelectTrigger>
            <SelectValue placeholder={t('filters.typeService')} />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="ALL">{t('filters.allTypes')}</SelectItem>
            {(typesQuery.data ?? []).map((type) => (
              <SelectItem key={type.id} value={type.id}>
                {type.libelle}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>

        <Select
          value={priorite}
          onValueChange={(v) => {
            setPriorite(v as DossierPriorite | 'ALL')
            setPage(0)
          }}
        >
          <SelectTrigger>
            <SelectValue placeholder={t('filters.priorite')} />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="ALL">{t('filters.allPriorites')}</SelectItem>
            <SelectItem value="URGENTE">{t('priorites.URGENTE')}</SelectItem>
            <SelectItem value="NORMALE">{t('priorites.NORMALE')}</SelectItem>
            <SelectItem value="FAIBLE">{t('priorites.FAIBLE')}</SelectItem>
          </SelectContent>
        </Select>

        <Input
          type="date"
          value={dateDebut}
          onChange={(e) => {
            setDateDebut(e.target.value)
            setPage(0)
          }}
          aria-label={t('filters.dateDebut')}
        />
        <Input
          type="date"
          value={dateFin}
          onChange={(e) => {
            setDateFin(e.target.value)
            setPage(0)
          }}
          aria-label={t('filters.dateFin')}
        />
      </div>

      <div className="flex flex-wrap gap-2">
        {DOSSIER_STATUTS.map((statut) => (
          <button
            key={statut}
            type="button"
            onClick={() => toggleStatut(statut)}
            className={`rounded-full px-3 py-1 text-xs font-medium transition ${getStatutBadgeClass(statut)} ${
              selectedStatuts.includes(statut) ? 'ring-2 ring-accent' : 'opacity-70'
            }`}
          >
            {t(`statuts.${statut}`)}
          </button>
        ))}
      </div>

      <div className="rounded-lg border bg-white">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead
                className="cursor-pointer"
                onClick={() => toggleSort('reference')}
              >
                {t('columns.reference')}
                {sortIndicator('reference')}
              </TableHead>
              <TableHead>{t('columns.usager')}</TableHead>
              <TableHead>{t('columns.typeService')}</TableHead>
              <TableHead>{t('columns.statut')}</TableHead>
              <TableHead>{t('columns.agent')}</TableHead>
              <TableHead>{t('columns.priorite')}</TableHead>
              <TableHead
                className="cursor-pointer"
                onClick={() => toggleSort('dateEcheance')}
              >
                {t('columns.echeance')}
                {sortIndicator('dateEcheance')}
              </TableHead>
              <TableHead className="text-right">{t('columns.actions')}</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {listQuery.isLoading ? (
              <TableRow>
                <TableCell colSpan={8} className="text-center text-muted-foreground">
                  {t('loading')}
                </TableCell>
              </TableRow>
            ) : dossiers.length === 0 ? (
              <TableRow>
                <TableCell colSpan={8} className="text-center text-muted-foreground">
                  {t('empty')}
                </TableCell>
              </TableRow>
            ) : (
              dossiers.map((dossier) => (
                <TableRow key={dossier.id}>
                  <TableCell className="font-medium">{dossier.reference}</TableCell>
                  <TableCell>
                    {dossier.usagerPrenom} {dossier.usagerNom}
                  </TableCell>
                  <TableCell>{dossier.typeServiceLibelle}</TableCell>
                  <TableCell>
                    <Badge className={getStatutBadgeClass(dossier.statut)}>
                      {t(`statuts.${dossier.statut}`)}
                    </Badge>
                  </TableCell>
                  <TableCell>{dossier.agentNom ?? '—'}</TableCell>
                  <TableCell>
                    <Badge variant="outline">{t(`priorites.${dossier.priorite}`)}</Badge>
                  </TableCell>
                  <TableCell>
                    {dossier.dateEcheance
                      ? new Date(dossier.dateEcheance).toLocaleDateString()
                      : '—'}
                  </TableCell>
                  <TableCell className="text-right">
                    <Button variant="ghost" size="sm" asChild>
                      <Link to={`/dossiers/${dossier.id}`}>
                        <Eye className="h-4 w-4" />
                      </Link>
                    </Button>
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </div>

      {totalPages > 1 && (
        <div className="flex items-center justify-center gap-2">
          <Button
            variant="outline"
            size="sm"
            disabled={page === 0}
            onClick={() => setPage((p) => p - 1)}
          >
            {t('pagination.prev')}
          </Button>
          <span className="text-sm text-muted-foreground">
            {t('pagination.page', { current: page + 1, total: totalPages })}
          </span>
          <Button
            variant="outline"
            size="sm"
            disabled={page >= totalPages - 1}
            onClick={() => setPage((p) => p + 1)}
          >
            {t('pagination.next')}
          </Button>
        </div>
      )}
    </div>
  )
}

export default DossiersPage
