import { apiClient } from '@/lib/apiClient'
import type { ApiResponse } from '@/types/api'
import type { AuditLog, AuditLogListParams, PageResponse } from '@/types/audit'

export const auditService = {
  list: (params: AuditLogListParams) =>
    apiClient.get<ApiResponse<PageResponse<AuditLog>>>('/audit/logs', { params }),

  exportCsv: (params: Omit<AuditLogListParams, 'page' | 'size'>) =>
    apiClient.get<Blob>('/audit/export', { params, responseType: 'blob' }),
}
