import { useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { Eye, Pencil, Plus, Trash2 } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { Badge } from '@/components/ui/badge'
import { ConfirmDialog } from '@/components/usagers/ConfirmDialog'
import { UsagersTableSkeleton } from '@/components/usagers/UsagersTableSkeleton'
import { useDeleteUsager, useSearchUsagers, useUsagers } from '@/hooks/useUsagers'
import { useAuthStore } from '@/stores/authStore'
import type { TypeUsager, UsagerSummary } from '@/types/usager'
import { canDeleteUsagers, canWriteUsagers } from '@/utils/rolePermissions'

export function UsagersPage() {
  const { t } = useTranslation('usagers')
  const navigate = useNavigate()
  const [search, setSearch] = useState('')
  const [typeFilter, setTypeFilter] = useState<TypeUsager | 'ALL'>('ALL')
  const [nationaliteFilter, setNationaliteFilter] = useState('')
  const [page, setPage] = useState(0)
  const [deleteTarget, setDeleteTarget] = useState<UsagerSummary | null>(null)

  const isSearching = search.trim().length >= 2
  const listQuery = useUsagers({ page, size: 20, q: isSearching ? search : undefined })
  const searchQuery = useSearchUsagers(search)
  const deleteMutation = useDeleteUsager()
  const userRole = useAuthStore((state) => state.user?.role)
  const canWrite = canWriteUsagers(userRole)
  const canDelete = canDeleteUsagers(userRole)

  const rawUsagers = isSearching ? searchQuery.data ?? [] : listQuery.data?.content ?? []
  const isLoading = isSearching ? searchQuery.isLoading : listQuery.isLoading

  const usagers = useMemo(() => {
    return rawUsagers.filter((usager) => {
      const typeMatch = typeFilter === 'ALL' || usager.typeUsager === typeFilter
      const natMatch =
        !nationaliteFilter ||
        (usager.nationalite ?? '').toUpperCase().includes(nationaliteFilter.toUpperCase())
      return typeMatch && natMatch
    })
  }, [rawUsagers, typeFilter, nationaliteFilter])

  const totalPages = listQuery.data?.totalPages ?? 1

  const handleDelete = () => {
    if (!deleteTarget) return
    deleteMutation.mutate(deleteTarget.id, {
      onSuccess: () => setDeleteTarget(null),
    })
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <h1 className="text-2xl font-semibold text-primary">{t('title')}</h1>
        {canWrite && (
          <Button onClick={() => navigate('/usagers/new')} className="bg-primary">
            <Plus className="mr-2 h-4 w-4" />
            {t('actions.new')}
          </Button>
        )}
      </div>

      <div className="grid gap-3 md:grid-cols-4">
        <Input
          placeholder={t('searchPlaceholder')}
          value={search}
          onChange={(e) => {
            setSearch(e.target.value)
            setPage(0)
          }}
          className="md:col-span-2"
        />
        <Select
          value={typeFilter}
          onValueChange={(value) => setTypeFilter(value as TypeUsager | 'ALL')}
        >
          <SelectTrigger>
            <SelectValue placeholder={t('filters.type')} />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="ALL">{t('filters.allTypes')}</SelectItem>
            <SelectItem value="DIASPORA">DIASPORA</SelectItem>
            <SelectItem value="MANDATAIRE">MANDATAIRE</SelectItem>
            <SelectItem value="AUTRE">AUTRE</SelectItem>
          </SelectContent>
        </Select>
        <Input
          placeholder={t('filters.nationality')}
          value={nationaliteFilter}
          onChange={(e) => setNationaliteFilter(e.target.value)}
        />
      </div>

      {isLoading ? (
        <UsagersTableSkeleton />
      ) : (
        <div className="rounded-md border bg-white">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>{t('columns.numero')}</TableHead>
                <TableHead>{t('columns.fullName')}</TableHead>
                <TableHead>{t('columns.nationality')}</TableHead>
                <TableHead>{t('columns.type')}</TableHead>
                <TableHead>{t('columns.phone')}</TableHead>
                <TableHead>{t('columns.email')}</TableHead>
                <TableHead className="text-right">{t('columns.actions')}</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {usagers.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={7} className="text-muted-foreground py-8 text-center">
                    {t('empty')}
                  </TableCell>
                </TableRow>
              ) : (
                usagers.map((usager) => (
                  <TableRow key={usager.id}>
                    <TableCell className="font-medium">{usager.numero}</TableCell>
                    <TableCell>
                      {usager.prenom} {usager.nom}
                    </TableCell>
                    <TableCell>{usager.nationalite ?? '—'}</TableCell>
                    <TableCell>
                      <Badge variant="secondary">{usager.typeUsager}</Badge>
                    </TableCell>
                    <TableCell>{usager.telephone ?? '—'}</TableCell>
                    <TableCell>{usager.email ?? '—'}</TableCell>
                    <TableCell className="text-right">
                      <div className="flex justify-end gap-1">
                        <Button variant="ghost" size="icon" asChild>
                          <Link to={`/usagers/${usager.id}`}>
                            <Eye className="h-4 w-4" />
                          </Link>
                        </Button>
                        {canWrite && (
                          <Button
                            variant="ghost"
                            size="icon"
                            onClick={() => navigate(`/usagers/${usager.id}/edit`)}
                          >
                            <Pencil className="h-4 w-4" />
                          </Button>
                        )}
                        {canDelete && (
                          <Button
                            variant="ghost"
                            size="icon"
                            onClick={() => setDeleteTarget(usager)}
                          >
                            <Trash2 className="h-4 w-4 text-danger" />
                          </Button>
                        )}
                      </div>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </div>
      )}

      {!isSearching && totalPages > 1 && (
        <div className="flex items-center justify-between">
          <Button
            variant="outline"
            disabled={page === 0}
            onClick={() => setPage((p) => Math.max(0, p - 1))}
          >
            {t('pagination.previous')}
          </Button>
          <span className="text-muted-foreground text-sm">
            {t('pagination.page', { current: page + 1, total: totalPages })}
          </span>
          <Button
            variant="outline"
            disabled={page + 1 >= totalPages}
            onClick={() => setPage((p) => p + 1)}
          >
            {t('pagination.next')}
          </Button>
        </div>
      )}

      <ConfirmDialog
        open={Boolean(deleteTarget)}
        title={t('delete.title')}
        description={t('delete.description', {
          name: deleteTarget ? `${deleteTarget.prenom} ${deleteTarget.nom}` : '',
        })}
        confirmLabel={t('delete.confirm')}
        cancelLabel={t('delete.cancel')}
        onConfirm={handleDelete}
        onCancel={() => setDeleteTarget(null)}
        loading={deleteMutation.isPending}
      />
    </div>
  )
}

export default UsagersPage
