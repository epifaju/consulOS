import { useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { ChevronLeft, ChevronRight, Plus } from 'lucide-react'
import { Button } from '@/components/ui/button'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { useAgenda } from '@/hooks/useRendezvous'
import { useTypesService } from '@/hooks/useDossiers'
import { RdvCreateModal } from '@/components/rendezvous/RdvCreateModal'
import { RdvDetailDrawer } from '@/components/rendezvous/RdvDetailDrawer'
import {
  formatDateISO,
  getRdvStatutBadgeClass,
  getWeekDays,
  getWeekStart,
  TIME_SLOTS,
} from '@/utils/rdvUtils'
import type { RdvAgendaItem } from '@/types/rendezvous'

export function AgendaPage() {
  const { t } = useTranslation('rendezvous')
  const [weekStart, setWeekStart] = useState(() => getWeekStart(new Date()))
  const [typeServiceFilter, setTypeServiceFilter] = useState('')
  const [selectedRdvId, setSelectedRdvId] = useState<string | null>(null)
  const [drawerOpen, setDrawerOpen] = useState(false)
  const [createOpen, setCreateOpen] = useState(false)
  const [createInitialDate, setCreateInitialDate] = useState<Date | undefined>()
  const [createInitialTime, setCreateInitialTime] = useState<string | undefined>()

  const weekDays = useMemo(() => getWeekDays(weekStart), [weekStart])
  const dateFin = weekDays[4] ?? weekDays[weekDays.length - 1]

  const agendaQuery = useAgenda({
    dateDebut: formatDateISO(weekDays[0] ?? weekStart),
    dateFin: formatDateISO(dateFin ?? weekStart),
  })

  const typesQuery = useTypesService()

  const filteredRdvs = useMemo(() => {
    const items = agendaQuery.data ?? []
    if (!typeServiceFilter) return items
    return items.filter((r) => r.typeServiceId === typeServiceFilter)
  }, [agendaQuery.data, typeServiceFilter])

  const rdvsByDayAndTime = useMemo(() => {
    const map = new Map<string, RdvAgendaItem[]>()
    for (const rdv of filteredRdvs) {
      const d = new Date(rdv.dateHeure)
      const dateKey = formatDateISO(d)
      const timeKey = `${String(d.getUTCHours()).padStart(2, '0')}:${String(d.getUTCMinutes()).padStart(2, '0')}`
      const key = `${dateKey}|${timeKey}`
      const list = map.get(key) ?? []
      list.push(rdv)
      map.set(key, list)
    }
    return map
  }, [filteredRdvs])

  const shiftWeek = (delta: number) => {
    setWeekStart((prev) => {
      const d = new Date(prev)
      d.setDate(d.getDate() + delta * 7)
      return d
    })
  }

  const openCreate = (day: Date, time: string) => {
    setCreateInitialDate(day)
    setCreateInitialTime(time)
    setCreateOpen(true)
  }

  const openDetail = (rdvId: string) => {
    setSelectedRdvId(rdvId)
    setDrawerOpen(true)
  }

  return (
    <div className="space-y-4">
      <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
        <h1 className="text-2xl font-semibold text-primary">{t('title')}</h1>
        <div className="flex flex-wrap items-center gap-2">
          <Button variant="outline" size="icon" onClick={() => shiftWeek(-1)}>
            <ChevronLeft className="h-4 w-4" />
          </Button>
          <span className="min-w-[180px] text-center text-sm font-medium">
            {weekDays[0]?.toLocaleDateString()} — {dateFin?.toLocaleDateString()}
          </span>
          <Button variant="outline" size="icon" onClick={() => shiftWeek(1)}>
            <ChevronRight className="h-4 w-4" />
          </Button>
          <Select
            value={typeServiceFilter || 'ALL'}
            onValueChange={(v) => setTypeServiceFilter(v === 'ALL' ? '' : v)}
          >
            <SelectTrigger className="w-[200px]">
              <SelectValue placeholder={t('filters.typeService')} />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="ALL">{t('filters.allTypes')}</SelectItem>
              {(typesQuery.data ?? []).map((type) => (
                <SelectItem key={type.id} value={type.id}>
                  {type.libelle}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          <Button onClick={() => setCreateOpen(true)} className="bg-primary">
            <Plus className="mr-2 h-4 w-4" />
            {t('actions.new')}
          </Button>
        </div>
      </div>

      <div className="overflow-x-auto rounded-lg border bg-white">
        <div
          className="grid min-w-[900px]"
          style={{ gridTemplateColumns: `80px repeat(${weekDays.length}, 1fr)` }}
        >
          <div className="border-b border-r bg-gray-50 p-2 text-xs font-medium" />
          {weekDays.map((day) => (
            <div
              key={day.toISOString()}
              className="border-b border-r bg-gray-50 p-2 text-center text-xs font-semibold text-primary"
            >
              {day.toLocaleDateString(undefined, { weekday: 'short', day: 'numeric' })}
            </div>
          ))}

          {TIME_SLOTS.map((time) => (
            <div key={`row-${time}`} className="contents">
              <div className="border-b border-r p-1 text-right text-xs text-muted-foreground">
                {time}
              </div>
              {weekDays.map((day) => {
                const key = `${formatDateISO(day)}|${time}`
                const rdvs = rdvsByDayAndTime.get(key) ?? []
                return (
                  <div
                    key={key}
                    className="relative min-h-[40px] border-b border-r p-0.5"
                  >
                    {rdvs.length === 0 ? (
                      <button
                        type="button"
                        className="h-full w-full rounded hover:bg-light/60"
                        onClick={() => openCreate(day, time)}
                        aria-label={t('actions.addSlot')}
                      />
                    ) : (
                      rdvs.map((rdv) => (
                        <button
                          key={rdv.id}
                          type="button"
                          onClick={() => openDetail(rdv.id)}
                          className={`mb-0.5 w-full rounded border px-1 py-0.5 text-left text-[10px] leading-tight ${getRdvStatutBadgeClass(rdv.statut)}`}
                        >
                          <span className="font-semibold">
                            {rdv.usagerPrenom} {rdv.usagerNom.charAt(0)}.
                          </span>
                          {rdv.typeServiceLibelle && (
                            <span className="block truncate opacity-80">{rdv.typeServiceLibelle}</span>
                          )}
                        </button>
                      ))
                    )}
                  </div>
                )
              })}
            </div>
          ))}
        </div>
      </div>

      {agendaQuery.isLoading && (
        <p className="text-muted-foreground text-sm">{t('loading')}</p>
      )}

      <RdvCreateModal
        open={createOpen}
        onOpenChange={setCreateOpen}
        initialDate={createInitialDate}
        initialTime={createInitialTime}
      />

      <RdvDetailDrawer
        rdvId={selectedRdvId}
        open={drawerOpen}
        onOpenChange={setDrawerOpen}
      />
    </div>
  )
}

export default AgendaPage
