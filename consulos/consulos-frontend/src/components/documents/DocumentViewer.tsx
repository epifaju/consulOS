import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Download, Expand, Eye } from 'lucide-react'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { documentService } from '@/services/documentService'
import { useDocumentOcr } from '@/hooks/useOcr'

interface DocumentViewerProps {
  documentId: string
  mimeType: string | null
  nomOriginal: string
}

export function DocumentViewer({ documentId, mimeType, nomOriginal }: DocumentViewerProps) {
  const { t } = useTranslation('documents')
  const [open, setOpen] = useState(false)
  const [fullscreen, setFullscreen] = useState(false)
  const [url, setUrl] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const isPdf = mimeType === 'application/pdf'
  const isImage = mimeType?.startsWith('image/') ?? false
  const ocrQuery = useDocumentOcr(documentId, open)

  const fetchUrl = async () => {
    setLoading(true)
    setError(null)
    try {
      const response = await documentService.getDownloadUrl(documentId)
      return response.data.data.url
    } catch {
      setError(t('viewer.error'))
      return null
    } finally {
      setLoading(false)
    }
  }

  const handleView = async () => {
    const presignedUrl = await fetchUrl()
    if (presignedUrl) {
      setUrl(presignedUrl)
      setOpen(true)
    }
  }

  const handleDownload = async () => {
    const presignedUrl = url ?? (await fetchUrl())
    if (presignedUrl) {
      window.open(presignedUrl, '_blank', 'noopener,noreferrer')
    }
  }

  return (
    <>
      <div className="flex gap-1">
        <Button variant="ghost" size="sm" onClick={() => void handleView()} disabled={loading}>
          <Eye className="h-4 w-4" />
        </Button>
        <Button variant="ghost" size="sm" onClick={() => void handleDownload()} disabled={loading}>
          <Download className="h-4 w-4" />
        </Button>
      </div>

      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent className={fullscreen ? 'max-h-[95vh] max-w-[95vw]' : 'max-w-3xl'}>
          <DialogHeader className="flex flex-row items-center justify-between">
            <DialogTitle className="truncate pr-8">{nomOriginal}</DialogTitle>
            <Button variant="outline" size="sm" onClick={() => setFullscreen((f) => !f)}>
              <Expand className="mr-1 h-4 w-4" />
              {t('viewer.fullscreen')}
            </Button>
          </DialogHeader>
          {error && <p className="text-danger text-sm">{error}</p>}
          {url && isPdf && (
            <iframe
              src={url}
              title={nomOriginal}
              className={fullscreen ? 'h-[80vh] w-full' : 'h-[60vh] w-full rounded border'}
            />
          )}
          {url && isImage && (
            <img
              src={url}
              alt={nomOriginal}
              className={fullscreen ? 'max-h-[80vh] w-full object-contain' : 'max-h-[60vh] w-full rounded object-contain'}
            />
          )}
          {url && !isPdf && !isImage && (
            <p className="text-muted-foreground text-sm">{t('viewer.noPreview')}</p>
          )}
          <div className="mt-4 rounded-md border bg-surface p-3">
            <p className="mb-1 text-sm font-medium text-primary">{t('viewer.ocr')}</p>
            {ocrQuery.isLoading ? (
              <p className="text-muted-foreground text-xs">{t('loading')}</p>
            ) : ocrQuery.data?.status === 'DONE' ? (
              <pre className="max-h-40 overflow-auto whitespace-pre-wrap text-xs">
                {ocrQuery.data.extractedText}
              </pre>
            ) : ocrQuery.data?.status === 'FAILED' ? (
              <p className="text-danger text-xs">{t('viewer.ocrFailed')}</p>
            ) : (
              <p className="text-muted-foreground text-xs">{t('viewer.ocrEmpty')}</p>
            )}
          </div>
        </DialogContent>
      </Dialog>
    </>
  )
}

export default DocumentViewer
