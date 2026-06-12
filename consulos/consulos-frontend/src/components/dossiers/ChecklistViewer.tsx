import { useTranslation } from 'react-i18next'
import type { ChecklistItem } from '@/types/dossier'

interface ChecklistViewerProps {
  items: ChecklistItem[] | null | undefined
}

export function ChecklistViewer({ items }: ChecklistViewerProps) {
  const { t } = useTranslation('dossiers')
  const checklist = items ?? []
  const validatedCount = 0

  if (checklist.length === 0) {
    return <p className="text-muted-foreground text-sm">{t('checklist.empty')}</p>
  }

  return (
    <div className="space-y-4">
      <p className="text-sm font-medium text-primary">
        {t('checklist.progress', { validated: validatedCount, total: checklist.length })}
      </p>
      <ul className="space-y-3">
        {checklist.map((item) => (
          <li key={item.code} className="flex items-start gap-3">
            <input type="checkbox" checked={false} disabled readOnly className="mt-1 h-4 w-4" />
            <div>
              <p className="text-sm font-medium">{item.libelle}</p>
              <p className="text-muted-foreground text-xs">{item.code}</p>
            </div>
          </li>
        ))}
      </ul>
    </div>
  )
}

export default ChecklistViewer
