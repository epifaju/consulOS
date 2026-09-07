import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation } from '@tanstack/react-query'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { z } from 'zod'
import { Loader2 } from 'lucide-react'
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
import { useAuthStore } from '@/stores/authStore'
import type { ApiResponse } from '@/types/api'
import type { LoginResponse } from '@/types/auth'

const loginSchema = z.object({
  email: z.string().email(),
  password: z.string().min(8),
})

type LoginFormValues = z.infer<typeof loginSchema>

function resolveRedirectPath(role: string, returnUrl: string | null): string {
  if (returnUrl && returnUrl.startsWith('/') && !returnUrl.startsWith('/login')) {
    return returnUrl
  }
  if (role === 'ROLE_ADMIN') {
    return '/administration'
  }
  if (role === 'ROLE_CAISSIER') {
    return '/caisse'
  }
  return '/'
}

export function LoginPage() {
  const { t } = useTranslation('auth')
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const login = useAuthStore((state) => state.login)
  const [apiError, setApiError] = useState<string | null>(null)

  const form = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      email: '',
      password: '',
    },
  })

  const loginMutation = useMutation({
    mutationFn: (values: LoginFormValues) => authService.login(values),
    onSuccess: (response) => {
      const payload = response.data.data as LoginResponse
      login(payload.accessToken, {
        id: payload.user.id,
        email: payload.user.email,
        nom: payload.user.nom,
        prenom: payload.user.prenom,
        role: payload.user.role,
        organisationId: payload.user.organisationId,
        organisationCode: payload.user.organisationCode ?? '',
      })
      const rawReturnUrl = searchParams.get('returnUrl')
      const returnUrl = rawReturnUrl ? decodeURIComponent(rawReturnUrl) : null
      navigate(resolveRedirectPath(payload.user.role, returnUrl), { replace: true })
    },
    onError: (error: AxiosError<ApiResponse<null>>) => {
      const errorCode = error.response?.data?.errorCode
      if (errorCode === 'USER_INACTIVE') {
        setApiError(t('errors.inactive'))
      } else if (errorCode === 'RATE_LIMIT_EXCEEDED') {
        setApiError(t('errors.rateLimit'))
      } else if (error.response?.status === 401) {
        setApiError(t('errors.invalidCredentials'))
      } else {
        setApiError(t('errors.generic'))
      }
    },
  })

  const onSubmit = (values: LoginFormValues) => {
    setApiError(null)
    loginMutation.mutate(values)
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-primary to-accent p-4">
      <Card className="w-full max-w-md shadow-xl">
        <CardHeader className="space-y-2 text-center">
          <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-primary-lighter text-xl font-bold text-primary">
            CO
          </div>
          <CardTitle className="text-2xl text-primary">ConsulOS</CardTitle>
          <p className="text-muted-foreground text-sm">{t('welcome')}</p>
        </CardHeader>
        <CardContent>
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
              <FormField
                control={form.control}
                name="password"
                render={({ field }) => (
                  <FormItem>
                    <div className="flex items-center justify-between">
                      <FormLabel>{t('password')}</FormLabel>
                      <Link
                        to="/forgot-password"
                        className="text-accent text-xs hover:underline"
                      >
                        {t('forgotPasswordLink')}
                      </Link>
                    </div>
                    <FormControl>
                      <Input type="password" autoComplete="current-password" {...field} />
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
                disabled={loginMutation.isPending}
              >
                {loginMutation.isPending ? (
                  <>
                    <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                    {t('submitting')}
                  </>
                ) : (
                  t('submit')
                )}
              </Button>

              <p className="text-center text-sm">
                <Link to="/portal" className="text-accent hover:underline">
                  {t('portalLink')}
                </Link>
              </p>
            </form>
          </Form>
        </CardContent>
      </Card>
    </div>
  )
}

export default LoginPage
