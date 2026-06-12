export interface UserAdmin {
  id: string
  email: string
  nom: string
  prenom: string
  roleCode: string
  roleLibelle: string
  actif: boolean
  derniereConnexion: string | null
  createdAt: string
}

export interface UserCreateRequest {
  email: string
  nom: string
  prenom: string
  roleId: string
  actif?: boolean
}

export interface UserUpdateRequest {
  email?: string
  nom?: string
  prenom?: string
  roleId?: string
  actif?: boolean
}

export interface UserCreateResponse {
  user: UserAdmin
  temporaryPassword: string
}

export interface Role {
  id: string
  code: string
  libelle: string
  permissions: string[]
}

export interface Organisation {
  id: string
  code: string
  nom: string
  paysCode: string
  langueDefaut: string
  deviseDefaut: string
  actif: boolean
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}
