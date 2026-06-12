import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Pencil, Plus, Power } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { UsersFormModal } from '@/components/administration/UsersFormModal'
import { useAdminUsers, useToggleUserActif } from '@/hooks/useAdmin'
import type { UserAdmin } from '@/types/admin'

export function UsersPage() {
  const { t } = useTranslation('admin')
  const [page, setPage] = useState(0)
  const [modalOpen, setModalOpen] = useState(false)
  const [editingUser, setEditingUser] = useState<UserAdmin | null>(null)
  const [tempPassword, setTempPassword] = useState<string | null>(null)

  const usersQuery = useAdminUsers(page)
  const toggleMutation = useToggleUserActif()

  const users = usersQuery.data?.content ?? []
  const totalPages = usersQuery.data?.totalPages ?? 1

  const openCreate = () => {
    setEditingUser(null)
    setModalOpen(true)
  }

  const openEdit = (user: UserAdmin) => {
    setEditingUser(user)
    setModalOpen(true)
  }

  return (
    <div className="space-y-4">
      <div className="flex justify-between">
        <h2 className="text-lg font-semibold">{t('users.title')}</h2>
        <Button onClick={openCreate} className="bg-primary">
          <Plus className="mr-2 h-4 w-4" />
          {t('users.new')}
        </Button>
      </div>

      {tempPassword && (
        <div className="rounded-md border border-amber-200 bg-amber-50 p-3 text-sm">
          {t('users.tempPassword')}: <code className="font-mono font-bold">{tempPassword}</code>
        </div>
      )}

      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>{t('users.fields.name')}</TableHead>
            <TableHead>{t('users.fields.email')}</TableHead>
            <TableHead>{t('users.fields.role')}</TableHead>
            <TableHead>{t('users.fields.actif')}</TableHead>
            <TableHead>{t('users.actions')}</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {users.map((user) => (
            <TableRow key={user.id}>
              <TableCell>{user.prenom} {user.nom}</TableCell>
              <TableCell>{user.email}</TableCell>
              <TableCell>{user.roleLibelle}</TableCell>
              <TableCell>
                <Badge variant={user.actif ? 'default' : 'secondary'}>
                  {user.actif ? t('users.active') : t('users.inactive')}
                </Badge>
              </TableCell>
              <TableCell>
                <div className="flex gap-1">
                  <Button variant="ghost" size="sm" onClick={() => openEdit(user)}>
                    <Pencil className="h-4 w-4" />
                  </Button>
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => toggleMutation.mutate(user.id)}
                  >
                    <Power className="h-4 w-4" />
                  </Button>
                </div>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>

      {totalPages > 1 && (
        <div className="flex justify-center gap-2">
          <Button variant="outline" size="sm" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
            {t('pagination.prev')}
          </Button>
          <span className="text-sm">{page + 1} / {totalPages}</span>
          <Button
            variant="outline"
            size="sm"
            disabled={page >= totalPages - 1}
            onClick={() => setPage((p) => p + 1)}
          >
            {t('pagination.next')}
          </Button>
        </div>
      )}

      <UsersFormModal
        open={modalOpen}
        onOpenChange={setModalOpen}
        user={editingUser}
        onCreated={setTempPassword}
      />
    </div>
  )
}

export default UsersPage
