import { useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import {
  CartesianGrid,
  Legend,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import type { RecetteChartPoint } from '@/types/dashboard'

const MODE_COLORS: Record<string, string> = {
  ESPECES: '#1E8449',
  CARTE: '#2E86C1',
  VIREMENT: '#1B4F72',
  EXTERNE: '#B7950B',
  EXONERE: '#7f8c8d',
}

interface RecettesLineChartProps {
  data: RecetteChartPoint[]
}

export function RecettesLineChart({ data }: RecettesLineChartProps) {
  const { t } = useTranslation('dashboard')

  const { chartData, modes } = useMemo(() => {
    const byDate = new Map<string, Record<string, number>>()
    const modeSet = new Set<string>()
    for (const point of data) {
      modeSet.add(point.modePaiement)
      const entry = byDate.get(point.date) ?? {}
      entry[point.modePaiement] = point.montant
      byDate.set(point.date, entry)
    }
    const chartData = Array.from(byDate.entries())
      .map(([date, values]) => ({ date, ...values }))
      .sort((a, b) => a.date.localeCompare(b.date))
    return { chartData, modes: Array.from(modeSet) }
  }, [data])

  return (
    <Card className="h-full">
      <CardHeader>
        <CardTitle className="text-base">{t('charts.recettes')}</CardTitle>
      </CardHeader>
      <CardContent className="h-72">
        {chartData.length === 0 ? (
          <p className="text-muted-foreground text-sm">{t('empty')}</p>
        ) : (
          <ResponsiveContainer width="100%" height="100%">
            <LineChart data={chartData}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="date" tick={{ fontSize: 11 }} />
              <YAxis />
              <Tooltip />
              <Legend />
              {modes.map((mode) => (
                <Line
                  key={mode}
                  type="monotone"
                  dataKey={mode}
                  stroke={MODE_COLORS[mode] ?? '#64748b'}
                  dot={false}
                  name={mode}
                />
              ))}
            </LineChart>
          </ResponsiveContainer>
        )}
      </CardContent>
    </Card>
  )
}

export default RecettesLineChart
