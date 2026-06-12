import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { ArrowLeft, Download, Printer } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { useEncaissement } from '@/hooks/useEncaissements'
import { encaissementService } from '@/services/encaissementService'

export function RecuPage() {
  const { t } = useTranslation('encaissements')
  const { id } = useParams<{ id: string }>()
  const encQuery = useEncaissement(id ?? '')
  const [pdfUrl, setPdfUrl] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!id) return

    let objectUrl: string | null = null
    setLoading(true)
    setError(null)

    encaissementService
      .getRecuPdf(id)
      .then((response) => {
        objectUrl = URL.createObjectURL(response.data)
        setPdfUrl(objectUrl)
      })
      .catch(() => setError(t('recu.error')))
      .finally(() => setLoading(false))

    return () => {
      if (objectUrl) {
        URL.revokeObjectURL(objectUrl)
      }
    }
  }, [id, t])

  const handleDownload = () => {
    if (!pdfUrl || !id) return
    const a = document.createElement('a')
    a.href = pdfUrl
    a.download = `recu-${encQuery.data?.numeroRecu ?? id}.pdf`
    a.click()
  }

  const handlePrint = () => {
    if (!pdfUrl) return
    const win = window.open(pdfUrl, '_blank')
    win?.print()
  }

  return (
    <div className="space-y-4">
      <Button variant="ghost" asChild className="px-0">
        <Link to="/caisse">
          <ArrowLeft className="mr-2 h-4 w-4" />
          {t('recu.back')}
        </Link>
      </Button>

      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-primary">{t('recu.title')}</h1>
          {encQuery.data && (
            <p className="text-muted-foreground font-mono text-sm">{encQuery.data.numeroRecu}</p>
          )}
        </div>
        <div className="flex gap-2">
          <Button variant="outline" onClick={handlePrint} disabled={!pdfUrl}>
            <Printer className="mr-2 h-4 w-4" />
            {t('recu.print')}
          </Button>
          <Button onClick={handleDownload} disabled={!pdfUrl} className="bg-primary">
            <Download className="mr-2 h-4 w-4" />
            {t('recu.download')}
          </Button>
        </div>
      </div>

      {loading && <p className="text-muted-foreground">{t('loading')}</p>}
      {error && <p className="text-danger text-sm">{error}</p>}
      {pdfUrl && (
        <iframe
          src={pdfUrl}
          title={t('recu.title')}
          className="h-[75vh] w-full rounded border bg-white"
        />
      )}
    </div>
  )
}

export default RecuPage
