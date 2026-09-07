import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { ArrowLeft } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Input } from '@/components/ui/input'
import {
  useAddDossierCommentaire,
  useDossier,
  useDossierHistorique,
  useDossierTransition,
} from '@/hooks/useDossiers'
import { WorkflowTimeline } from '@/components/dossiers/WorkflowTimeline'
import { ChecklistViewer } from '@/components/dossiers/ChecklistViewer'
import { AssignAgentPanel } from '@/components/dossiers/AssignAgentPanel'
import { DocumentList } from '@/components/documents/DocumentList'
import { getStatutBadgeClass } from '@/utils/dossierStatus'
import { useAuthStore } from '@/stores/authStore'
import { canAssignDossierAgent } from '@/utils/rolePermissions'
import type { DossierStatut } from '@/types/dossier'

function hasRole(role: string, ...allowed: string[]): boolean {
  return allowed.some((a) => role.includes(a))
}

interface TransitionAction {
  label: string
  statut: DossierStatut
  variant?: 'default' | 'destructive' | 'outline'
  requiresMotif?: boolean
}

export function DossierDetailPage() {
  const { id } = useParams<{ id: string }>()
  const { t } = useTranslation('dossiers')
  const userRole = useAuthStore((s) => s.user?.role ?? '')
  const [motifDialog, setMotifDialog] = useState<TransitionAction | null>(null)
  const [motif, setMotif] = useState('')
  const [commentaire, setCommentaire] = useState('')

  const { data: dossier, isLoading } = useDossier(id ?? '')
  const historiqueQuery = useDossierHistorique(id ?? '')
  const transitionMutation = useDossierTransition(id ?? '')
  const commentaireMutation = useAddDossierCommentaire(id ?? '')

  if (isLoading) {
    return <p className="text-muted-foreground">{t('loading')}</p>
  }

  if (!dossier) {
    return <p className="text-danger">{t('notFound')}</p>
  }

  const getActions = (): TransitionAction[] => {
    const actions: TransitionAction[] = []
    const statut = dossier.statut

    if (statut === 'BROUILLON' && hasRole(userRole, 'AGENT', 'ADMIN', 'SUPERVISEUR')) {
      actions.push({ label: t('actions.soumettre'), statut: 'SOUMIS' })
    }
    if (statut === 'SOUMIS' && hasRole(userRole, 'AGENT', 'ADMIN', 'SUPERVISEUR')) {
      actions.push({
        label: t('actions.incomplet'),
        statut: 'INCOMPLET',
        variant: 'outline',
        requiresMotif: true,
      })
      actions.push({ label: t('actions.enRevue'), statut: 'EN_REVUE' })
    }
    if (statut === 'EN_REVUE' && hasRole(userRole, 'SUPERVISEUR', 'ADMIN')) {
      actions.push({ label: t('actions.valider'), statut: 'VALIDE' })
      actions.push({
        label: t('actions.rejeter'),
        statut: 'REJETE',
        variant: 'destructive',
        requiresMotif: true,
      })
      actions.push({
        label: t('actions.retourIncomplet'),
        statut: 'INCOMPLET',
        variant: 'outline',
        requiresMotif: true,
      })
    }
    if (statut === 'VALIDE' && hasRole(userRole, 'AGENT', 'ADMIN', 'SUPERVISEUR')) {
      actions.push({ label: t('actions.cloturer'), statut: 'CLOTURE' })
    }

    return actions
  }

  const handleTransition = (action: TransitionAction) => {
    if (action.requiresMotif) {
      setMotifDialog(action)
      return
    }
    transitionMutation.mutate({ statut: action.statut })
  }

  const confirmMotifTransition = () => {
    if (!motifDialog || !motif.trim()) return
    transitionMutation.mutate(
      { statut: motifDialog.statut, motif: motif.trim() },
      {
        onSuccess: () => {
          setMotifDialog(null)
          setMotif('')
        },
      },
    )
  }

  const handleAddCommentaire = () => {
    if (!commentaire.trim()) return
    commentaireMutation.mutate(commentaire.trim(), {
      onSuccess: () => setCommentaire(''),
    })
  }

  const actions = getActions()
  const canAssign = canAssignDossierAgent(userRole)

  return (
    <div className="space-y-6">
      <Button variant="ghost" asChild className="px-0">
        <Link to="/dossiers">
          <ArrowLeft className="mr-2 h-4 w-4" />
          {t('backToList')}
        </Link>
      </Button>

      <div className="sticky top-0 z-10 -mx-4 border-b bg-background/95 px-4 py-4 backdrop-blur sm:-mx-6 sm:px-6">
        <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <p className="text-2xl font-semibold text-primary">{dossier.reference}</p>
            <div className="mt-2 flex flex-wrap items-center gap-2">
              <Badge className={getStatutBadgeClass(dossier.statut)}>
                {t(`statuts.${dossier.statut}`)}
              </Badge>
              <span className="text-muted-foreground text-sm">
                <Link to={`/usagers/${dossier.usagerId}`} className="text-accent hover:underline">
                  {dossier.usagerPrenom} {dossier.usagerNom}
                </Link>
                {' · '}
                {dossier.typeServiceLibelle}
                {dossier.agentNom ? ` · ${dossier.agentNom}` : ''}
              </span>
            </div>
          </div>
          <div className="flex flex-wrap gap-2">
            {actions.map((action) => (
              <Button
                key={action.statut}
                variant={action.variant ?? 'default'}
                size="sm"
                disabled={transitionMutation.isPending}
                onClick={() => handleTransition(action)}
              >
                {action.label}
              </Button>
            ))}
          </div>
        </div>
      </div>

      {motifDialog && (
        <Card className="border-warning">
          <CardHeader>
            <CardTitle className="text-base">{motifDialog.label}</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            <Input
              placeholder={t('actions.motifPlaceholder')}
              value={motif}
              onChange={(e) => setMotif(e.target.value)}
            />
            <div className="flex gap-2">
              <Button onClick={confirmMotifTransition} disabled={!motif.trim()}>
                {t('actions.confirm')}
              </Button>
              <Button variant="outline" onClick={() => setMotifDialog(null)}>
                {t('actions.cancel')}
              </Button>
            </div>
          </CardContent>
        </Card>
      )}

      <Tabs defaultValue="info">
        <TabsList>
          <TabsTrigger value="info">{t('tabs.info')}</TabsTrigger>
          <TabsTrigger value="documents">{t('tabs.documents')}</TabsTrigger>
          <TabsTrigger value="historique">{t('tabs.historique')}</TabsTrigger>
          <TabsTrigger value="commentaires">{t('tabs.commentaires')}</TabsTrigger>
        </TabsList>

        <TabsContent value="info" className="mt-4 space-y-4">
          {canAssign && (
            <Card>
              <CardContent className="pt-6">
                <AssignAgentPanel
                  dossierId={dossier.id}
                  currentAgentId={dossier.agentId}
                />
              </CardContent>
            </Card>
          )}
          <Card>
            <CardContent className="grid gap-4 pt-6 sm:grid-cols-2">
              <div>
                <p className="text-muted-foreground text-xs">{t('fields.priorite')}</p>
                <p className="font-medium">{t(`priorites.${dossier.priorite}`)}</p>
              </div>
              <div>
                <p className="text-muted-foreground text-xs">{t('fields.echeance')}</p>
                <p className="font-medium">
                  {dossier.dateEcheance
                    ? new Date(dossier.dateEcheance).toLocaleString()
                    : '—'}
                </p>
              </div>
              <div>
                <p className="text-muted-foreground text-xs">{t('fields.soumission')}</p>
                <p className="font-medium">
                  {dossier.dateSoumission
                    ? new Date(dossier.dateSoumission).toLocaleString()
                    : '—'}
                </p>
              </div>
              <div>
                <p className="text-muted-foreground text-xs">{t('fields.motifRejet')}</p>
                <p className="font-medium">{dossier.motifRejet ?? '—'}</p>
              </div>
              <div className="sm:col-span-2">
                <p className="text-muted-foreground text-xs">{t('fields.notes')}</p>
                <p className="font-medium">{dossier.notesInternes ?? '—'}</p>
              </div>
              <div className="sm:col-span-2">
                <p className="mb-2 font-medium">{t('checklist.title')}</p>
                <ChecklistViewer items={dossier.checklist} />
              </div>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="documents" className="mt-4">
          <DocumentList dossierId={dossier.id} usagerId={dossier.usagerId} />
        </TabsContent>

        <TabsContent value="historique" className="mt-4">
          <Card>
            <CardContent className="pt-6">
              <WorkflowTimeline
                entries={historiqueQuery.data ?? []}
                isLoading={historiqueQuery.isLoading}
              />
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="commentaires" className="mt-4 space-y-4">
          <Card>
            <CardContent className="space-y-3 pt-6">
              <textarea
                className="min-h-[80px] w-full rounded-md border px-3 py-2 text-sm"
                placeholder={t('commentaires.placeholder')}
                value={commentaire}
                onChange={(e) => setCommentaire(e.target.value)}
              />
              <Button
                onClick={handleAddCommentaire}
                disabled={commentaireMutation.isPending || !commentaire.trim()}
              >
                {t('commentaires.add')}
              </Button>
            </CardContent>
          </Card>
          {(dossier.commentaires ?? []).map((c) => (
            <Card key={c.id}>
              <CardContent className="pt-4">
                <p className="text-sm">{c.contenu}</p>
                <p className="text-muted-foreground mt-2 text-xs">
                  {c.auteurNom ?? t('commentaires.anonymous')} ·{' '}
                  {new Date(c.createdAt).toLocaleString()}
                </p>
              </CardContent>
            </Card>
          ))}
        </TabsContent>
      </Tabs>
    </div>
  )
}

export default DossierDetailPage
