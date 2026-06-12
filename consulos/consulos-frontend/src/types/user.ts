export interface UserProfile {
  id: string
  email: string
  nom: string
  prenom: string
  role: string
  organisationId: string
  organisationCode?: string
  organisationNom?: string
}
