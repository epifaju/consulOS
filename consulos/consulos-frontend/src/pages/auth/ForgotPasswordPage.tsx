import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { z } from 'zod'
import { ArrowLeft, Loader2, MailCheck } from 'lucide-react'
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
import { authService } from '@/services/authService'

const forgotSchema = z.object({
  email: z.string().email(),
})

type ForgotFormValues = z.infer<typeof forgotSchema>

export function ForgotPasswordPage() {
  const { t } = useTranslation('auth')
  const [sent, setSent] = useState(false)
  const [apiError, setApiError] = useState<string | null>(null)

  const form = useForm<ForgotFormValues>({
    resolver: zodResolver(forgotSchema),
    defaultValues: { email: '' },
  })

  const mutation = useMutation({
    mutationFn: (values: ForgotFormValues) => authService.forgotPassword(values.email),
    onSuccess: () => {
      setSent(true)
      setApiError(null)
    },
    onError: () => {
      setApiError(t('forgotPassword.errors.generic'))
    },
  })

  const onSubmit = (values: ForgotFormValues) => {
    setApiError(null)
    mutation.mutate(values)
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-primary to-accent p-4">
      <Card className="w-full max-w-md shadow-xl">
        <CardHeader className="space-y-2 text-center">
          <CardTitle className="text-2xl text-primary">{t('forgotPassword.title')}</CardTitle>
          <p className="text-muted-foreground text-sm">{t('forgotPassword.subtitle')}</p>
        </CardHeader>
        <CardContent>
          {sent ? (
            <div className="space-y-4 text-center">
              <MailCheck className="mx-auto h-12 w-12 text-success" aria-hidden />
              <p className="text-sm">{t('forgotPassword.success')}</p>
              <p className="text-muted-foreground text-xs">{t('forgotPassword.mailhogHint')}</p>
              <Button asChild variant="outline" className="w-full">
                <Link to="/login">
                  <ArrowLeft className="mr-2 h-4 w-4" />
                  {t('forgotPassword.backToLogin')}
                </Link>
              </Button>
            </div>
          ) : (
            <Form {...form}>
              <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
                <FormField
                  control={form.control}
                  name="email"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>{t('email')}</FormLabel>
                      <FormControl>
                        <Input type="email" autoComplete="email" {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                {apiError && (
                  <p className="text-danger text-sm" role="alert">
                    {apiError}
                  </p>
                )}

                <Button
                  type="submit"
                  className="w-full bg-primary hover:bg-primary/90"
                  disabled={mutation.isPending}
                >
                  {mutation.isPending ? (
                    <>
                      <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                      {t('forgotPassword.submitting')}
                    </>
                  ) : (
                    t('forgotPassword.submit')
                  )}
                </Button>

                <Button asChild variant="ghost" className="w-full">
                  <Link to="/login">
                    <ArrowLeft className="mr-2 h-4 w-4" />
                    {t('forgotPassword.backToLogin')}
                  </Link>
                </Button>
              </form>
            </Form>
          )}
        </CardContent>
      </Card>
    </div>
  )
}

export default ForgotPasswordPage
