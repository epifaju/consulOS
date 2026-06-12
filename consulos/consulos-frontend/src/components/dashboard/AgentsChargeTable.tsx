import { useTranslation } from 'react-i18next'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import type { AgentCharge } from '@/types/dashboard'

interface AgentsChargeTableProps {
  agents: AgentCharge[]
}

export function AgentsChargeTable({ agents }: AgentsChargeTableProps) {
  const { t } = useTranslation('dashboard')

  return (
    <Card className="h-full">
      <CardHeader>
        <CardTitle className="text-base">{t('agents.title')}</CardTitle>
      </CardHeader>
      <CardContent>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>{t('agents.name')}</TableHead>
              <TableHead className="text-right">{t('agents.dossiers')}</TableHead>
              <TableHead className="text-right">{t('agents.rdv')}</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {agents.length === 0 && (
              <TableRow>
                <TableCell colSpan={3} className="text-muted-foreground text-center text-sm">
                  {t('agents.empty')}
                </TableCell>
              </TableRow>
            )}
            {agents.map((agent) => (
              <TableRow key={agent.agentId}>
                <TableCell>{agent.agentNom}</TableCell>
                <TableCell className="text-right">{agent.nbDossiersEnCours}</TableCell>
                <TableCell className="text-right">{agent.nbRdvAujourdhui}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </CardContent>
    </Card>
  )
}

export default AgentsChargeTable
