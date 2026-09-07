import { apiClient } from '@/lib/apiClient'
import type { ApiResponse } from '@/types/api'

export interface PaymentSession {
  id: string
  encaissementId: string
  provider: string
  providerSessionId: string
  status: string
  amount: number | string
  currency: string
  checkoutUrl: string | null
}

export const paymentService = {
  status: () =>
    apiClient.get<ApiResponse<{ enabled: boolean; provider: string }>>('/payments/status'),

  checkout: (encaissementId: string) =>
    apiClient.post<ApiResponse<PaymentSession>>('/payments/checkout', { encaissementId }),

  getSession: (providerSessionId: string) =>
    apiClient.get<ApiResponse<PaymentSession>>(`/payments/sessions/${providerSessionId}`),

  complete: (providerSessionId: string) =>
    apiClient.post<ApiResponse<PaymentSession>>(`/payments/sessions/${providerSessionId}/complete`),
}
