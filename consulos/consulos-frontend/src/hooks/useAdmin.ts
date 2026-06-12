import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { adminService } from '@/services/adminService'
import type { Organisation, UserCreateRequest, UserUpdateRequest } from '@/types/admin'

export function useAdminUsers(page = 0) {
  return useQuery({
    queryKey: ['admin-users', page],
    queryFn: async () => {
      const response = await adminService.listUsers({ page, size: 20 })
      return response.data.data
    },
  })
}

export function useAdminRoles() {
  return useQuery({
    queryKey: ['admin-roles'],
    queryFn: async () => {
      const response = await adminService.listRoles()
      return response.data.data
    },
  })
}

export function useOrganisation() {
  return useQuery({
    queryKey: ['admin-organisation'],
    queryFn: async () => {
      const response = await adminService.getOrganisation()
      return response.data.data
    },
  })
}

export function useCreateUser() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (dto: UserCreateRequest) => adminService.createUser(dto),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['admin-users'] })
    },
  })
}

export function useUpdateUser(id: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (dto: UserUpdateRequest) => adminService.updateUser(id, dto),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['admin-users'] })
    },
  })
}

export function useToggleUserActif() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => adminService.toggleActif(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['admin-users'] })
    },
  })
}

export function useUpdateOrganisation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (dto: Organisation) => adminService.updateOrganisation(dto),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['admin-organisation'] })
    },
  })
}
