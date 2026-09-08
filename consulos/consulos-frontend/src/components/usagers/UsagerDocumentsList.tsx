import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { ExternalLink, Trash2 } from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
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
import { useDeleteDocument, useUpdateDocumentStatut } from '@/hooks/useDocuments'
import { useAuthStore } from '@/stores/authStore'
import type { UsagerDocumentSummary } from '@/types/usager'
import { STATUTS_CONTROLE, type StatutControle } from '@/types/document'
import { formatFileSize, getStatutControleBadgeClass } from '@/utils/documentStatus'

interface UsagerDocumentsListProps {
  documents: UsagerDocumentSummary[]
  isLoading: boolean
  usagerId?: string
}

export function UsagerDocumentsList({ documents, isLoading, usagerId }: UsagerDocumentsListProps) {
  const { t } = useTranslation(['usagers', 'documents'])
  const userRole = useAuthStore((s) => s.user?.role ?? '')
  const isAdmin = userRole.includes('ADMIN')

  const updateStatut = useUpdateDocumentStatut()
  const deleteMutation = useDeleteDocument()

  const handleStatutChange = (id: string, statut: StatutControle) => {
    updateStatut.mutate({ id, dto: { statut } })
  }

  const handleDelete = (id: string) => {
    if (window.confirm(t('documents:list.confirmDelete'))) {
      deleteMutation.mutate(id)
    }
  }

  if (isLoading) {
    return <p className="text-muted-foreground text-sm">{t('usagers:loading')}</p>
  }

  if (documents.length === 0) {
    return <p className="text-muted-foreground text-sm">{t('usagers:documents.empty')}</p>
  }

  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>{t('documents:list.columns.name')}</TableHead>
          <TableHead>{t('documents:list.columns.type')}</TableHead>
          <TableHead>{t('documents:list.columns.dossier')}</TableHead>
          <TableHead>{t('documents:list.columns.statut')}</TableHead>
          <TableHead>{t('documents:list.columns.size')}</TableHead>
          <TableHead>{t('documents:list.columns.date')}</TableHead>
          <TableHead className="text-right">{t('documents:list.columns.actions')}</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {documents.map((doc) => (
          <TableRow key={doc.id}>
            <TableCell className="max-w-[180px] truncate font-medium">
              {doc.nomOriginal}
            </TableCell>
            <TableCell>{t(`documents:types.${doc.typeDocument}`, doc.typeDocument)}</TableCell>
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
              <Select
                value={doc.statutControle}
                onValueChange={(v) => handleStatutChange(doc.id, v as StatutControle)}
              >
                <SelectTrigger className="h-8 w-[140px] border-0 p-0 shadow-none">
                  <Badge className={getStatutControleBadgeClass(doc.statutControle)}>
                    {t(`documents:statuts.${doc.statutControle}`)}
                  </Badge>
                </SelectTrigger>
                <SelectContent>
                  {STATUTS_CONTROLE.map((s) => (
                    <SelectItem key={s} value={s}>
                      {t(`documents:statuts.${s}`)}
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
                  usagerId={usagerId}
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
        ))}
      </TableBody>
    </Table>
  )
}

export default UsagerDocumentsList
