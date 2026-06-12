import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useTranslation } from 'react-i18next'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { useAdminRoles, useCreateUser, useUpdateUser } from '@/hooks/useAdmin'
import type { UserAdmin } from '@/types/admin'

const schema = z.object({
  email: z.string().email(),
  nom: z.string().min(1).max(100),
  prenom: z.string().min(1).max(100),
  roleId: z.string().uuid(),
  actif: z.boolean(),
})

type FormValues = z.infer<typeof schema>

interface UsersFormModalProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  user?: UserAdmin | null
  onCreated?: (tempPassword: string) => void
}

export function UsersFormModal({ open, onOpenChange, user, onCreated }: UsersFormModalProps) {
  const { t } = useTranslation('admin')
  const rolesQuery = useAdminRoles()
  const createMutation = useCreateUser()
  const updateMutation = useUpdateUser(user?.id ?? '')

  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      email: '',
      nom: '',
      prenom: '',
      roleId: '',
      actif: true,
    },
  })

  useEffect(() => {
    if (user) {
      const role = rolesQuery.data?.find((r) => r.code === user.roleCode)
      form.reset({
        email: user.email,
        nom: user.nom,
        prenom: user.prenom,
        roleId: role?.id ?? '',
        actif: user.actif,
      })
    } else {
      form.reset({ email: '', nom: '', prenom: '', roleId: '', actif: true })
    }
  }, [user, rolesQuery.data, form, open])

  const onSubmit = form.handleSubmit((values) => {
    if (user) {
      updateMutation.mutate(values, { onSuccess: () => onOpenChange(false) })
    } else {
      createMutation.mutate(values, {
        onSuccess: (res) => {
          onCreated?.(res.data.data.temporaryPassword)
          onOpenChange(false)
        },
      })
    }
  })

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{user ? t('users.edit') : t('users.create')}</DialogTitle>
        </DialogHeader>
        <form onSubmit={onSubmit} className="space-y-4">
          <div>
            <label className="mb-1 block text-sm font-medium">{t('users.fields.email')}</label>
            <Input {...form.register('email')} type="email" />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="mb-1 block text-sm font-medium">{t('users.fields.prenom')}</label>
              <Input {...form.register('prenom')} />
            </div>
            <div>
              <label className="mb-1 block text-sm font-medium">{t('users.fields.nom')}</label>
              <Input {...form.register('nom')} />
            </div>
          </div>
          <div>
            <label className="mb-1 block text-sm font-medium">{t('users.fields.role')}</label>
            <Select
              value={form.watch('roleId')}
              onValueChange={(v) => form.setValue('roleId', v)}
            >
              <SelectTrigger>
                <SelectValue placeholder={t('users.fields.role')} />
              </SelectTrigger>
              <SelectContent>
                {(rolesQuery.data ?? []).map((role) => (
                  <SelectItem key={role.id} value={role.id}>
                    {role.libelle}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          <label className="flex items-center gap-2 text-sm">
            <input
              type="checkbox"
              checked={form.watch('actif')}
              onChange={(e) => form.setValue('actif', e.target.checked)}
            />
            {t('users.fields.actif')}
          </label>
          <div className="flex justify-end gap-2">
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
              {t('actions.cancel')}
            </Button>
            <Button type="submit" disabled={createMutation.isPending || updateMutation.isPending}>
              {t('actions.save')}
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  )
}

export default UsersFormModal
