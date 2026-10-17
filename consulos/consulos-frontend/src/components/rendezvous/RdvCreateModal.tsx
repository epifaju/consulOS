import { useMemo, useState } from 'react'
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
import { useCreateRdv, useDisponibilites } from '@/hooks/useRendezvous'
import { useSearchUsagers } from '@/hooks/useUsagers'
import { useTypesService } from '@/hooks/useDossiers'
import { formatDateISO } from '@/utils/rdvUtils'
import type { UsagerSummary } from '@/types/usager'

interface RdvCreateModalProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  initialDate?: Date
  initialTime?: string
}

export function RdvCreateModal({ open, onOpenChange, initialDate, initialTime }: RdvCreateModalProps) {
  const { t } = useTranslation('rendezvous')
  const [step, setStep] = useState(1)
  const [selectedDate, setSelectedDate] = useState(
    initialDate ? formatDateISO(initialDate) : formatDateISO(new Date()),
  )
  const [selectedTime, setSelectedTime] = useState(initialTime ?? '')
  const [typeServiceId, setTypeServiceId] = useState('')
  const [usagerSearch, setUsagerSearch] = useState('')
  const [selectedUsager, setSelectedUsager] = useState<UsagerSummary | null>(null)
  const [notes, setNotes] = useState('')

  const typesQuery = useTypesService()
  const disponibilitesQuery = useDisponibilites(
    selectedDate && typeServiceId
      ? { date: selectedDate, typeServiceId }
      : selectedDate
        ? { date: selectedDate }
        : null,
  )
  const searchQuery = useSearchUsagers(usagerSearch)
  const createMutation = useCreateRdv()

  const creneaux = disponibilitesQuery.data ?? []

  const resetAndClose = () => {
    setStep(1)
    setSelectedUsager(null)
    setNotes('')
    onOpenChange(false)
  }

  const handleCreate = () => {
    if (!selectedUsager || !selectedTime) return
    const dateHeure = `${selectedDate}T${selectedTime}:00Z`
    createMutation.mutate(
      {
        usagerId: selectedUsager.id,
        dateHeure,
        typeServiceId: typeServiceId || undefined,
        notes: notes || undefined,
      },
      { onSuccess: () => resetAndClose() },
    )
  }

  const stepTitle = useMemo(() => {
    if (step === 1) return t('create.step1')
    if (step === 2) return t('create.step2')
    return t('create.step3')
  }, [step, t])

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[90vh] max-w-lg overflow-y-auto">
        <DialogHeader>
          <DialogTitle>{t('create.title')} — {stepTitle}</DialogTitle>
        </DialogHeader>

        {step === 1 && (
          <div className="space-y-4">
            <div>
              <label htmlFor="rdv-date" className="mb-1 block text-sm font-medium">
                {t('create.date')}
              </label>
              <Input
                id="rdv-date"
                type="date"
                value={selectedDate}
                onChange={(e) => {
                  setSelectedDate(e.target.value)
                  setSelectedTime('')
                }}
              />
            </div>
            <div>
              <label htmlFor="rdv-type-service" className="mb-1 block text-sm font-medium">
                {t('create.typeService')}
              </label>
              <Select value={typeServiceId || 'NONE'} onValueChange={(v) => setTypeServiceId(v === 'NONE' ? '' : v)}>
                <SelectTrigger id="rdv-type-service">
                  <SelectValue placeholder={t('create.selectType')} />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="NONE">{t('create.noType')}</SelectItem>
                  {(typesQuery.data ?? []).map((type) => (
                    <SelectItem key={type.id} value={type.id}>
                      {type.libelle}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div>
              <p className="mb-2 text-sm font-medium">{t('create.creneaux')}</p>
              {disponibilitesQuery.isLoading ? (
                <p className="text-muted-foreground text-sm">{t('loading')}</p>
              ) : creneaux.length === 0 ? (
                <p className="text-muted-foreground text-sm">{t('create.noSlots')}</p>
              ) : (
                <div className="grid grid-cols-3 gap-2">
                  {creneaux.map((c) => (
                    <button
                      key={c.heureDebut}
                      type="button"
                      onClick={() => setSelectedTime(c.heureDebut.slice(0, 5))}
                      className={`rounded-md border px-2 py-2 text-sm transition ${
                        selectedTime === c.heureDebut.slice(0, 5)
                          ? 'border-accent bg-light ring-2 ring-accent'
                          : 'hover:bg-light'
                      }`}
                    >
                      {c.heureDebut.slice(0, 5)}
                      <span className="text-muted-foreground block text-xs">
                        {c.placesRestantes}/{c.quotaMax}
                      </span>
                    </button>
                  ))}
                </div>
              )}
            </div>
            <div className="flex justify-end">
              <Button disabled={!selectedTime} onClick={() => setStep(2)}>
                {t('create.next')}
              </Button>
            </div>
          </div>
        )}

        {step === 2 && (
          <div className="space-y-4">
            <label htmlFor="rdv-usager-search" className="sr-only">
              {t('create.usagerSearch')}
            </label>
            <Input
              id="rdv-usager-search"
              placeholder={t('create.usagerSearch')}
              value={usagerSearch}
              onChange={(e) => setUsagerSearch(e.target.value)}
            />
            {selectedUsager && (
              <div className="rounded-md border border-accent bg-light p-3">
                <p className="font-medium">
                  {selectedUsager.prenom} {selectedUsager.nom}
                </p>
                <p className="text-muted-foreground text-sm">{selectedUsager.numero}</p>
              </div>
            )}
            {usagerSearch.trim().length >= 2 && (
              <ul className="max-h-48 space-y-2 overflow-y-auto">
                {(searchQuery.data ?? []).map((usager) => (
                  <li key={usager.id}>
                    <button
                      type="button"
                      onClick={() => setSelectedUsager(usager)}
                      className={`w-full rounded-md border p-2 text-left text-sm hover:bg-light ${
                        selectedUsager?.id === usager.id ? 'border-accent bg-light' : ''
                      }`}
                    >
                      {usager.prenom} {usager.nom} — {usager.numero}
                    </button>
                  </li>
                ))}
              </ul>
            )}
            <div className="flex justify-between">
              <Button variant="outline" onClick={() => setStep(1)}>
                {t('create.prev')}
              </Button>
              <Button disabled={!selectedUsager} onClick={() => setStep(3)}>
                {t('create.next')}
              </Button>
            </div>
          </div>
        )}

        {step === 3 && selectedUsager && (
          <div className="space-y-4">
            <div className="rounded-md bg-gray-50 p-3 text-sm">
              <p>
                <strong>{t('create.recapDate')}:</strong> {selectedDate} {selectedTime}
              </p>
              <p>
                <strong>{t('create.recapUsager')}:</strong> {selectedUsager.prenom} {selectedUsager.nom}
              </p>
            </div>
            <label htmlFor="rdv-notes" className="mb-1 block text-sm font-medium">
              {t('create.notes')}
            </label>
            <textarea
              id="rdv-notes"
              className="min-h-[60px] w-full rounded-md border px-3 py-2 text-sm"
              placeholder={t('create.notes')}
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
            />
            <div className="flex justify-between">
              <Button variant="outline" onClick={() => setStep(2)}>
                {t('create.prev')}
              </Button>
              <Button onClick={handleCreate} disabled={createMutation.isPending}>
                {createMutation.isPending ? t('create.creating') : t('create.submit')}
              </Button>
            </div>
          </div>
        )}
      </DialogContent>
    </Dialog>
  )
}

export default RdvCreateModal
