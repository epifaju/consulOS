import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { UserPlus } from 'lucide-react'
import { Button } from '@/components/ui/button'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { useAssignableAgents, useAssignDossierAgent } from '@/hooks/useDossiers'

interface AssignAgentPanelProps {
  dossierId: string
  currentAgentId: string | null
}

export function AssignAgentPanel({ dossierId, currentAgentId }: AssignAgentPanelProps) {
  const { t } = useTranslation('dossiers')
  const [selectedAgentId, setSelectedAgentId] = useState(currentAgentId ?? '')
  const agentsQuery = useAssignableAgents()
  const assignMutation = useAssignDossierAgent(dossierId)

  const handleAssign = () => {
    if (!selectedAgentId) return
    assignMutation.mutate(selectedAgentId)
  }

  return (
    <div className="flex flex-col gap-2 sm:flex-row sm:items-end">
      <div className="min-w-[220px] flex-1">
        <p className="mb-1 text-muted-foreground text-xs">{t('assign.title')}</p>
        <Select
          value={selectedAgentId || undefined}
          onValueChange={setSelectedAgentId}
        >
          <SelectTrigger>
            <SelectValue placeholder={t('assign.selectPlaceholder')} />
          </SelectTrigger>
          <SelectContent>
            {(agentsQuery.data ?? []).map((agent) => (
              <SelectItem key={agent.id} value={agent.id}>
                {agent.prenom} {agent.nom}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>
      <Button
        size="sm"
        disabled={!selectedAgentId || assignMutation.isPending || selectedAgentId === currentAgentId}
        onClick={handleAssign}
      >
        <UserPlus className="mr-2 h-4 w-4" />
        {assignMutation.isPending ? t('assign.pending') : t('assign.submit')}
      </Button>
    </div>
  )
}

export default AssignAgentPanel
