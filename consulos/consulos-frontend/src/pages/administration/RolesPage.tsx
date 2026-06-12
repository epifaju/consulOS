import { useTranslation } from 'react-i18next'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { useAdminRoles } from '@/hooks/useAdmin'

export function RolesPage() {
  const { t } = useTranslation('admin')
  const rolesQuery = useAdminRoles()

  return (
    <div className="space-y-4">
      <h2 className="text-lg font-semibold">{t('roles.title')}</h2>
      <p className="text-muted-foreground text-sm">{t('roles.readonly')}</p>
      <div className="grid gap-4 md:grid-cols-2">
        {(rolesQuery.data ?? []).map((role) => (
          <Card key={role.id}>
            <CardHeader>
              <CardTitle className="text-base">{role.libelle}</CardTitle>
              <p className="text-muted-foreground font-mono text-xs">{role.code}</p>
            </CardHeader>
            <CardContent className="flex flex-wrap gap-1">
              {role.permissions.map((perm) => (
                <Badge key={perm} variant="outline" className="text-xs">
                  {perm}
                </Badge>
              ))}
            </CardContent>
          </Card>
        ))}
      </div>
    </div>
  )
}

export default RolesPage
