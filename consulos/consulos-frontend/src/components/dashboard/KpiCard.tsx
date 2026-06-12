import { ArrowDown, ArrowUp, Minus } from 'lucide-react'
import type { LucideIcon } from 'lucide-react'
import { Card, CardContent } from '@/components/ui/card'
import type { Kpi } from '@/types/dashboard'

interface KpiCardProps {
  kpi: Kpi
  icon: LucideIcon
  label: string
  formatValue?: (value: number) => string
}

export function KpiCard({ kpi, icon: Icon, label, formatValue }: KpiCardProps) {
  const display = formatValue ? formatValue(kpi.valeur) : String(kpi.valeur)
  const trend = kpi.tendancePourcent
  const TrendIcon = trend > 0 ? ArrowUp : trend < 0 ? ArrowDown : Minus
  const trendColor = trend > 0 ? 'text-success' : trend < 0 ? 'text-danger' : 'text-muted-foreground'

  return (
    <Card>
      <CardContent className="flex items-start justify-between p-4">
        <div>
          <p className="text-muted-foreground text-sm">{label}</p>
          <p className="text-2xl font-bold text-primary">{display}</p>
          <div className={`mt-1 flex items-center gap-1 text-xs ${trendColor}`}>
            <TrendIcon className="h-3 w-3" />
            <span>{Math.abs(trend)}% vs période préc.</span>
          </div>
        </div>
        <div className="rounded-lg bg-light p-2">
          <Icon className="h-5 w-5 text-accent" />
        </div>
      </CardContent>
    </Card>
  )
}

export default KpiCard
