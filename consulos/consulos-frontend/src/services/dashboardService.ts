import { apiClient } from '@/lib/apiClient'
import type { ApiResponse } from '@/types/api'
import type {
  AgentCharge,
  Alerte,
  DashboardPeriode,
  DashboardStats,
  DossierChartRow,
  RecetteChartPoint,
} from '@/types/dashboard'

export const dashboardService = {
  getStats: (params?: { dateDebut?: string; dateFin?: string }) =>
    apiClient.get<ApiResponse<DashboardStats>>('/dashboard/stats', { params }),

  getDossiersChart: (periode: DashboardPeriode = 'MOIS') =>
    apiClient.get<ApiResponse<DossierChartRow[]>>('/dashboard/dossiers-chart', { params: { periode } }),

  getRecettesChart: (periode: DashboardPeriode = 'MOIS') =>
    apiClient.get<ApiResponse<RecetteChartPoint[]>>('/dashboard/recettes-chart', { params: { periode } }),

  getAlerts: () =>
    apiClient.get<ApiResponse<Alerte[]>>('/dashboard/alerts'),

  getAgentsCharge: () =>
    apiClient.get<ApiResponse<AgentCharge[]>>('/dashboard/agents-charge'),
}
