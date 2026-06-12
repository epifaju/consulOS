import { useQuery } from '@tanstack/react-query'
import { dashboardService } from '@/services/dashboardService'
import type { DashboardPeriode } from '@/types/dashboard'

export function useDashboardStats(dateDebut?: string, dateFin?: string) {
  return useQuery({
    queryKey: ['dashboard-stats', dateDebut, dateFin],
    queryFn: async () => {
      const response = await dashboardService.getStats({ dateDebut, dateFin })
      return response.data.data
    },
  })
}

export function useDossiersChart(periode: DashboardPeriode = 'MOIS') {
  return useQuery({
    queryKey: ['dashboard-dossiers-chart', periode],
    queryFn: async () => {
      const response = await dashboardService.getDossiersChart(periode)
      return response.data.data
    },
  })
}

export function useRecettesChart(periode: DashboardPeriode = 'MOIS') {
  return useQuery({
    queryKey: ['dashboard-recettes-chart', periode],
    queryFn: async () => {
      const response = await dashboardService.getRecettesChart(periode)
      return response.data.data
    },
  })
}

export function useDashboardAlerts() {
  return useQuery({
    queryKey: ['dashboard-alerts'],
    queryFn: async () => {
      const response = await dashboardService.getAlerts()
      return response.data.data
    },
  })
}

export function useAgentsCharge() {
  return useQuery({
    queryKey: ['dashboard-agents-charge'],
    queryFn: async () => {
      const response = await dashboardService.getAgentsCharge()
      return response.data.data
    },
  })
}
