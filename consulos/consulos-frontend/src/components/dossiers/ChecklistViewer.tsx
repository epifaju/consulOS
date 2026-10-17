import { useTranslation } from 'react-i18next'
import { useDossierChecklist, useUpdateChecklistItem } from '@/hooks/useDossiers'
import { useAuthStore } from '@/stores/authStore'
import type { ChecklistItem } from '@/types/dossier'

const EDIT_ROLES = new Set(['ROLE_AGENT', 'ROLE_SUPERVISEUR', 'ROLE_ADMIN'])

interface ChecklistViewerProps {
  items: ChecklistItem[] | null | undefined
  dossierId?: string
}

function fallbackItems(items: ChecklistItem[] | null | undefined): ChecklistItem[] {
  return (items ?? []).map((item) => ({
    ...item,
    valide: item.valide === true,
  }))
}

export function ChecklistViewer({ items, dossierId }: ChecklistViewerProps) {
  const { t } = useTranslation('dossiers')
  const role = useAuthStore((state) => state.user?.role)
  const checklistQuery = useDossierChecklist(dossierId)
  const updateItem = useUpdateChecklistItem(dossierId ?? '')
  const canEdit = Boolean(dossierId) && EDIT_ROLES.has(role ?? '')

  const checklist = dossierId ? (checklistQuery.data ?? fallbackItems(items)) : fallbackItems(items)
  const validatedCount = checklist.filter((item) => item.valide === true).length

  if (checklist.length === 0) {
    return <p className="text-muted-foreground text-sm">{t('checklist.empty')}</p>
  }

  return (
    <div className="space-y-4">
      <p className="text-sm font-medium text-primary">
        {t('checklist.progress', { validated: validatedCount, total: checklist.length })}
      </p>
      <ul className="space-y-3">
        {checklist.map((item) => {
          const inputId = `checklist-${item.code}`
          return (
            <li key={item.code} className="flex items-start gap-3">
              <input
                id={inputId}
                type="checkbox"
                className="mt-1 h-4 w-4"
                checked={item.valide === true}
                disabled={!canEdit || updateItem.isPending}
                onChange={(event) => {
                  if (!dossierId || !canEdit) {
                    return
                  }
                  updateItem.mutate({ code: item.code, valide: event.target.checked })
                }}
              />
              <label htmlFor={inputId} className="cursor-pointer">
                <p className="text-sm font-medium">{item.libelle}</p>
                <p className="text-muted-foreground text-xs">{item.code}</p>
              </label>
            </li>
          )
        })}
      </ul>
    </div>
  )
}

export default ChecklistViewer
