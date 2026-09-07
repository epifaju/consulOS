import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { ExternalLink, Trash2 } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
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
import { DocumentViewer } from '@/components/documents/DocumentViewer'
import {
  useDeleteDocument,
  useDocumentsList,
  useUpdateDocumentStatut,
} from '@/hooks/useDocuments'
import { useAuthStore } from '@/stores/authStore'
import {
  DOCUMENT_TYPES,
  STATUTS_CONTROLE,
  type StatutControle,
  type TypeDocument,
} from '@/types/document'
import { formatFileSize, getStatutControleBadgeClass } from '@/utils/documentStatus'

export function DocumentsPage() {
  const { t } = useTranslation('documents')
  const userRole = useAuthStore((s) => s.user?.role ?? '')
  const isAdmin = userRole.includes('ADMIN')

  const [page, setPage] = useState(0)
  const [search, setSearch] = useState('')
  const [dossierReference, setDossierReference] = useState('')
  const [typeDocument, setTypeDocument] = useState<TypeDocument | 'ALL'>('ALL')
  const [selectedStatuts, setSelectedStatuts] = useState<StatutControle[]>([])
  const [dateDebut, setDateDebut] = useState('')
  const [dateFin, setDateFin] = useState('')

  const listQuery = useDocumentsList({
    page,
    size: 20,
    sort: 'createdAt,desc',
    search: search.trim() || undefined,
    dossierReference: dossierReference.trim() || undefined,
    typeDocument: typeDocument !== 'ALL' ? typeDocument : undefined,
    statutControle: selectedStatuts.length > 0 ? selectedStatuts : undefined,
    dateDebut: dateDebut ? `${dateDebut}T00:00:00Z` : undefined,
    dateFin: dateFin ? `${dateFin}T23:59:59Z` : undefined,
  })

  const updateStatut = useUpdateDocumentStatut()
  const deleteMutation = useDeleteDocument()

  const documents = listQuery.data?.content ?? []
  const totalPages = listQuery.data?.totalPages ?? 1

  const toggleStatut = (statut: StatutControle) => {
    setSelectedStatuts((prev) =>
      prev.includes(statut) ? prev.filter((s) => s !== statut) : [...prev, statut],
    )
    setPage(0)
  }

  const handleStatutChange = (id: string, statut: StatutControle) => {
    updateStatut.mutate({ id, dto: { statut } })
  }

  const handleDelete = (id: string) => {
    if (window.confirm(t('list.confirmDelete'))) {
      deleteMutation.mutate(id)
    }
  }

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-semibold text-primary">{t('title')}</h1>

      <div className="grid gap-3 md:grid-cols-2 lg:grid-cols-4">
        <Input
          placeholder={t('filters.search')}
          value={search}
          onChange={(e) => {
            setSearch(e.target.value)
            setPage(0)
          }}
        />
        <Input
          placeholder={t('filters.dossierReference')}
          value={dossierReference}
          onChange={(e) => {
            setDossierReference(e.target.value)
            setPage(0)
          }}
        />
        <Select
          value={typeDocument}
          onValueChange={(v) => {
            setTypeDocument(v as TypeDocument | 'ALL')
            setPage(0)
          }}
        >
          <SelectTrigger>
            <SelectValue placeholder={t('filters.typeDocument')} />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="ALL">{t('filters.allTypes')}</SelectItem>
            {DOCUMENT_TYPES.map((type) => (
              <SelectItem key={type} value={type}>
                {t(`types.${type}`)}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        <div className="flex gap-2">
          <Input
            type="date"
            value={dateDebut}
            onChange={(e) => {
              setDateDebut(e.target.value)
              setPage(0)
            }}
            aria-label={t('filters.dateDebut')}
            className="flex-1"
          />
          <Input
            type="date"
            value={dateFin}
            onChange={(e) => {
              setDateFin(e.target.value)
              setPage(0)
            }}
            aria-label={t('filters.dateFin')}
            className="flex-1"
          />
        </div>
      </div>

      <div className="flex flex-wrap gap-2">
        {STATUTS_CONTROLE.map((statut) => (
          <button
            key={statut}
            type="button"
            onClick={() => toggleStatut(statut)}
            className={`rounded-full px-3 py-1 text-xs font-medium transition ${getStatutControleBadgeClass(statut)} ${
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
              <TableHead>{t('list.columns.name')}</TableHead>
              <TableHead>{t('list.columns.type')}</TableHead>
              <TableHead>{t('list.columns.dossier')}</TableHead>
              <TableHead>{t('list.columns.usager')}</TableHead>
              <TableHead>{t('list.columns.statut')}</TableHead>
              <TableHead>{t('list.columns.size')}</TableHead>
              <TableHead>{t('list.columns.date')}</TableHead>
              <TableHead className="text-right">{t('list.columns.actions')}</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {listQuery.isLoading ? (
              <TableRow>
                <TableCell colSpan={8} className="text-center text-muted-foreground">
                  {t('loading')}
                </TableCell>
              </TableRow>
            ) : documents.length === 0 ? (
              <TableRow>
                <TableCell colSpan={8} className="text-center text-muted-foreground">
                  {t('list.emptyGlobal')}
                </TableCell>
              </TableRow>
            ) : (
              documents.map((doc) => (
                <TableRow key={doc.id}>
                  <TableCell className="max-w-[180px] truncate font-medium">
                    {doc.nomOriginal}
                  </TableCell>
                  <TableCell>{t(`types.${doc.typeDocument}`, doc.typeDocument)}</TableCell>
                  <TableCell>
                    {doc.dossierId && doc.dossierReference ? (
                      <Link
                        to={`/dossiers/${doc.dossierId}`}
                        className="inline-flex items-center gap-1 text-accent hover:underline"
                      >
                        {doc.dossierReference}
                        <ExternalLink className="h-3 w-3" />
                      </Link>
                    ) : (
                      '—'
                    )}
                  </TableCell>
                  <TableCell>
                    {doc.usagerPrenom || doc.usagerNom
                      ? `${doc.usagerPrenom ?? ''} ${doc.usagerNom ?? ''}`.trim()
                      : '—'}
                  </TableCell>
                  <TableCell>
                    <Select
                      value={doc.statutControle}
                      onValueChange={(v) => handleStatutChange(doc.id, v as StatutControle)}
                    >
                      <SelectTrigger className="h-8 w-[140px] border-0 p-0 shadow-none">
                        <Badge className={getStatutControleBadgeClass(doc.statutControle)}>
                          {t(`statuts.${doc.statutControle}`)}
                        </Badge>
                      </SelectTrigger>
                      <SelectContent>
                        {STATUTS_CONTROLE.map((s) => (
                          <SelectItem key={s} value={s}>
                            {t(`statuts.${s}`)}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </TableCell>
                  <TableCell>{formatFileSize(doc.tailleOctets)}</TableCell>
                  <TableCell>{new Date(doc.createdAt).toLocaleDateString()}</TableCell>
                  <TableCell className="text-right">
                    <div className="flex items-center justify-end gap-1">
                      <DocumentViewer
                        documentId={doc.id}
                        mimeType={doc.mimeType}
                        nomOriginal={doc.nomOriginal}
                      />
                      {isAdmin && (
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => handleDelete(doc.id)}
                          disabled={deleteMutation.isPending}
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

export default DocumentsPage
