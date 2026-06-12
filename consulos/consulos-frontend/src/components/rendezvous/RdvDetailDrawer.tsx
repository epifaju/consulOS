import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
} from '@/components/ui/sheet'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Input } from '@/components/ui/input'
import { useRdvDetail, useUpdateRdvStatut } from '@/hooks/useRendezvous'
import { getRdvStatutBadgeClass } from '@/utils/rdvUtils'
import type { RdvStatut } from '@/types/rendezvous'

interface RdvDetailDrawerProps {
  rdvId: string | null
  open: boolean
  onOpenChange: (open: boolean) => void
}

interface StatutAction {
  label: string
  statut: RdvStatut
  variant?: 'default' | 'destructive' | 'outline'
  requiresMotif?: boolean
}

export function RdvDetailDrawer({ rdvId, open, onOpenChange }: RdvDetailDrawerProps) {
  const { t } = useTranslation('rendezvous')
  const { data: rdv, isLoading } = useRdvDetail(rdvId)
  const updateStatut = useUpdateRdvStatut(rdvId ?? '')
  const [motifAction, setMotifAction] = useState<StatutAction | null>(null)
  const [motif, setMotif] = useState('')

  const getActions = (): StatutAction[] => {
    if (!rdv) return []
    switch (rdv.statut) {
      case 'CONFIRME':
        return [
          { label: t('actions.present'), statut: 'PRESENT' },
          { label: t('actions.absent'), statut: 'ABSENT', variant: 'outline' },
          { label: t('actions.reporter'), statut: 'REPORTE', variant: 'outline', requiresMotif: true },
          { label: t('actions.annuler'), statut: 'ANNULE', variant: 'destructive', requiresMotif: true },
        ]
      case 'EN_ATTENTE':
        return [
          { label: t('actions.confirmer'), statut: 'CONFIRME' },
          { label: t('actions.annuler'), statut: 'ANNULE', variant: 'destructive', requiresMotif: true },
        ]
      default:
        return []
    }
  }

  const handleAction = (action: StatutAction) => {
    if (action.requiresMotif) {
      setMotifAction(action)
      return
    }
    updateStatut.mutate({ statut: action.statut }, { onSuccess: () => onOpenChange(false) })
  }

  const confirmMotif = () => {
    if (!motifAction) return
    updateStatut.mutate(
      { statut: motifAction.statut, motif: motif.trim() || undefined },
      {
        onSuccess: () => {
          setMotifAction(null)
          setMotif('')
          onOpenChange(false)
        },
      },
    )
  }

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent className="w-full sm:max-w-md">
        <SheetHeader>
          <SheetTitle>{t('detail.title')}</SheetTitle>
        </SheetHeader>

        {isLoading && <p className="text-muted-foreground mt-4 text-sm">{t('loading')}</p>}

        {rdv && (
          <div className="mt-6 space-y-4">
            <Badge className={getRdvStatutBadgeClass(rdv.statut)}>
              {t(`statuts.${rdv.statut}`)}
            </Badge>

            <div className="space-y-2 text-sm">
              <p>
                <span className="text-muted-foreground">{t('detail.date')}:</span>{' '}
                {new Date(rdv.dateHeure).toLocaleString()}
              </p>
              <p>
                <span className="text-muted-foreground">{t('detail.usager')}:</span>{' '}
                <Link to={`/usagers/${rdv.usagerId}`} className="text-accent hover:underline">
                  {rdv.usagerPrenom} {rdv.usagerNom}
                </Link>
              </p>
              {rdv.typeServiceLibelle && (
                <p>
                  <span className="text-muted-foreground">{t('detail.service')}:</span>{' '}
                  {rdv.typeServiceLibelle}
                </p>
              )}
              {rdv.agentNom && (
                <p>
                  <span className="text-muted-foreground">{t('detail.agent')}:</span> {rdv.agentNom}
                </p>
              )}
              {rdv.dossierReference && (
                <p>
                  <span className="text-muted-foreground">{t('detail.dossier')}:</span>{' '}
                  <Link to={`/dossiers/${rdv.dossierId}`} className="text-accent hover:underline">
                    {rdv.dossierReference}
                  </Link>
                </p>
              )}
              {rdv.notes && (
                <p>
                  <span className="text-muted-foreground">{t('detail.notes')}:</span> {rdv.notes}
                </p>
              )}
            </div>

            {motifAction && (
              <div className="space-y-2 rounded-md border p-3">
                <Input
                  placeholder={t('actions.motifPlaceholder')}
                  value={motif}
                  onChange={(e) => setMotif(e.target.value)}
                />
                <div className="flex gap-2">
                  <Button size="sm" onClick={confirmMotif}>
                    {t('actions.confirm')}
                  </Button>
                  <Button size="sm" variant="outline" onClick={() => setMotifAction(null)}>
                    {t('actions.cancel')}
                  </Button>
                </div>
              </div>
            )}

            <div className="flex flex-wrap gap-2 pt-2">
              {getActions().map((action) => (
                <Button
                  key={action.statut}
                  size="sm"
                  variant={action.variant ?? 'default'}
                  disabled={updateStatut.isPending}
                  onClick={() => handleAction(action)}
                >
                  {action.label}
                </Button>
              ))}
            </div>
          </div>
        )}
      </SheetContent>
    </Sheet>
  )
}

export default RdvDetailDrawer
