import { useMemo, useState } from 'react'
import { Navigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { Loader2 } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { toast } from '@/hooks/use-toast'
import {
  usePortalCreateRdv,
  usePortalDisponibilites,
  usePortalDocuments,
  usePortalDossier,
  usePortalUploadDocument,
} from '@/hooks/usePortal'
import { usePortalStore } from '@/stores/portalStore'

const DOCUMENT_TYPES = ['PIECE_IDENTITE', 'JUSTIFICATIF', 'PHOTO', 'FORMULAIRE', 'AUTRE'] as const

export function PortalDossierPage() {
  const { t } = useTranslation('portal')
  const sessionToken = usePortalStore((s) => s.sessionToken)
  const clearSession = usePortalStore((s) => s.clearSession)
  const cachedDossier = usePortalStore((s) => s.dossier)

  const dossierQuery = usePortalDossier(Boolean(sessionToken))
  const documentsQuery = usePortalDocuments(Boolean(sessionToken))
  const uploadMutation = usePortalUploadDocument()
  const createRdv = usePortalCreateRdv()

  const [file, setFile] = useState<File | null>(null)
  const [typeDocument, setTypeDocument] = useState<string>('AUTRE')
  const [rdvDate, setRdvDate] = useState(() => new Date().toISOString().slice(0, 10))
  const slotsQuery = usePortalDisponibilites(rdvDate, Boolean(sessionToken))

  const dossier = dossierQuery.data ?? cachedDossier

  const tomorrowHint = useMemo(() => {
    const d = new Date()
    d.setDate(d.getDate() + 1)
    return d.toISOString().slice(0, 10)
  }, [])

  if (!sessionToken) {
    return <Navigate to="/portal" replace />
  }

  const handleUpload = () => {
    if (!file) return
    uploadMutation.mutate(
      { file, typeDocument },
      {
        onSuccess: () => {
          toast({ title: t('documents.success') })
          setFile(null)
        },
      },
    )
  }

  const bookSlot = (heureDebut: string) => {
    const time = String(heureDebut).slice(0, 5)
    const dateHeure = `${rdvDate}T${time}:00Z`
    createRdv.mutate(
      { dateHeure },
      {
        onSuccess: () => toast({ title: t('rdv.success') }),
      },
    )
  }

  return (
    <div className="min-h-screen bg-surface">
      <header className="flex h-16 items-center justify-between border-b bg-white px-4 md:px-8">
        <div>
          <p className="text-xs uppercase tracking-wide text-muted-foreground">{t('shell.title')}</p>
          <h1 className="text-lg font-semibold text-primary">
            {dossier?.reference ?? '…'}
          </h1>
        </div>
        <Button
          variant="outline"
          onClick={() => {
            clearSession()
            window.location.href = '/portal'
          }}
        >
          {t('shell.logout')}
        </Button>
      </header>

      <main className="mx-auto max-w-4xl space-y-6 p-4 md:p-8">
        <Card>
          <CardHeader>
            <CardTitle className="text-primary">
              {dossier
                ? `${dossier.usagerPrenom} ${dossier.usagerNom}`
                : t('dossier.citizen')}
            </CardTitle>
          </CardHeader>
          <CardContent className="grid gap-3 sm:grid-cols-2">
            {dossierQuery.isLoading && !dossier ? (
              <Loader2 className="h-5 w-5 animate-spin text-primary" />
            ) : (
              <>
                <div>
                  <p className="text-muted-foreground text-xs">{t('dossier.status')}</p>
                  <p className="font-medium">{dossier?.statut}</p>
                </div>
                <div>
                  <p className="text-muted-foreground text-xs">{t('dossier.service')}</p>
                  <p className="font-medium">{dossier?.typeServiceLibelle}</p>
                </div>
                <div>
                  <p className="text-muted-foreground text-xs">{t('dossier.submitted')}</p>
                  <p className="font-medium">
                    {dossier?.dateSoumission
                      ? new Date(dossier.dateSoumission).toLocaleDateString()
                      : '—'}
                  </p>
                </div>
                <div>
                  <p className="text-muted-foreground text-xs">{t('dossier.due')}</p>
                  <p className="font-medium">
                    {dossier?.dateEcheance
                      ? new Date(dossier.dateEcheance).toLocaleDateString()
                      : '—'}
                  </p>
                </div>
              </>
            )}
          </CardContent>
        </Card>

        <Tabs defaultValue="documents">
          <TabsList>
            <TabsTrigger value="documents">{t('documents.title')}</TabsTrigger>
            <TabsTrigger value="rdv">{t('rdv.title')}</TabsTrigger>
          </TabsList>

          <TabsContent value="documents" className="mt-4 space-y-4">
            <Card>
              <CardHeader>
                <CardTitle className="text-base">{t('documents.upload')}</CardTitle>
              </CardHeader>
              <CardContent className="grid gap-3 sm:grid-cols-2">
                <div>
                  <label className="mb-1 block text-sm font-medium">{t('documents.type')}</label>
                  <Select value={typeDocument} onValueChange={setTypeDocument}>
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      {DOCUMENT_TYPES.map((type) => (
                        <SelectItem key={type} value={type}>
                          {type}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
                <div>
                  <label className="mb-1 block text-sm font-medium">{t('documents.file')}</label>
                  <Input
                    type="file"
                    accept=".pdf,.jpg,.jpeg,.png,.webp"
                    onChange={(e) => setFile(e.target.files?.[0] ?? null)}
                  />
                </div>
                <Button
                  className="bg-primary sm:col-span-2"
                  disabled={!file || uploadMutation.isPending}
                  onClick={handleUpload}
                >
                  {uploadMutation.isPending ? (
                    <Loader2 className="h-4 w-4 animate-spin" />
                  ) : (
                    t('documents.submit')
                  )}
                </Button>
              </CardContent>
            </Card>

            <Card>
              <CardContent className="pt-6">
                {documentsQuery.isLoading ? (
                  <Loader2 className="h-5 w-5 animate-spin text-primary" />
                ) : (documentsQuery.data ?? []).length === 0 ? (
                  <p className="text-muted-foreground text-sm">{t('documents.empty')}</p>
                ) : (
                  <ul className="divide-y">
                    {(documentsQuery.data ?? []).map((doc) => (
                      <li key={doc.id} className="flex items-center justify-between py-3 text-sm">
                        <div>
                          <p className="font-medium">{doc.nomOriginal}</p>
                          <p className="text-muted-foreground text-xs">
                            {doc.typeDocument} · {doc.statutControle}
                          </p>
                        </div>
                        <span className="text-muted-foreground text-xs">
                          {(doc.tailleOctets / 1024).toFixed(0)} Ko
                        </span>
                      </li>
                    ))}
                  </ul>
                )}
              </CardContent>
            </Card>
          </TabsContent>

          <TabsContent value="rdv" className="mt-4 space-y-4">
            <Card>
              <CardHeader>
                <CardTitle className="text-base">{t('rdv.title')}</CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                <div>
                  <label className="mb-1 block text-sm font-medium">{t('rdv.date')}</label>
                  <Input
                    type="date"
                    min={tomorrowHint}
                    value={rdvDate}
                    onChange={(e) => setRdvDate(e.target.value)}
                  />
                </div>
                <div>
                  <p className="mb-2 text-sm font-medium">{t('rdv.slots')}</p>
                  {slotsQuery.isLoading ? (
                    <Loader2 className="h-5 w-5 animate-spin text-primary" />
                  ) : (slotsQuery.data ?? []).filter((s) => s.placesRestantes > 0).length === 0 ? (
                    <p className="text-muted-foreground text-sm">{t('rdv.empty')}</p>
                  ) : (
                    <div className="grid gap-2 sm:grid-cols-2">
                      {(slotsQuery.data ?? [])
                        .filter((s) => s.placesRestantes > 0)
                        .map((slot) => (
                          <Button
                            key={`${slot.heureDebut}-${slot.heureFin}`}
                            variant="outline"
                            className="justify-between"
                            disabled={createRdv.isPending}
                            onClick={() => bookSlot(String(slot.heureDebut))}
                          >
                            <span>
                              {String(slot.heureDebut).slice(0, 5)} – {String(slot.heureFin).slice(0, 5)}
                            </span>
                            <span className="text-muted-foreground text-xs">
                              {t('rdv.places', {
                                restantes: slot.placesRestantes,
                                max: slot.quotaMax,
                              })}
                            </span>
                          </Button>
                        ))}
                    </div>
                  )}
                </div>
              </CardContent>
            </Card>
          </TabsContent>
        </Tabs>
      </main>
    </div>
  )
}

export default PortalDossierPage
