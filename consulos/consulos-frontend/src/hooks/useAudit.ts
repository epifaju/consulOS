import { useQuery } from '@tanstack/react-query'
import { auditService } from '@/services/auditService'
import type { AuditLogListParams } from '@/types/audit'

export function useAuditLogs(params: AuditLogListParams) {
  return useQuery({
    queryKey: ['audit-logs', params],
    queryFn: async () => {
      const response = await auditService.list(params)
      return response.data.data
    },
  })
}
