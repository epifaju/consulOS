import { QueryClientProvider } from '@tanstack/react-query'
import { AuthBootstrap } from '@/components/auth/AuthBootstrap'
import { Toaster } from '@/components/ui/toaster'
import { queryClient } from '@/lib/queryClient'
import '@/lib/i18n'
import { AppRouter } from '@/router/AppRouter'

export function ConsulOSApp() {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthBootstrap>
        <AppRouter />
      </AuthBootstrap>
      <Toaster />
    </QueryClientProvider>
  )
}

export default ConsulOSApp
