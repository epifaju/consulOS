import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import axios from 'axios'
import { Download, Expand, Eye, Sparkles } from 'lucide-react'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { documentService } from '@/services/documentService'
import { useDocumentOcr, useOcrSuggestions } from '@/hooks/useOcr'
import { resolveApiErrorMessage } from '@/lib/apiErrors'
import { toast } from '@/hooks/use-toast'

interface DocumentViewerProps {
  documentId: string
  mimeType: string | null
  nomOriginal: string
  usagerId?: string | null
}

async function resolveBlobAwareError(error: unknown, fallback: string): Promise<string> {
  if (axios.isAxiosError(error) && error.response?.data instanceof Blob) {
    try {
      const text = await error.response.data.text()
      const json = JSON.parse(text) as { message?: string }
      if (json.message) {
        return json.message
      }
    } catch {
      // ignore parse errors
    }
  }
  const msg = resolveApiErrorMessage(error)
  return msg || fallback
}

export function DocumentViewer({
  documentId,
  mimeType,
  nomOriginal,
  usagerId,
}: DocumentViewerProps) {
  const { t } = useTranslation('documents')
  const navigate = useNavigate()
  const [open, setOpen] = useState(false)
  const [fullscreen, setFullscreen] = useState(false)
  const [url, setUrl] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const isPdf = mimeType === 'application/pdf'
  const isImage = mimeType?.startsWith('image/') ?? false
  const ocrQuery = useDocumentOcr(documentId, open)
  const suggestionsQuery = useOcrSuggestions(
    documentId,
    open && ocrQuery.data?.status === 'DONE',
  )

  useEffect(() => {
    return () => {
      if (url?.startsWith('blob:')) {
        URL.revokeObjectURL(url)
      }
    }
  }, [url])

  const fetchBlobUrl = async () => {
    setLoading(true)
    setError(null)
    try {
      const response = await documentService.getContent(documentId, 'inline')
      if (url?.startsWith('blob:')) {
        URL.revokeObjectURL(url)
      }
      const objectUrl = URL.createObjectURL(response.data)
      setUrl(objectUrl)
      return objectUrl
    } catch (contentErr) {
      // Fallback : URL présignée MinIO si l'endpoint /content n'est pas dispo / en erreur
      try {
        const response = await documentService.getDownloadUrl(documentId)
        const presigned = response.data.data.url
        setUrl(presigned)
        return presigned
      } catch {
        const message = await resolveBlobAwareError(contentErr, t('viewer.error'))
        setError(message)
        toast({ variant: 'destructive', title: message })
        return null
      }
    } finally {
      setLoading(false)
    }
  }

  const handleView = async () => {
    const objectUrl = await fetchBlobUrl()
    if (objectUrl) {
      setOpen(true)
    }
  }

  const handleDownload = async () => {
    setLoading(true)
    setError(null)
    try {
      const response = await documentService.getContent(documentId, 'attachment')
      const objectUrl = URL.createObjectURL(response.data)
      const anchor = document.createElement('a')
      anchor.href = objectUrl
      anchor.download = nomOriginal
      anchor.rel = 'noopener'
      document.body.appendChild(anchor)
      anchor.click()
      anchor.remove()
      URL.revokeObjectURL(objectUrl)
    } catch {
      try {
        const response = await documentService.getDownloadUrl(documentId)
        window.open(response.data.data.url, '_blank', 'noopener,noreferrer')
      } catch (err) {
        const message = await resolveBlobAwareError(err, t('viewer.error'))
        setError(message)
        toast({ variant: 'destructive', title: message })
      }
    } finally {
      setLoading(false)
    }
  }

  const handlePrefill = () => {
    const target = usagerId ? `/usagers/${usagerId}/edit` : '/usagers/new'
    const params = new URLSearchParams({ ocrDocId: documentId })
    setOpen(false)
    void navigate(`${target}?${params.toString()}`)
  }

  const canPrefill =
    ocrQuery.data?.status === 'DONE' &&
    suggestionsQuery.data != null &&
    suggestionsQuery.data.confidence > 0

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
            <div className="mb-2 flex flex-wrap items-center justify-between gap-2">
              <p className="text-sm font-medium text-primary">{t('viewer.ocr')}</p>
              {canPrefill && (
                <Button size="sm" variant="outline" onClick={handlePrefill}>
                  <Sparkles className="mr-1 h-4 w-4" />
                  {usagerId ? t('viewer.ocrPrefillEdit') : t('viewer.ocrPrefillCreate')}
                </Button>
              )}
            </div>
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
            {suggestionsQuery.data && suggestionsQuery.data.confidence > 0 && (
              <p className="text-muted-foreground mt-2 text-xs">
                {t('viewer.ocrConfidence', {
                  value: Math.round(suggestionsQuery.data.confidence * 100),
                })}
              </p>
            )}
          </div>
        </DialogContent>
      </Dialog>
    </>
  )
}

export default DocumentViewer
