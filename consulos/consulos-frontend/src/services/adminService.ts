import { apiClient } from '@/lib/apiClient'
import type { ApiResponse } from '@/types/api'
import type {
  Organisation,
  PageResponse,
  Role,
  UserAdmin,
  UserCreateRequest,
  UserCreateResponse,
  UserUpdateRequest,
} from '@/types/admin'

export const adminService = {
  listUsers: (params?: { page?: number; size?: number }) =>
    apiClient.get<ApiResponse<PageResponse<UserAdmin>>>('/admin/users', { params }),

  getUser: (id: string) =>
    apiClient.get<ApiResponse<UserAdmin>>(`/admin/users/${id}`),

  createUser: (dto: UserCreateRequest) =>
    apiClient.post<ApiResponse<UserCreateResponse>>('/admin/users', dto),

  updateUser: (id: string, dto: UserUpdateRequest) =>
    apiClient.put<ApiResponse<UserAdmin>>(`/admin/users/${id}`, dto),

  toggleActif: (id: string) =>
    apiClient.post<ApiResponse<UserAdmin>>(`/admin/users/${id}/toggle-actif`),

  resetPassword: (id: string) =>
    apiClient.post<ApiResponse<{ temporaryPassword: string }>>(`/admin/users/${id}/reset-password`),

  deleteUser: (id: string) =>
    apiClient.delete(`/admin/users/${id}`),

  listRoles: () =>
    apiClient.get<ApiResponse<Role[]>>('/admin/roles'),

  getOrganisation: () =>
    apiClient.get<ApiResponse<Organisation>>('/admin/organisation'),

  updateOrganisation: (dto: Organisation) =>
    apiClient.put<ApiResponse<Organisation>>('/admin/organisation', dto),
}
