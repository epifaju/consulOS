import { apiClient } from '@/lib/apiClient'
import type { ApiResponse } from '@/types/api'
import type { ReportingMensuel } from '@/types/reporting'

export const reportingService = {
  mensuel: (annee: number, mois: number) =>
    apiClient.get<ApiResponse<ReportingMensuel>>('/reporting/mensuel', {
      params: { annee, mois },
    }),

  mensuelExcel: (annee: number, mois: number) =>
    apiClient.get<Blob>('/reporting/mensuel/excel', {
      params: { annee, mois },
      responseType: 'blob',
    }),

  dossiersCsv: (annee: number, mois: number) =>
    apiClient.get<Blob>('/reporting/dossiers/csv', {
      params: { annee, mois },
      responseType: 'blob',
    }),

  rdvCsv: (annee: number, mois: number) =>
    apiClient.get<Blob>('/reporting/rdv/csv', {
      params: { annee, mois },
      responseType: 'blob',
    }),
}
