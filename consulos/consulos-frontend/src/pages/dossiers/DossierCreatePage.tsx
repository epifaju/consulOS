import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import {
  ArrowLeft,
  ArrowRight,
  BookOpen,
  CreditCard,
  FileText,
  Stamp,
  User,
} from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { useCreateDossier, useTypesService } from '@/hooks/useDossiers'
import { useSearchUsagers } from '@/hooks/useUsagers'
import { ChecklistViewer } from '@/components/dossiers/ChecklistViewer'
import type { TypeService } from '@/types/dossier'
import type { UsagerSummary } from '@/types/usager'

const SERVICE_ICONS: Record<string, typeof FileText> = {
  PASSEPORT: BookOpen,
  ETAT_CIVIL: FileText,
  CARTE_CONSULAIRE: CreditCard,
  LEGALISATION: Stamp,
  PROCURATION: FileText,
}

function ServiceIcon({ code }: { code: string }) {
  const Icon = SERVICE_ICONS[code] ?? FileText
  return <Icon className="h-8 w-8 text-accent" />
}

export function DossierCreatePage() {
  const { t } = useTranslation('dossiers')
  const navigate = useNavigate()
  const [step, setStep] = useState(1)
  const [usagerSearch, setUsagerSearch] = useState('')
  const [selectedUsager, setSelectedUsager] = useState<UsagerSummary | null>(null)
  const [selectedType, setSelectedType] = useState<TypeService | null>(null)
  const [notes, setNotes] = useState('')

  const searchQuery = useSearchUsagers(usagerSearch)
  const typesQuery = useTypesService()
  const createMutation = useCreateDossier()

  const canGoStep2 = selectedUsager !== null
  const canGoStep3 = selectedType !== null

  const handleCreate = () => {
    if (!selectedUsager || !selectedType) return
    createMutation.mutate(
      {
        usagerId: selectedUsager.id,
        typeServiceId: selectedType.id,
        notesInternes: notes || undefined,
      },
      {
        onSuccess: (response) => {
          const id = response.data.data.id
          navigate(`/dossiers/${id}`)
        },
      },
    )
  }

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <Button variant="ghost" asChild className="px-0">
        <Link to="/dossiers">
          <ArrowLeft className="mr-2 h-4 w-4" />
          {t('backToList')}
        </Link>
      </Button>

      <h1 className="text-2xl font-semibold text-primary">{t('create.title')}</h1>

      <div className="flex items-center gap-2 text-sm">
        {[1, 2, 3].map((s) => (
          <div key={s} className="flex items-center gap-2">
            <span
              className={`flex h-8 w-8 items-center justify-center rounded-full text-xs font-bold ${
                step >= s ? 'bg-primary text-white' : 'bg-gray-200 text-gray-600'
              }`}
            >
              {s}
            </span>
            <span className={step === s ? 'font-medium' : 'text-muted-foreground'}>
              {t(`create.step${s}`)}
            </span>
            {s < 3 && <div className="mx-2 h-px w-8 bg-gray-200" />}
          </div>
        ))}
      </div>

      {step === 1 && (
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <User className="h-5 w-5" />
              {t('create.selectUsager')}
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <Input
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
              <ul className="max-h-60 space-y-2 overflow-y-auto">
                {(searchQuery.data ?? []).map((usager) => (
                  <li key={usager.id}>
                    <button
                      type="button"
                      onClick={() => setSelectedUsager(usager)}
                      className={`w-full rounded-md border p-3 text-left transition hover:bg-light ${
                        selectedUsager?.id === usager.id ? 'border-accent bg-light' : ''
                      }`}
                    >
                      <p className="font-medium">
                        {usager.prenom} {usager.nom}
                      </p>
                      <p className="text-muted-foreground text-sm">{usager.numero}</p>
                    </button>
                  </li>
                ))}
              </ul>
            )}
            <div className="flex justify-end">
              <Button disabled={!canGoStep2} onClick={() => setStep(2)}>
                {t('create.next')}
                <ArrowRight className="ml-2 h-4 w-4" />
              </Button>
            </div>
          </CardContent>
        </Card>
      )}

      {step === 2 && (
        <Card>
          <CardHeader>
            <CardTitle>{t('create.selectService')}</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="grid gap-3 sm:grid-cols-2">
              {(typesQuery.data ?? []).map((type) => (
                <button
                  key={type.id}
                  type="button"
                  onClick={() => setSelectedType(type)}
                  className={`rounded-lg border p-4 text-left transition hover:border-accent ${
                    selectedType?.id === type.id ? 'border-accent bg-light ring-2 ring-accent' : ''
                  }`}
                >
                  <div className="mb-3 flex items-start justify-between">
                    <ServiceIcon code={type.code} />
                    {type.tarifDefaut != null && (
                      <Badge variant="outline">{type.tarifDefaut} €</Badge>
                    )}
                  </div>
                  <p className="font-semibold">{type.libelle}</p>
                  <p className="text-muted-foreground mt-1 text-sm">
                    {t('create.delai', { days: type.delaiJours })}
                  </p>
                </button>
              ))}
            </div>
            <div className="flex justify-between">
              <Button variant="outline" onClick={() => setStep(1)}>
                {t('create.prev')}
              </Button>
              <Button disabled={!canGoStep3} onClick={() => setStep(3)}>
                {t('create.next')}
                <ArrowRight className="ml-2 h-4 w-4" />
              </Button>
            </div>
          </CardContent>
        </Card>
      )}

      {step === 3 && selectedUsager && selectedType && (
        <Card>
          <CardHeader>
            <CardTitle>{t('create.recap')}</CardTitle>
          </CardHeader>
          <CardContent className="space-y-6">
            <div className="grid gap-4 sm:grid-cols-2">
              <div>
                <p className="text-muted-foreground text-xs">{t('columns.usager')}</p>
                <p className="font-medium">
                  {selectedUsager.prenom} {selectedUsager.nom}
                </p>
              </div>
              <div>
                <p className="text-muted-foreground text-xs">{t('columns.typeService')}</p>
                <p className="font-medium">{selectedType.libelle}</p>
              </div>
            </div>

            <div>
              <p className="mb-2 font-medium">{t('checklist.title')}</p>
              <ChecklistViewer items={selectedType.checklist} />
            </div>

            <div>
              <label htmlFor="notes" className="mb-2 block text-sm font-medium">
                {t('create.notes')}
              </label>
              <textarea
                id="notes"
                className="min-h-[80px] w-full rounded-md border px-3 py-2 text-sm"
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
              />
            </div>

            <div className="flex justify-between">
              <Button variant="outline" onClick={() => setStep(2)}>
                {t('create.prev')}
              </Button>
              <Button
                onClick={handleCreate}
                disabled={createMutation.isPending}
                className="bg-primary"
              >
                {createMutation.isPending ? t('create.creating') : t('create.submit')}
              </Button>
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  )
}

export default DossierCreatePage
