import { apiClient } from '@/lib/apiClient'
import type { ApiResponse } from '@/types/api'
import type { RegionalReportingMensuel } from '@/types/regionalReporting'

export const regionalReportingService = {
  mensuel: (annee: number, mois: number, reseauId?: string) =>
    apiClient.get<ApiResponse<RegionalReportingMensuel>>('/regional/reporting/mensuel', {
      params: {
        annee,
        mois,
        ...(reseauId ? { reseauId } : {}),
      },
    }),

  mensuelExcel: (annee: number, mois: number, reseauId?: string) =>
    apiClient.get<Blob>('/regional/reporting/mensuel/excel', {
      params: {
        annee,
        mois,
        ...(reseauId ? { reseauId } : {}),
      },
      responseType: 'blob',
    }),
}
