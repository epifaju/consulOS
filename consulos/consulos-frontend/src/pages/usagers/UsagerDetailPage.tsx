import { Link, useNavigate, useParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { ArrowLeft, Download, Pencil } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { UsagerActivityTimeline } from '@/components/usagers/UsagerActivityTimeline'
import { UsagerDocumentsList } from '@/components/usagers/UsagerDocumentsList'
import { UsagerDossiersList } from '@/components/usagers/UsagerDossiersList'
import { useExportUsagerFichePdf, useExportUsagerRgpd } from '@/hooks/useUsagerExports'
import { useUsagerActivity, useUsagerDocuments, useUsagerDossiers } from '@/hooks/useUsagerQueries'
import { useUsager } from '@/hooks/useUsagers'
import { useAuthStore } from '@/stores/authStore'
import { canExportUsagerFichePdf, canExportUsagerRgpd, canWriteUsagers } from '@/utils/rolePermissions'

function InfoField({ label, value }: { label: string; value: string | null | undefined }) {
  return (
    <div>
      <p className="text-muted-foreground text-xs">{label}</p>
      <p className="text-sm font-medium">{value || '—'}</p>
    </div>
  )
}

export function UsagerDetailPage() {
  const { id } = useParams<{ id: string }>()
  const { t } = useTranslation('usagers')
  const navigate = useNavigate()
  const { data: usager, isLoading } = useUsager(id ?? '')
  const exportRgpd = useExportUsagerRgpd()
  const exportFichePdf = useExportUsagerFichePdf()
  const dossiersQuery = useUsagerDossiers(id ?? '')
  const documentsQuery = useUsagerDocuments(id ?? '')
  const activityQuery = useUsagerActivity(id ?? '')
  const userRole = useAuthStore((state) => state.user?.role)
  const canWrite = canWriteUsagers(userRole)
  const canExportRgpd = canExportUsagerRgpd(userRole)
  const canExportPdf = canExportUsagerFichePdf(userRole)

  if (isLoading) {
    return <p className="text-muted-foreground">{t('loading')}</p>
  }

  if (!usager) {
    return <p className="text-danger">{t('notFound')}</p>
  }

  return (
    <div className="space-y-6">
      <Button variant="ghost" asChild className="px-0">
        <Link to="/usagers">
          <ArrowLeft className="mr-2 h-4 w-4" />
          {t('backToList')}
        </Link>
      </Button>

      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <p className="text-muted-foreground text-sm">{usager.numero}</p>
          <h1 className="text-2xl font-semibold text-primary">
            {usager.prenom} {usager.nom}
          </h1>
          <div className="mt-2 flex gap-2">
            <Badge>{usager.typeUsager}</Badge>
            {usager.nationalite && <Badge variant="outline">{usager.nationalite}</Badge>}
          </div>
        </div>
        <div className="flex flex-wrap gap-2">
          {canExportPdf && (
            <Button
              variant="outline"
              disabled={exportFichePdf.isPending}
              onClick={() => exportFichePdf.mutate(usager.id)}
            >
              <Download className="mr-2 h-4 w-4" />
              {exportFichePdf.isPending ? t('actions.exportFichePdfPending') : t('actions.exportFichePdf')}
            </Button>
          )}
          {canExportRgpd && (
            <Button
              variant="outline"
              disabled={exportRgpd.isPending}
              onClick={() => exportRgpd.mutate(usager.id)}
            >
              <Download className="mr-2 h-4 w-4" />
              {exportRgpd.isPending ? t('actions.exportRgpdPending') : t('actions.exportRgpd')}
            </Button>
          )}
          {canWrite && (
            <Button onClick={() => navigate(`/usagers/${usager.id}/edit`)}>
              <Pencil className="mr-2 h-4 w-4" />
              {t('actions.edit')}
            </Button>
          )}
        </div>
      </div>

      <Tabs defaultValue="info">
        <TabsList>
          <TabsTrigger value="info">{t('tabs.info')}</TabsTrigger>
          <TabsTrigger value="dossiers">{t('tabs.dossiers')}</TabsTrigger>
          <TabsTrigger value="documents">{t('tabs.documents')}</TabsTrigger>
          <TabsTrigger value="history">{t('tabs.history')}</TabsTrigger>
        </TabsList>

        <TabsContent value="info">
          <Card>
            <CardHeader>
              <CardTitle>{t('tabs.info')}</CardTitle>
            </CardHeader>
            <CardContent className="grid gap-4 sm:grid-cols-2">
              <InfoField label={t('fields.nom')} value={usager.nom} />
              <InfoField label={t('fields.prenom')} value={usager.prenom} />
              <InfoField label={t('fields.dateNaissance')} value={usager.dateNaissance} />
              <InfoField label={t('fields.lieuNaissance')} value={usager.lieuNaissance} />
              <InfoField label={t('fields.nationalite')} value={usager.nationalite} />
              <InfoField label={t('fields.typeUsager')} value={usager.typeUsager} />
              <InfoField label={t('fields.telephone')} value={usager.telephone} />
              <InfoField label={t('fields.email')} value={usager.email} />
              <InfoField label={t('fields.adresse')} value={usager.adresse} />
              <InfoField label={t('fields.numeroDocument')} value={usager.numeroDocument} />
              <InfoField label={t('fields.typeDocument')} value={usager.typeDocument} />
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="dossiers">
          <Card>
            <CardHeader>
              <CardTitle>{t('tabs.dossiers')}</CardTitle>
            </CardHeader>
            <CardContent>
              <UsagerDossiersList
                dossiers={dossiersQuery.data ?? []}
                isLoading={dossiersQuery.isLoading}
              />
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="documents">
          <Card>
            <CardHeader>
              <CardTitle>{t('tabs.documents')}</CardTitle>
            </CardHeader>
            <CardContent>
              <UsagerDocumentsList
                documents={documentsQuery.data ?? []}
                isLoading={documentsQuery.isLoading}
                usagerId={usager.id}
              />
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="history">
          <Card>
            <CardHeader>
              <CardTitle>{t('tabs.history')}</CardTitle>
            </CardHeader>
            <CardContent>
              <UsagerActivityTimeline
                entries={activityQuery.data ?? []}
                isLoading={activityQuery.isLoading}
              />
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>
    </div>
  )
}

export default UsagerDetailPage
