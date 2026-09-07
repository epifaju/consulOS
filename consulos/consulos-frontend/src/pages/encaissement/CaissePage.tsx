import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { CreditCard, FileText, Plus } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { JournalCaissePanel } from '@/components/encaissement/JournalCaissePanel'
import { PaiementModal } from '@/components/encaissement/PaiementModal'
import { useCreateEncaissement, useEncaissements, useTarifs } from '@/hooks/useEncaissements'
import { useDossiers } from '@/hooks/useDossiers'
import { useSearchUsagers } from '@/hooks/useUsagers'
import {
  formatMontant,
  getStatutBadgeClass,
  MODES_PAIEMENT,
  STATUTS_ENCAISSEMENT,
} from '@/utils/encaissementStatus'
import type { EncaissementSummary, ModePaiement, StatutEncaissement } from '@/types/encaissement'
import type { DossierSummary } from '@/types/dossier'
import type { UsagerSummary } from '@/types/usager'

function todayISO(): string {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

export function CaissePage() {
  const { t } = useTranslation('encaissements')
  const [selectedDate, setSelectedDate] = useState(todayISO)
  const [statutFilter, setStatutFilter] = useState<StatutEncaissement | 'ALL'>('ALL')
  const [modeFilter, setModeFilter] = useState<ModePaiement | 'ALL'>('ALL')
  const [page, setPage] = useState(0)

  const [usagerSearch, setUsagerSearch] = useState('')
  const [selectedUsager, setSelectedUsager] = useState<UsagerSummary | null>(null)
  const [selectedDossier, setSelectedDossier] = useState<DossierSummary | null>(null)
  const [montantTotal, setMontantTotal] = useState('')
  const [modePaiement, setModePaiement] = useState<ModePaiement>('ESPECES')
  const [notes, setNotes] = useState('')

  const [paiementOpen, setPaiementOpen] = useState(false)
  const [selectedEncaissement, setSelectedEncaissement] = useState<EncaissementSummary | null>(null)

  const listQuery = useEncaissements({
    page,
    size: 20,
    sort: 'createdAt,desc',
    statut: statutFilter !== 'ALL' ? statutFilter : undefined,
    modePaiement: modeFilter !== 'ALL' ? modeFilter : undefined,
    dateDebut: `${selectedDate}T00:00:00Z`,
    dateFin: `${selectedDate}T23:59:59Z`,
  })

  const searchQuery = useSearchUsagers(usagerSearch)
  const dossiersQuery = useDossiers(
    {
      page: 0,
      size: 50,
      usagerId: selectedUsager?.id,
    },
    Boolean(selectedUsager?.id),
  )
  const tarifsQuery = useTarifs()
  const createMutation = useCreateEncaissement()

  const encaissements = listQuery.data?.content ?? []
  const totalPages = listQuery.data?.totalPages ?? 1

  useEffect(() => {
    setSelectedDossier(null)
    setMontantTotal('')
  }, [selectedUsager?.id])

  useEffect(() => {
    if (!selectedDossier || !tarifsQuery.data) return
    const libelle = selectedDossier.typeServiceLibelle?.toLowerCase() ?? ''
    const match = tarifsQuery.data.find((tf) => tf.libelle.toLowerCase().includes(libelle))
    if (match) {
      setMontantTotal(String(match.montant))
    }
  }, [selectedDossier, tarifsQuery.data])

  const dossierOptions = useMemo(
    () => dossiersQuery.data?.content ?? [],
    [dossiersQuery.data],
  )

  const resetForm = () => {
    setSelectedUsager(null)
    setSelectedDossier(null)
    setUsagerSearch('')
    setMontantTotal('')
    setModePaiement('ESPECES')
    setNotes('')
  }

  const handleCreate = () => {
    if (!selectedUsager || !montantTotal) return
    const montant = parseFloat(montantTotal)
    if (Number.isNaN(montant) || montant <= 0) return

    createMutation.mutate(
      {
        usagerId: selectedUsager.id,
        dossierId: selectedDossier?.id,
        montantTotal: montant,
        modePaiement,
        notes: notes || undefined,
      },
      { onSuccess: () => resetForm() },
    )
  }

  const openPaiement = (enc: EncaissementSummary) => {
    setSelectedEncaissement(enc)
    setPaiementOpen(true)
  }

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-semibold text-primary">{t('title')}</h1>

      <div className="grid gap-4 lg:grid-cols-5">
        <div className="space-y-4 lg:col-span-3">
          <div className="flex flex-wrap gap-2">
            <Input
              type="date"
              value={selectedDate}
              onChange={(e) => {
                setSelectedDate(e.target.value)
                setPage(0)
              }}
              className="w-auto"
            />
            <Select
              value={statutFilter}
              onValueChange={(v) => {
                setStatutFilter(v as StatutEncaissement | 'ALL')
                setPage(0)
              }}
            >
              <SelectTrigger className="w-[160px]">
                <SelectValue placeholder={t('filters.statut')} />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">{t('filters.allStatuts')}</SelectItem>
                {STATUTS_ENCAISSEMENT.map((s) => (
                  <SelectItem key={s} value={s}>
                    {t(`statuts.${s}`)}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            <Select
              value={modeFilter}
              onValueChange={(v) => {
                setModeFilter(v as ModePaiement | 'ALL')
                setPage(0)
              }}
            >
              <SelectTrigger className="w-[160px]">
                <SelectValue placeholder={t('filters.mode')} />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">{t('filters.allModes')}</SelectItem>
                {MODES_PAIEMENT.map((m) => (
                  <SelectItem key={m} value={m}>
                    {t(`modes.${m}`)}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="overflow-x-auto rounded-md border bg-white">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>{t('columns.recu')}</TableHead>
                  <TableHead>{t('columns.usager')}</TableHead>
                  <TableHead className="hidden md:table-cell">{t('columns.service')}</TableHead>
                  <TableHead className="text-right">{t('columns.montant')}</TableHead>
                  <TableHead className="text-right hidden sm:table-cell">{t('columns.percu')}</TableHead>
                  <TableHead className="hidden lg:table-cell">{t('columns.mode')}</TableHead>
                  <TableHead>{t('columns.statut')}</TableHead>
                  <TableHead>{t('columns.actions')}</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {listQuery.isLoading && (
                  <TableRow>
                    <TableCell colSpan={8} className="text-muted-foreground text-center">
                      {t('loading')}
                    </TableCell>
                  </TableRow>
                )}
                {!listQuery.isLoading && encaissements.length === 0 && (
                  <TableRow>
                    <TableCell colSpan={8} className="text-muted-foreground text-center">
                      {t('empty')}
                    </TableCell>
                  </TableRow>
                )}
                {encaissements.map((enc) => (
                  <TableRow key={enc.id}>
                    <TableCell className="font-mono text-xs">{enc.numeroRecu}</TableCell>
                    <TableCell>
                      {enc.usagerPrenom} {enc.usagerNom}
                    </TableCell>
                    <TableCell className="hidden md:table-cell">
                      {enc.typeServiceLibelle ?? '—'}
                    </TableCell>
                    <TableCell className="text-right">
                      {formatMontant(enc.montantTotal, enc.devise)}
                    </TableCell>
                    <TableCell className="text-right hidden sm:table-cell">
                      {formatMontant(enc.montantPercu, enc.devise)}
                    </TableCell>
                    <TableCell className="hidden lg:table-cell">
                      {enc.modePaiement ? t(`modes.${enc.modePaiement}`) : '—'}
                    </TableCell>
                    <TableCell>
                      <Badge variant="outline" className={getStatutBadgeClass(enc.statut)}>
                        {t(`statuts.${enc.statut}`)}
                      </Badge>
                    </TableCell>
                    <TableCell>
                      <div className="flex gap-1">
                        {(enc.statut === 'DU' || enc.statut === 'PARTIEL') && (
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => openPaiement(enc)}
                            title={t('actions.payer')}
                          >
                            <CreditCard className="h-4 w-4" />
                          </Button>
                        )}
                        <Button variant="ghost" size="sm" asChild title={t('actions.recu')}>
                          <Link to={`/caisse/recu/${enc.id}`}>
                            <FileText className="h-4 w-4" />
                          </Link>
                        </Button>
                      </div>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>

          {totalPages > 1 && (
            <div className="flex justify-center gap-2">
              <Button
                variant="outline"
                size="sm"
                disabled={page === 0}
                onClick={() => setPage((p) => p - 1)}
              >
                {t('pagination.prev')}
              </Button>
              <span className="flex items-center text-sm">
                {page + 1} / {totalPages}
              </span>
              <Button
                variant="outline"
                size="sm"
                disabled={page >= totalPages - 1}
                onClick={() => setPage((p) => p + 1)}
              >
                {t('pagination.next')}
              </Button>
            </div>
          )}
        </div>

        <div className="space-y-4 lg:col-span-2">
          <Card>
            <CardHeader>
              <CardTitle className="text-base">{t('form.title')}</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div>
                <label className="mb-1 block text-sm font-medium">{t('form.usager')}</label>
                <Input
                  placeholder={t('form.usagerSearch')}
                  value={usagerSearch}
                  onChange={(e) => setUsagerSearch(e.target.value)}
                />
                {selectedUsager && (
                  <p className="mt-1 text-sm text-accent">
                    {selectedUsager.prenom} {selectedUsager.nom} ({selectedUsager.numero})
                  </p>
                )}
                {searchQuery.data && searchQuery.data.length > 0 && !selectedUsager && (
                  <ul className="mt-1 max-h-32 overflow-y-auto rounded border">
                    {searchQuery.data.map((u) => (
                      <li key={u.id}>
                        <button
                          type="button"
                          className="w-full px-3 py-2 text-left text-sm hover:bg-light"
                          onClick={() => {
                            setSelectedUsager(u)
                            setUsagerSearch('')
                          }}
                        >
                          {u.prenom} {u.nom} — {u.numero}
                        </button>
                      </li>
                    ))}
                  </ul>
                )}
              </div>

              <div>
                <label className="mb-1 block text-sm font-medium">{t('form.dossier')}</label>
                <Select
                  value={selectedDossier?.id ?? 'NONE'}
                  onValueChange={(v) => {
                    if (v === 'NONE') {
                      setSelectedDossier(null)
                      return
                    }
                    const dossier = dossierOptions.find((d) => d.id === v) ?? null
                    setSelectedDossier(dossier)
                  }}
                  disabled={!selectedUsager}
                >
                  <SelectTrigger>
                    <SelectValue placeholder={t('form.dossierPlaceholder')} />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="NONE">{t('form.noDossier')}</SelectItem>
                    {dossierOptions.map((d) => (
                      <SelectItem key={d.id} value={d.id}>
                        {d.reference} — {d.typeServiceLibelle}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              <div>
                <label className="mb-1 block text-sm font-medium">{t('form.montant')}</label>
                <Input
                  type="number"
                  min={0.01}
                  step={0.01}
                  value={montantTotal}
                  onChange={(e) => setMontantTotal(e.target.value)}
                />
              </div>

              <div>
                <label className="mb-2 block text-sm font-medium">{t('form.mode')}</label>
                <div className="flex flex-wrap gap-3">
                  {MODES_PAIEMENT.map((mode) => (
                    <label key={mode} className="flex items-center gap-1 text-sm">
                      <input
                        type="radio"
                        name="modePaiement"
                        value={mode}
                        checked={modePaiement === mode}
                        onChange={() => setModePaiement(mode)}
                      />
                      {t(`modes.${mode}`)}
                    </label>
                  ))}
                </div>
              </div>

              <div>
                <label className="mb-1 block text-sm font-medium">{t('form.notes')}</label>
                <Input value={notes} onChange={(e) => setNotes(e.target.value)} />
              </div>

              <Button
                className="w-full bg-primary"
                onClick={handleCreate}
                disabled={!selectedUsager || !montantTotal || createMutation.isPending}
              >
                <Plus className="mr-2 h-4 w-4" />
                {t('form.submit')}
              </Button>
            </CardContent>
          </Card>
        </div>
      </div>

      <JournalCaissePanel date={selectedDate} />

      <PaiementModal
        open={paiementOpen}
        onOpenChange={setPaiementOpen}
        encaissement={selectedEncaissement}
      />
    </div>
  )
}

export default CaissePage
