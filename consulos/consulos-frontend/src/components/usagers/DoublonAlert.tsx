import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { AlertTriangle } from 'lucide-react'
import { Button } from '@/components/ui/button'
import type { DoublonGroup } from '@/types/usager'

interface DoublonAlertProps {
  doublons: DoublonGroup[]
  onIgnore: () => void
}

export function DoublonAlert({ doublons, onIgnore }: DoublonAlertProps) {
  const { t } = useTranslation('usagers')

  if (doublons.length === 0) {
    return null
  }

  return (
    <div className="rounded-md border border-warning/40 bg-warning/10 p-4">
      <div className="flex items-start gap-3">
        <AlertTriangle className="mt-0.5 h-5 w-5 shrink-0 text-warning" />
        <div className="flex-1 space-y-2">
          <p className="text-sm font-medium text-warning">{t('doublon.title')}</p>
          {doublons.map((group) => (
            <div key={group.motif} className="space-y-1">
              <p className="text-muted-foreground text-xs">{group.motif}</p>
              <ul className="space-y-1">
                {group.similaires.map((usager) => (
                  <li key={usager.id}>
                    <Link
                      to={`/usagers/${usager.id}`}
                      className="text-primary text-sm underline hover:text-accent"
                    >
                      {usager.numero} — {usager.prenom} {usager.nom}
                    </Link>
                  </li>
                ))}
              </ul>
            </div>
          ))}
          <Button type="button" variant="outline" size="sm" onClick={onIgnore}>
            {t('doublon.ignore')}
          </Button>
        </div>
      </div>
    </div>
  )
}

export default DoublonAlert
