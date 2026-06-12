import { useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import {
  Bar,
  BarChart,
  CartesianGrid,
  Legend,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import type { DossierChartRow } from '@/types/dashboard'

const STATUT_COLORS: Record<string, string> = {
  BROUILLON: '#94a3b8',
  SOUMIS: '#2E86C1',
  INCOMPLET: '#B7950B',
  EN_REVUE: '#e67e22',
  VALIDE: '#1E8449',
  REJETE: '#C0392B',
  CLOTURE: '#1B4F72',
}

interface DossiersBarChartProps {
  data: DossierChartRow[]
}

export function DossiersBarChart({ data }: DossiersBarChartProps) {
  const { t } = useTranslation('dashboard')

  const { chartData, statuts } = useMemo(() => {
    const byType = new Map<string, Record<string, number>>()
    const statutSet = new Set<string>()
    for (const row of data) {
      statutSet.add(row.statut)
      const entry = byType.get(row.typeService) ?? {}
      entry[row.statut] = row.count
      byType.set(row.typeService, entry)
    }
    const chartData = Array.from(byType.entries()).map(([typeService, counts]) => ({
      typeService,
      ...counts,
    }))
    return { chartData, statuts: Array.from(statutSet) }
  }, [data])

  return (
    <Card className="h-full">
      <CardHeader>
        <CardTitle className="text-base">{t('charts.dossiers')}</CardTitle>
      </CardHeader>
      <CardContent className="h-72">
        {chartData.length === 0 ? (
          <p className="text-muted-foreground text-sm">{t('empty')}</p>
        ) : (
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={chartData}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="typeService" tick={{ fontSize: 11 }} />
              <YAxis allowDecimals={false} />
              <Tooltip />
              <Legend />
              {statuts.map((statut) => (
                <Bar
                  key={statut}
                  dataKey={statut}
                  stackId="a"
                  fill={STATUT_COLORS[statut] ?? '#64748b'}
                  name={statut}
                />
              ))}
            </BarChart>
          </ResponsiveContainer>
        )}
      </CardContent>
    </Card>
  )
}

export default DossiersBarChart
