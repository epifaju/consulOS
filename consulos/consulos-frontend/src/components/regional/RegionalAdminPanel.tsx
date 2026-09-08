import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Loader2, UserMinus, UserPlus } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import {
  useAttachReseauPoste,
  useDetachReseauPoste,
  useReseauMembres,
} from '@/hooks/useReseau'
import type { ReseauPoste } from '@/services/reseauService'

interface RegionalAdminPanelProps {
  reseauId: string | null
  roleReseau: string | null | undefined
  postes: ReseauPoste[]
  enabled: boolean
}

export function RegionalAdminPanel({
  reseauId,
  roleReseau,
  postes,
  enabled,
}: RegionalAdminPanelProps) {
  const { t } = useTranslation('regional')
  const isAdmin = roleReseau?.toUpperCase() === 'ADMIN'
  const [orgCode, setOrgCode] = useState('')
  const [error, setError] = useState<string | null>(null)

  const membresQuery = useReseauMembres(reseauId, enabled && Boolean(reseauId))
  const attachMutation = useAttachReseauPoste()
  const detachMutation = useDetachReseauPoste()

  if (!enabled || !reseauId) {
    return null
  }

  const handleAttach = () => {
    const code = orgCode.trim()
    if (!code) return
    setError(null)
    attachMutation.mutate(
      { reseauId, organisationCode: code },
      {
        onSuccess: () => setOrgCode(''),
        onError: () => setError(t('admin.attachError')),
      },
    )
  }

  return (
    <div className="grid gap-4 lg:grid-cols-2">
      <Card>
        <CardHeader className="pb-2">
          <CardTitle className="text-lg text-primary">{t('admin.postesTitle')}</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          {isAdmin && (
            <div className="flex flex-col gap-2 sm:flex-row sm:items-end">
              <div className="flex-1">
                <label className="mb-1 block text-xs font-medium">{t('admin.orgCode')}</label>
                <Input
                  value={orgCode}
                  placeholder="SEN-FR"
                  onChange={(e) => setOrgCode(e.target.value)}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter') handleAttach()
                  }}
                />
              </div>
              <Button
                className="bg-primary"
                disabled={attachMutation.isPending || !orgCode.trim()}
                onClick={handleAttach}
              >
                {attachMutation.isPending ? (
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                ) : (
                  <UserPlus className="mr-2 h-4 w-4" />
                )}
                {t('admin.attach')}
              </Button>
            </div>
          )}
          {error && <p className="text-danger text-xs">{error}</p>}
          {!isAdmin && (
            <p className="text-muted-foreground text-xs">{t('admin.readOnly')}</p>
          )}
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>{t('postes.code')}</TableHead>
                <TableHead>{t('postes.nom')}</TableHead>
                {isAdmin && <TableHead className="w-24" />}
              </TableRow>
            </TableHeader>
            <TableBody>
              {postes.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={isAdmin ? 3 : 2} className="text-muted-foreground text-center text-sm">
                    {t('postes.empty')}
                  </TableCell>
                </TableRow>
              ) : (
                postes.map((poste) => (
                  <TableRow key={poste.organisationId}>
                    <TableCell className="font-medium">{poste.code}</TableCell>
                    <TableCell>{poste.nom}</TableCell>
                    {isAdmin && (
                      <TableCell>
                        <Button
                          variant="outline"
                          size="sm"
                          disabled={detachMutation.isPending}
                          onClick={() =>
                            detachMutation.mutate({
                              reseauId,
                              organisationId: poste.organisationId,
                            })
                          }
                        >
                          <UserMinus className="h-4 w-4" />
                        </Button>
                      </TableCell>
                    )}
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </CardContent>
      </Card>

      <Card>
        <CardHeader className="pb-2">
          <CardTitle className="text-lg text-primary">{t('admin.membresTitle')}</CardTitle>
        </CardHeader>
        <CardContent>
          {membresQuery.isLoading ? (
            <p className="text-muted-foreground text-sm">{t('loading')}</p>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>{t('admin.membre')}</TableHead>
                  <TableHead>{t('admin.email')}</TableHead>
                  <TableHead>{t('admin.role')}</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {(membresQuery.data ?? []).length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={3} className="text-muted-foreground text-center text-sm">
                      {t('admin.noMembres')}
                    </TableCell>
                  </TableRow>
                ) : (
                  (membresQuery.data ?? []).map((m) => (
                    <TableRow key={m.userId}>
                      <TableCell className="font-medium">
                        {m.prenom} {m.nom}
                      </TableCell>
                      <TableCell className="text-sm">{m.email}</TableCell>
                      <TableCell>{m.roleReseau}</TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>
    </div>
  )
}

export default RegionalAdminPanel
