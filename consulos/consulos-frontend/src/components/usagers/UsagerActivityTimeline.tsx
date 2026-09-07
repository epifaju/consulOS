import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'
import { Badge } from '@/components/ui/badge'
import type { UsagerActivityItem } from '@/types/usager'

interface UsagerActivityTimelineProps {
  entries: UsagerActivityItem[]
  isLoading: boolean
}

export function UsagerActivityTimeline({ entries, isLoading }: UsagerActivityTimelineProps) {
  const { t } = useTranslation('usagers')

  if (isLoading) {
    return <p className="text-muted-foreground text-sm">{t('loading')}</p>
  }

  if (entries.length === 0) {
    return <p className="text-muted-foreground text-sm">{t('history.empty')}</p>
  }

  return (
    <ul className="space-y-4">
      {entries.map((entry) => (
        <li key={`${entry.category}-${entry.relatedId}-${entry.occurredAt}`} className="border-l-2 border-primary/30 pl-4">
          <div className="flex flex-wrap items-center gap-2">
            <Badge variant="outline">{t(`history.categories.${entry.category}`)}</Badge>
            <span className="text-muted-foreground text-xs">
              {entry.occurredAt ? new Date(entry.occurredAt).toLocaleString() : '—'}
            </span>
          </div>
          <p className="mt-1 text-sm font-medium">
            {entry.category === 'DOSSIER' && entry.relatedId ? (
              <Link className="text-primary hover:underline" to={`/dossiers/${entry.relatedId}`}>
                {entry.title}
              </Link>
            ) : (
              entry.title
            )}
          </p>
          {entry.detail && <p className="text-muted-foreground text-sm">{entry.detail}</p>}
        </li>
      ))}
    </ul>
  )
}
