import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { paymentService } from '@/services/paymentService'

export function usePaymentStatus(enabled = true) {
  return useQuery({
    queryKey: ['payments', 'status'],
    queryFn: async () => {
      try {
        const response = await paymentService.status()
        return response.data.data
      } catch {
        return { enabled: false, provider: 'off' }
      }
    },
    enabled,
    retry: false,
  })
}

export function usePaymentCheckout() {
  return useMutation({
    mutationFn: (encaissementId: string) => paymentService.checkout(encaissementId),
  })
}

export function usePaymentSession(providerSessionId: string | null) {
  return useQuery({
    queryKey: ['payments', 'session', providerSessionId],
    queryFn: async () => {
      const response = await paymentService.getSession(providerSessionId!)
      return response.data.data
    },
    enabled: Boolean(providerSessionId),
  })
}

export function useCompletePaymentSession() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (providerSessionId: string) => paymentService.complete(providerSessionId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['encaissements'] })
    },
  })
}
