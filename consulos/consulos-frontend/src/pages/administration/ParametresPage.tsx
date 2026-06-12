import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useTranslation } from 'react-i18next'
import { useQuery } from '@tanstack/react-query'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { useOrganisation, useUpdateOrganisation } from '@/hooks/useAdmin'
import { useTarifs } from '@/hooks/useEncaissements'
import { parametresService } from '@/services/parametresService'

const orgSchema = z.object({
  nom: z.string().min(1),
  paysCode: z.string().length(3),
  langueDefaut: z.string().min(2),
  deviseDefaut: z.string().length(3),
})

type OrgForm = z.infer<typeof orgSchema>

export function ParametresPage() {
  const { t } = useTranslation('admin')
  const orgQuery = useOrganisation()
  const updateOrg = useUpdateOrganisation()
  const tarifsQuery = useTarifs()
  const creneauxQuery = useQuery({
    queryKey: ['parametres-creneaux'],
    queryFn: async () => (await parametresService.listCreneaux()).data.data,
  })
  const fermeturesQuery = useQuery({
    queryKey: ['parametres-fermetures'],
    queryFn: async () => (await parametresService.listJoursFermeture()).data.data,
  })

  const form = useForm<OrgForm>({
    resolver: zodResolver(orgSchema),
    defaultValues: { nom: '', paysCode: '', langueDefaut: 'fr', deviseDefaut: 'EUR' },
  })

  useEffect(() => {
    if (orgQuery.data) {
      form.reset({
        nom: orgQuery.data.nom,
        paysCode: orgQuery.data.paysCode,
        langueDefaut: orgQuery.data.langueDefaut,
        deviseDefaut: orgQuery.data.deviseDefaut,
      })
    }
  }, [orgQuery.data, form])

  const onSubmit = form.handleSubmit((values) => {
    if (!orgQuery.data) return
    updateOrg.mutate({ ...orgQuery.data, ...values })
  })

  return (
    <div className="space-y-4">
      <h2 className="text-lg font-semibold">{t('parametres.title')}</h2>
      <Tabs defaultValue="general">
        <TabsList>
          <TabsTrigger value="general">{t('parametres.tabs.general')}</TabsTrigger>
          <TabsTrigger value="horaires">{t('parametres.tabs.horaires')}</TabsTrigger>
          <TabsTrigger value="tarifs">{t('parametres.tabs.tarifs')}</TabsTrigger>
        </TabsList>

        <TabsContent value="general">
          <Card>
            <CardHeader>
              <CardTitle className="text-base">{t('parametres.general')}</CardTitle>
            </CardHeader>
            <CardContent>
              <form onSubmit={onSubmit} className="grid max-w-lg gap-3">
                <Input {...form.register('nom')} placeholder={t('parametres.fields.nom')} />
                <Input {...form.register('paysCode')} placeholder={t('parametres.fields.pays')} />
                <Input {...form.register('langueDefaut')} placeholder={t('parametres.fields.langue')} />
                <Input {...form.register('deviseDefaut')} placeholder={t('parametres.fields.devise')} />
                <Button type="submit" className="w-fit bg-primary" disabled={updateOrg.isPending}>
                  {t('actions.save')}
                </Button>
              </form>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="horaires">
          <div className="grid gap-4 md:grid-cols-2">
            <Card>
              <CardHeader>
                <CardTitle className="text-base">{t('parametres.creneaux')}</CardTitle>
              </CardHeader>
              <CardContent className="space-y-2 text-sm">
                {(creneauxQuery.data ?? []).map((c) => (
                  <p key={c.id}>
                    J{c.jourSemaine} : {c.heureDebut}–{c.heureFin} ({c.dureeMinutes} min)
                  </p>
                ))}
              </CardContent>
            </Card>
            <Card>
              <CardHeader>
                <CardTitle className="text-base">{t('parametres.fermetures')}</CardTitle>
              </CardHeader>
              <CardContent className="space-y-2 text-sm">
                {(fermeturesQuery.data ?? []).map((j) => (
                  <p key={j.id}>{j.dateFermeture} — {j.motif ?? '—'}</p>
                ))}
              </CardContent>
            </Card>
          </div>
        </TabsContent>

        <TabsContent value="tarifs">
          <Card>
            <CardHeader>
              <CardTitle className="text-base">{t('parametres.tarifs')}</CardTitle>
            </CardHeader>
            <CardContent className="space-y-2 text-sm">
              {(tarifsQuery.data ?? []).map((tarif) => (
                <p key={tarif.id}>
                  {tarif.libelle} — {tarif.montant} {tarif.devise}
                </p>
              ))}
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>
    </div>
  )
}

export default ParametresPage
