import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation } from '@tanstack/react-query'
import { Link, useSearchParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { z } from 'zod'
import { ArrowLeft, CheckCircle2, Loader2 } from 'lucide-react'
import { AxiosError } from 'axios'
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
import type { ApiResponse } from '@/types/api'

const resetSchema = z
  .object({
    newPassword: z.string().min(8).regex(/^(?=.*[A-Z])(?=.*\d).{8,}$/),
    confirmPassword: z.string().min(8),
  })
  .refine((data) => data.newPassword === data.confirmPassword, {
    path: ['confirmPassword'],
    message: 'mismatch',
  })

type ResetFormValues = z.infer<typeof resetSchema>

export function ResetPasswordPage() {
  const { t } = useTranslation('auth')
  const [searchParams] = useSearchParams()
  const token = searchParams.get('token') ?? ''
  const [done, setDone] = useState(false)
  const [apiError, setApiError] = useState<string | null>(null)

  const form = useForm<ResetFormValues>({
    resolver: zodResolver(resetSchema),
    defaultValues: { newPassword: '', confirmPassword: '' },
  })

  const mutation = useMutation({
    mutationFn: (values: ResetFormValues) =>
      authService.resetPassword({
        token,
        newPassword: values.newPassword,
        confirmPassword: values.confirmPassword,
      }),
    onSuccess: () => {
      setDone(true)
      setApiError(null)
    },
    onError: (error: unknown) => {
      const axiosError = error as AxiosError<ApiResponse<null>>
      const code = axiosError.response?.data?.errorCode
      if (code === 'INVALID_RESET_TOKEN') {
        setApiError(t('resetPassword.errors.invalidToken'))
        return
      }
      setApiError(t('resetPassword.errors.generic'))
    },
  })

  const onSubmit = (values: ResetFormValues) => {
    setApiError(null)
    mutation.mutate(values)
  }

  if (!token) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-primary to-accent p-4">
        <Card className="w-full max-w-md shadow-xl">
          <CardHeader className="text-center">
            <CardTitle className="text-2xl text-primary">{t('resetPassword.title')}</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4 text-center">
            <p className="text-danger text-sm" role="alert">
              {t('resetPassword.errors.missingToken')}
            </p>
            <Button asChild variant="outline" className="w-full">
              <Link to="/forgot-password">{t('resetPassword.requestNewLink')}</Link>
            </Button>
            <Button asChild variant="ghost" className="w-full">
              <Link to="/login">
                <ArrowLeft className="mr-2 h-4 w-4" />
                {t('resetPassword.backToLogin')}
              </Link>
            </Button>
          </CardContent>
        </Card>
      </div>
    )
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-primary to-accent p-4">
      <Card className="w-full max-w-md shadow-xl">
        <CardHeader className="space-y-2 text-center">
          <CardTitle className="text-2xl text-primary">{t('resetPassword.title')}</CardTitle>
          <p className="text-muted-foreground text-sm">{t('resetPassword.subtitle')}</p>
        </CardHeader>
        <CardContent>
          {done ? (
            <div className="space-y-4 text-center">
              <CheckCircle2 className="mx-auto h-12 w-12 text-success" aria-hidden />
              <p className="text-sm">{t('resetPassword.success')}</p>
              <Button asChild className="w-full bg-primary">
                <Link to="/login">{t('resetPassword.backToLogin')}</Link>
              </Button>
            </div>
          ) : (
            <Form {...form}>
              <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
                <FormField
                  control={form.control}
                  name="newPassword"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>{t('resetPassword.newPassword')}</FormLabel>
                      <FormControl>
                        <Input type="password" autoComplete="new-password" {...field} />
                      </FormControl>
                      <FormMessage>
                        {form.formState.errors.newPassword
                          ? t('resetPassword.errors.policy')
                          : null}
                      </FormMessage>
                    </FormItem>
                  )}
                />
                <FormField
                  control={form.control}
                  name="confirmPassword"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>{t('resetPassword.confirmPassword')}</FormLabel>
                      <FormControl>
                        <Input type="password" autoComplete="new-password" {...field} />
                      </FormControl>
                      <FormMessage>
                        {form.formState.errors.confirmPassword
                          ? t('resetPassword.errors.mismatch')
                          : null}
                      </FormMessage>
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
                      {t('resetPassword.submitting')}
                    </>
                  ) : (
                    t('resetPassword.submit')
                  )}
                </Button>

                <Button asChild variant="ghost" className="w-full">
                  <Link to="/login">
                    <ArrowLeft className="mr-2 h-4 w-4" />
                    {t('resetPassword.backToLogin')}
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

export default ResetPasswordPage
