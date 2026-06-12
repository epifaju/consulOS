import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { usagerService } from '@/services/usagerService'
import { useDebounce } from '@/hooks/useDebounce'
import type {
  DoublonParams,
  UsagerCreateRequest,
  UsagerListParams,
  UsagerUpdateRequest,
} from '@/types/usager'

export function useUsagers(params: UsagerListParams) {
  return useQuery({
    queryKey: ['usagers', params],
    queryFn: async () => {
      const response = await usagerService.list(params)
      return response.data.data
    },
  })
}

export function useUsager(id: string) {
  return useQuery({
    queryKey: ['usager', id],
    queryFn: async () => {
      const response = await usagerService.getById(id)
      return response.data.data
    },
    enabled: Boolean(id) && id !== 'new',
  })
}

export function useCreateUsager() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (dto: UsagerCreateRequest) => usagerService.create(dto),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['usagers'] })
    },
  })
}

export function useUpdateUsager(id: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (dto: UsagerUpdateRequest) => usagerService.update(id, dto),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['usagers'] })
      void queryClient.invalidateQueries({ queryKey: ['usager', id] })
    },
  })
}

export function useDeleteUsager() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => usagerService.delete(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['usagers'] })
    },
  })
}

export function useSearchUsagers(query: string) {
  const debouncedQuery = useDebounce(query, 300)
  return useQuery({
    queryKey: ['usagers-search', debouncedQuery],
    queryFn: async () => {
      const response = await usagerService.search(debouncedQuery)
      return response.data.data
    },
    enabled: debouncedQuery.trim().length >= 2,
  })
}

export function useDoublons(params: DoublonParams | null, enabled = true) {
  return useQuery({
    queryKey: ['usagers-doublons', params],
    queryFn: async () => {
      if (!params) return []
      const response = await usagerService.doublons(params)
      return response.data.data
    },
    enabled:
      enabled &&
      Boolean(params?.nom && params.dateNaissance && params.nationalite),
  })
}
