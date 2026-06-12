import { useCallback, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { FileText, ImageIcon, Upload } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { useUploadDocument } from '@/hooks/useDocuments'
import { toast } from '@/hooks/use-toast'
import type { TypeDocument } from '@/types/document'

const ACCEPTED_TYPES: Record<string, string[]> = {
  'application/pdf': ['.pdf'],
  'image/jpeg': ['.jpg', '.jpeg'],
  'image/png': ['.png'],
  'image/webp': ['.webp'],
}
const MAX_SIZE = 10_485_760

interface DocumentUploadZoneProps {
  dossierId: string
  usagerId?: string
  onUploadSuccess?: () => void
}

export function DocumentUploadZone({ dossierId, usagerId, onUploadSuccess }: DocumentUploadZoneProps) {
  const { t } = useTranslation('documents')
  const fileInputRef = useRef<HTMLInputElement>(null)
  const [dragOver, setDragOver] = useState(false)
  const [file, setFile] = useState<File | null>(null)
  const [previewUrl, setPreviewUrl] = useState<string | null>(null)
  const [typeDocument, setTypeDocument] = useState<TypeDocument>('AUTRE')
  const [notes, setNotes] = useState('')
  const [progress, setProgress] = useState(0)
  const [error, setError] = useState<string | null>(null)

  const uploadMutation = useUploadDocument(dossierId)

  const validateFile = useCallback(
    (candidate: File): string | null => {
      if (!Object.keys(ACCEPTED_TYPES).includes(candidate.type)) {
        return t('upload.errors.mime')
      }
      if (candidate.size > MAX_SIZE) {
        return t('upload.errors.size')
      }
      return null
    },
    [t],
  )

  const selectFile = useCallback(
    (candidate: File) => {
      const validationError = validateFile(candidate)
      if (validationError) {
        setError(validationError)
        setFile(null)
        setPreviewUrl(null)
        return
      }
      setError(null)
      setFile(candidate)
      if (candidate.type.startsWith('image/')) {
        setPreviewUrl(URL.createObjectURL(candidate))
      } else {
        setPreviewUrl(null)
      }
    },
    [validateFile],
  )

  const onDrop = useCallback(
    (e: React.DragEvent) => {
      e.preventDefault()
      setDragOver(false)
      const dropped = e.dataTransfer.files[0]
      if (dropped) selectFile(dropped)
    },
    [selectFile],
  )

  const handleUpload = () => {
    if (!file) return
    uploadMutation.mutate(
      {
        file,
        dossierId,
        usagerId,
        typeDocument,
        notes: notes || undefined,
        onProgress: setProgress,
      },
      {
        onSuccess: () => {
          toast({ title: t('upload.success') })
          setFile(null)
          setPreviewUrl(null)
          setNotes('')
          setProgress(0)
          onUploadSuccess?.()
        },
        onError: () => {
          setError(t('upload.errors.network'))
        },
      },
    )
  }

  return (
    <div className="space-y-4">
      <div
        role="button"
        tabIndex={0}
        onDragOver={(e) => {
          e.preventDefault()
          setDragOver(true)
        }}
        onDragLeave={() => setDragOver(false)}
        onDrop={onDrop}
        onClick={() => fileInputRef.current?.click()}
        onKeyDown={(e) => e.key === 'Enter' && fileInputRef.current?.click()}
        className={`flex cursor-pointer flex-col items-center justify-center rounded-lg border-2 border-dashed p-8 transition ${
          dragOver ? 'border-accent bg-light' : 'border-gray-300 hover:border-accent'
        }`}
      >
        <Upload className="mb-2 h-8 w-8 text-muted-foreground" />
        <p className="text-sm font-medium">{t('upload.dropzone')}</p>
        <p className="text-muted-foreground text-xs">{t('upload.hint')}</p>
        <input
          ref={fileInputRef}
          type="file"
          className="hidden"
          accept=".pdf,.jpg,.jpeg,.png,.webp"
          onChange={(e) => {
            const selected = e.target.files?.[0]
            if (selected) selectFile(selected)
          }}
        />
      </div>

      {file && (
        <div className="flex items-center gap-3 rounded-md border p-3">
          {previewUrl ? (
            <img src={previewUrl} alt="" className="h-12 w-12 rounded object-cover" />
          ) : file.type === 'application/pdf' ? (
            <FileText className="h-12 w-12 text-danger" />
          ) : (
            <ImageIcon className="h-12 w-12 text-accent" />
          )}
          <div className="min-w-0 flex-1">
            <p className="truncate text-sm font-medium">{file.name}</p>
            <p className="text-muted-foreground text-xs">{(file.size / 1024).toFixed(1)} Ko</p>
          </div>
        </div>
      )}

      {error && <p className="text-danger text-sm">{error}</p>}

      {uploadMutation.isPending && (
        <div className="space-y-1">
          <div className="h-2 w-full overflow-hidden rounded-full bg-gray-200">
            <div
              className="h-full bg-accent transition-all"
              style={{ width: `${progress}%` }}
            />
          </div>
          <p className="text-muted-foreground text-xs">{progress}%</p>
        </div>
      )}

      <div className="grid gap-3 sm:grid-cols-2">
        <div>
          <label className="mb-1 block text-sm font-medium">{t('upload.typeDocument')}</label>
          <Select value={typeDocument} onValueChange={(v) => setTypeDocument(v as TypeDocument)}>
            <SelectTrigger>
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="PIECE_IDENTITE">{t('types.PIECE_IDENTITE')}</SelectItem>
              <SelectItem value="JUSTIFICATIF">{t('types.JUSTIFICATIF')}</SelectItem>
              <SelectItem value="PHOTO">{t('types.PHOTO')}</SelectItem>
              <SelectItem value="FORMULAIRE">{t('types.FORMULAIRE')}</SelectItem>
              <SelectItem value="AUTRE">{t('types.AUTRE')}</SelectItem>
            </SelectContent>
          </Select>
        </div>
        <div>
          <label className="mb-1 block text-sm font-medium">{t('upload.notes')}</label>
          <Input value={notes} onChange={(e) => setNotes(e.target.value)} />
        </div>
      </div>

      <Button
        onClick={handleUpload}
        disabled={!file || uploadMutation.isPending}
        className="w-full bg-primary"
      >
        {uploadMutation.isPending ? t('upload.uploading') : t('upload.submit')}
      </Button>
    </div>
  )
}

export default DocumentUploadZone
