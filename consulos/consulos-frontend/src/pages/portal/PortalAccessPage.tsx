import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { z } from 'zod'
import { Loader2, MailCheck } from 'lucide-react'
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
import { useRequestPortalAccess } from '@/hooks/usePortal'

const accessSchema = z.object({
  reference: z.string().min(5).max(30),
  email: z.string().email(),
})

type AccessFormValues = z.infer<typeof accessSchema>

export function PortalAccessPage() {
  const { t } = useTranslation('portal')
  const [sent, setSent] = useState(false)
  const [apiError, setApiError] = useState<string | null>(null)

  const form = useForm<AccessFormValues>({
    resolver: zodResolver(accessSchema),
    defaultValues: { reference: '', email: '' },
  })

  const mutation = useRequestPortalAccess()

  const onSubmit = (values: AccessFormValues) => {
    setApiError(null)
    mutation.mutate(values, {
      onSuccess: () => setSent(true),
      onError: () => setApiError(t('access.errors.generic')),
    })
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-primary to-accent p-4">
      <Card className="w-full max-w-md shadow-xl">
        <CardHeader className="space-y-2 text-center">
          <CardTitle className="text-2xl text-primary">{t('access.title')}</CardTitle>
          <p className="text-muted-foreground text-sm">{t('access.subtitle')}</p>
        </CardHeader>
        <CardContent>
          {sent ? (
            <div className="space-y-4 text-center">
              <MailCheck className="mx-auto h-10 w-10 text-success" />
              <h2 className="text-lg font-semibold">{t('access.sentTitle')}</h2>
              <p className="text-muted-foreground text-sm">{t('access.sentBody')}</p>
              <Link to="/login" className="text-accent text-sm hover:underline">
                {t('access.staffLogin')}
              </Link>
            </div>
          ) : (
            <Form {...form}>
              <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
                <FormField
                  control={form.control}
                  name="reference"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>{t('access.reference')}</FormLabel>
                      <FormControl>
                        <Input placeholder="DOS-2026-00001" {...field} />
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
                      <FormLabel>{t('access.email')}</FormLabel>
                      <FormControl>
                        <Input type="email" autoComplete="email" {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
                {apiError && <p className="text-danger text-sm">{apiError}</p>}
                <Button type="submit" className="w-full bg-primary" disabled={mutation.isPending}>
                  {mutation.isPending ? <Loader2 className="h-4 w-4 animate-spin" /> : t('access.submit')}
                </Button>
                <p className="text-center text-sm">
                  <Link to="/login" className="text-accent hover:underline">
                    {t('access.staffLogin')}
                  </Link>
                </p>
              </form>
            </Form>
          )}
        </CardContent>
      </Card>
    </div>
  )
}

export default PortalAccessPage
