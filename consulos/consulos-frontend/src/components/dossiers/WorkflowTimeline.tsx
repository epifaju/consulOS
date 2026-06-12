import { useTranslation } from 'react-i18next'
import { getStatutTimelineColor } from '@/utils/dossierStatus'
import type { HistoriqueEntry } from '@/types/dossier'

interface WorkflowTimelineProps {
  entries: HistoriqueEntry[]
  isLoading?: boolean
}

export function WorkflowTimeline({ entries, isLoading }: WorkflowTimelineProps) {
  const { t } = useTranslation('dossiers')

  if (isLoading) {
    return <p className="text-muted-foreground text-sm">{t('loading')}</p>
  }

  if (entries.length === 0) {
    return <p className="text-muted-foreground text-sm">{t('historique.empty')}</p>
  }

  return (
    <ol className="relative border-l border-gray-200 pl-6">
      {entries.map((entry) => (
        <li key={entry.id} className="mb-6 ml-2">
          <span
            className={`absolute -left-[9px] flex h-4 w-4 items-center justify-center rounded-full ring-4 ring-white ${getStatutTimelineColor(entry.statutVers)}`}
          />
          <div className="flex flex-col gap-1">
            <p className="text-sm font-semibold">
              {entry.statutDe
                ? t('historique.transition', {
                    from: t(`statuts.${entry.statutDe}`),
                    to: t(`statuts.${entry.statutVers}`),
                  })
                : t('historique.creation', { statut: t(`statuts.${entry.statutVers}`) })}
            </p>
            <p className="text-muted-foreground text-xs">
              {new Date(entry.createdAt).toLocaleString()}
              {entry.userNom ? ` · ${entry.userNom}` : ''}
            </p>
            {entry.commentaire && (
              <p className="text-sm italic text-gray-600">{entry.commentaire}</p>
            )}
          </div>
        </li>
      ))}
    </ol>
  )
}

export default WorkflowTimeline
