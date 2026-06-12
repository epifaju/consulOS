import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Plus, Trash2 } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
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
import { DocumentUploadZone } from '@/components/documents/DocumentUploadZone'
import { DocumentViewer } from '@/components/documents/DocumentViewer'
import {
  useDeleteDocument,
  useDossierDocuments,
  useUpdateDocumentStatut,
} from '@/hooks/useDocuments'
import { useAuthStore } from '@/stores/authStore'
import { formatFileSize, getStatutControleBadgeClass } from '@/utils/documentStatus'
import type { StatutControle } from '@/types/document'

interface DocumentListProps {
  dossierId: string
  usagerId?: string
}

export function DocumentList({ dossierId, usagerId }: DocumentListProps) {
  const { t } = useTranslation('documents')
  const userRole = useAuthStore((s) => s.user?.role ?? '')
  const isAdmin = userRole.includes('ADMIN')

  const [uploadOpen, setUploadOpen] = useState(false)
  const { data: documents, isLoading } = useDossierDocuments(dossierId)
  const updateStatut = useUpdateDocumentStatut(dossierId)
  const deleteMutation = useDeleteDocument(dossierId)

  const handleStatutChange = (id: string, statut: StatutControle) => {
    updateStatut.mutate({ id, dto: { statut } })
  }

  const handleDelete = (id: string) => {
    if (window.confirm(t('list.confirmDelete'))) {
      deleteMutation.mutate(id)
    }
  }

  return (
    <div className="space-y-4">
      <div className="flex justify-end">
        <Button onClick={() => setUploadOpen(true)} className="bg-primary">
          <Plus className="mr-2 h-4 w-4" />
          {t('list.add')}
        </Button>
      </div>

      <div className="rounded-lg border bg-white">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>{t('list.columns.name')}</TableHead>
              <TableHead>{t('list.columns.type')}</TableHead>
              <TableHead>{t('list.columns.statut')}</TableHead>
              <TableHead>{t('list.columns.size')}</TableHead>
              <TableHead>{t('list.columns.date')}</TableHead>
              <TableHead className="text-right">{t('list.columns.actions')}</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {isLoading ? (
              <TableRow>
                <TableCell colSpan={6} className="text-center text-muted-foreground">
                  {t('loading')}
                </TableCell>
              </TableRow>
            ) : (documents ?? []).length === 0 ? (
              <TableRow>
                <TableCell colSpan={6} className="text-center text-muted-foreground">
                  {t('list.empty')}
                </TableCell>
              </TableRow>
            ) : (
              (documents ?? []).map((doc) => (
                <TableRow key={doc.id}>
                  <TableCell className="max-w-[180px] truncate font-medium">
                    {doc.nomOriginal}
                  </TableCell>
                  <TableCell>{t(`types.${doc.typeDocument}`, doc.typeDocument)}</TableCell>
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
                        {(['RECU', 'LISIBLE', 'COMPLET', 'A_CORRIGER', 'VALIDE'] as StatutControle[]).map(
                          (s) => (
                            <SelectItem key={s} value={s}>
                              {t(`statuts.${s}`)}
                            </SelectItem>
                          ),
                        )}
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

      <Dialog open={uploadOpen} onOpenChange={setUploadOpen}>
        <DialogContent className="max-w-lg">
          <DialogHeader>
            <DialogTitle>{t('upload.title')}</DialogTitle>
          </DialogHeader>
          <DocumentUploadZone
            dossierId={dossierId}
            usagerId={usagerId}
            onUploadSuccess={() => setUploadOpen(false)}
          />
        </DialogContent>
      </Dialog>
    </div>
  )
}

export default DocumentList
