import { useEffect, useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { useNavigate, useParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { z } from 'zod'
import { Loader2 } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '@/components/ui/form'
import { Input } from '@/components/ui/input'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { DoublonAlert } from '@/components/usagers/DoublonAlert'
import {
  useCreateUsager,
  useDoublons,
  useUpdateUsager,
  useUsager,
} from '@/hooks/useUsagers'
const usagerSchema = z.object({
  nom: z.string().min(1),
  prenom: z.string().min(1),
  dateNaissance: z.string().optional(),
  lieuNaissance: z.string().optional(),
  nationalite: z.string().optional(),
  typeUsager: z.enum(['DIASPORA', 'MANDATAIRE', 'AUTRE']).default('DIASPORA'),
  telephone: z.string().optional(),
  email: z.string().email().optional().or(z.literal('')),
  adresse: z.string().optional(),
  numeroDocument: z.string().optional(),
  typeDocument: z.enum(['PASSEPORT', 'CNI', 'TITRE_SEJOUR', 'AUTRE']).optional(),
})

type UsagerFormValues = z.infer<typeof usagerSchema>

export function UsagerFormPage() {
  const { id } = useParams<{ id: string }>()
  const isEdit = Boolean(id && id !== 'new')
  const { t } = useTranslation('usagers')
  const navigate = useNavigate()
  const [ignoreDoublons, setIgnoreDoublons] = useState(false)

  const { data: usager, isLoading } = useUsager(isEdit ? (id ?? '') : '')
  const createMutation = useCreateUsager()
  const updateMutation = useUpdateUsager(id ?? '')
  const form = useForm<UsagerFormValues>({
    resolver: zodResolver(usagerSchema),
    defaultValues: {
      nom: '',
      prenom: '',
      typeUsager: 'DIASPORA',
      email: '',
    },
  })

  const watchedNom = form.watch('nom')
  const watchedDate = form.watch('dateNaissance')
  const watchedNat = form.watch('nationalite')

  const doublonsQuery = useDoublons(
    watchedNom && watchedDate && watchedNat
      ? {
          nom: watchedNom,
          dateNaissance: watchedDate,
          nationalite: watchedNat,
          excludeId: isEdit ? id : undefined,
        }
      : null,
    !ignoreDoublons && !isEdit,
  )

  useEffect(() => {
    if (usager && isEdit) {
      form.reset({
        nom: usager.nom,
        prenom: usager.prenom,
        dateNaissance: usager.dateNaissance ?? undefined,
        lieuNaissance: usager.lieuNaissance ?? undefined,
        nationalite: usager.nationalite ?? undefined,
        typeUsager: usager.typeUsager,
        telephone: usager.telephone ?? undefined,
        email: usager.email ?? '',
        adresse: usager.adresse ?? undefined,
        numeroDocument: usager.numeroDocument ?? undefined,
        typeDocument: usager.typeDocument ?? undefined,
      })
    }
  }, [usager, isEdit, form])

  const onSubmit = (values: UsagerFormValues) => {
    const payload = {
      ...values,
      email: values.email || undefined,
      typeDocument: values.typeDocument || undefined,
    }

    if (isEdit) {
      updateMutation.mutate(payload, {
        onSuccess: (response) => navigate(`/usagers/${response.data.data.id}`),
      })
      return
    }

    createMutation.mutate(payload, {
      onSuccess: (response) => {
        navigate(`/usagers/${response.data.data.usager.id}`)
      },
    })
  }

  const activeDoublons = doublonsQuery.data ?? []

  const isPending = createMutation.isPending || updateMutation.isPending

  if (isEdit && isLoading) {
    return <p className="text-muted-foreground">{t('loading')}</p>
  }

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <h1 className="text-2xl font-semibold text-primary">
        {isEdit ? t('form.editTitle') : t('form.createTitle')}
      </h1>

      {!ignoreDoublons && !isEdit && (
        <DoublonAlert
          doublons={activeDoublons}
          onIgnore={() => setIgnoreDoublons(true)}
        />
      )}

      <Form {...form}>
        <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-6">
          <Card>
            <CardHeader>
              <CardTitle>{t('form.sections.identity')}</CardTitle>
            </CardHeader>
            <CardContent className="grid gap-4 sm:grid-cols-2">
              <FormField
                control={form.control}
                name="nom"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>{t('fields.nom')}</FormLabel>
                    <FormControl>
                      <Input {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="prenom"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>{t('fields.prenom')}</FormLabel>
                    <FormControl>
                      <Input {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="dateNaissance"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>{t('fields.dateNaissance')}</FormLabel>
                    <FormControl>
                      <Input type="date" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="lieuNaissance"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>{t('fields.lieuNaissance')}</FormLabel>
                    <FormControl>
                      <Input {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="nationalite"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>{t('fields.nationalite')}</FormLabel>
                    <FormControl>
                      <Input maxLength={3} {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="typeUsager"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>{t('fields.typeUsager')}</FormLabel>
                    <Select onValueChange={field.onChange} value={field.value}>
                      <FormControl>
                        <SelectTrigger>
                          <SelectValue />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent>
                        <SelectItem value="DIASPORA">DIASPORA</SelectItem>
                        <SelectItem value="MANDATAIRE">MANDATAIRE</SelectItem>
                        <SelectItem value="AUTRE">AUTRE</SelectItem>
                      </SelectContent>
                    </Select>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>{t('form.sections.contact')}</CardTitle>
            </CardHeader>
            <CardContent className="grid gap-4 sm:grid-cols-2">
              <FormField
                control={form.control}
                name="telephone"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>{t('fields.telephone')}</FormLabel>
                    <FormControl>
                      <Input {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="email"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>{t('fields.email')}</FormLabel>
                    <FormControl>
                      <Input type="email" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="adresse"
                render={({ field }) => (
                  <FormItem className="sm:col-span-2">
                    <FormLabel>{t('fields.adresse')}</FormLabel>
                    <FormControl>
                      <Input {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>{t('form.sections.document')}</CardTitle>
            </CardHeader>
            <CardContent className="grid gap-4 sm:grid-cols-2">
              <FormField
                control={form.control}
                name="numeroDocument"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>{t('fields.numeroDocument')}</FormLabel>
                    <FormControl>
                      <Input {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="typeDocument"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>{t('fields.typeDocument')}</FormLabel>
                    <Select onValueChange={field.onChange} value={field.value}>
                      <FormControl>
                        <SelectTrigger>
                          <SelectValue placeholder="—" />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent>
                        <SelectItem value="PASSEPORT">PASSEPORT</SelectItem>
                        <SelectItem value="CNI">CNI</SelectItem>
                        <SelectItem value="TITRE_SEJOUR">TITRE_SEJOUR</SelectItem>
                        <SelectItem value="AUTRE">AUTRE</SelectItem>
                      </SelectContent>
                    </Select>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </CardContent>
          </Card>

          <div className="flex justify-end gap-3">
            <Button type="button" variant="outline" onClick={() => navigate('/usagers')}>
              {t('form.cancel')}
            </Button>
            <Button type="submit" disabled={isPending} className="bg-primary">
              {isPending ? (
                <>
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                  {t('form.saving')}
                </>
              ) : (
                t('form.save')
              )}
            </Button>
          </div>
        </form>
      </Form>
    </div>
  )
}

export default UsagerFormPage
