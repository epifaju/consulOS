import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { useEnregistrerPaiement } from '@/hooks/useEncaissements'
import { formatMontant, MODES_PAIEMENT, resteDu } from '@/utils/encaissementStatus'
import type { EncaissementSummary, ModePaiement } from '@/types/encaissement'

interface PaiementModalProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  encaissement: EncaissementSummary | null
  onSuccess?: () => void
}

export function PaiementModal({ open, onOpenChange, encaissement, onSuccess }: PaiementModalProps) {
  const { t } = useTranslation('encaissements')
  const [montant, setMontant] = useState('')
  const [modePaiement, setModePaiement] = useState<ModePaiement>('ESPECES')
  const [notes, setNotes] = useState('')

  const paiementMutation = useEnregistrerPaiement(encaissement?.id ?? '')

  const reste = encaissement
    ? resteDu(encaissement.montantTotal, encaissement.montantPercu)
    : 0

  useEffect(() => {
    if (open && encaissement) {
      setMontant(reste > 0 ? String(reste) : '')
      setModePaiement('ESPECES')
      setNotes('')
    }
  }, [open, encaissement, reste])

  const handleSubmit = () => {
    if (!encaissement) return
    const value = parseFloat(montant)
    if (Number.isNaN(value) || value <= 0 || value > reste) return

    paiementMutation.mutate(
      { montant: value, modePaiement, notes: notes || undefined },
      {
        onSuccess: () => {
          onOpenChange(false)
          onSuccess?.()
        },
      },
    )
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle>{t('paiement.title')}</DialogTitle>
        </DialogHeader>

        {encaissement && (
          <div className="space-y-4">
            <div className="rounded-md bg-light p-3 text-sm">
              <p>
                <span className="font-medium">{t('columns.montant')} :</span>{' '}
                {formatMontant(encaissement.montantTotal, encaissement.devise)}
              </p>
              <p>
                <span className="font-medium">{t('columns.percu')} :</span>{' '}
                {formatMontant(encaissement.montantPercu, encaissement.devise)}
              </p>
              <p>
                <span className="font-medium">{t('paiement.reste')} :</span>{' '}
                {formatMontant(reste, encaissement.devise)}
              </p>
            </div>

            <div>
              <label className="mb-1 block text-sm font-medium">{t('paiement.montant')}</label>
              <Input
                type="number"
                min={0.01}
                max={reste}
                step={0.01}
                value={montant}
                onChange={(e) => setMontant(e.target.value)}
              />
            </div>

            <div>
              <label className="mb-1 block text-sm font-medium">{t('columns.mode')}</label>
              <Select value={modePaiement} onValueChange={(v) => setModePaiement(v as ModePaiement)}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {MODES_PAIEMENT.filter((m) => m !== 'EXONERE').map((mode) => (
                    <SelectItem key={mode} value={mode}>
                      {t(`modes.${mode}`)}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div>
              <label className="mb-1 block text-sm font-medium">{t('form.notes')}</label>
              <Input value={notes} onChange={(e) => setNotes(e.target.value)} />
            </div>

            <div className="flex justify-end gap-2">
              <Button variant="outline" onClick={() => onOpenChange(false)}>
                {t('actions.cancel')}
              </Button>
              <Button
                onClick={handleSubmit}
                disabled={paiementMutation.isPending || reste <= 0}
                className="bg-primary"
              >
                {t('paiement.confirm')}
              </Button>
            </div>
          </div>
        )}
      </DialogContent>
    </Dialog>
  )
}

export default PaiementModal
