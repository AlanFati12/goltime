import { cn } from '@/lib/utils'

import { NOMBRES_DIAS } from './constantes'
import { diaIsoDeHoy } from './horario'
import type { HorarioDia } from './tipos'

/** Horario semanal de solo lectura; resalta el día de hoy. */
export function HorarioTabla({ dias }: { dias: HorarioDia[] }) {
  const hoy = diaIsoDeHoy()
  return (
    <dl className="flex flex-col text-sm">
      {dias.map((d) => (
        <div
          key={d.diaSemana}
          className={cn(
            'flex justify-between gap-3 rounded-xl px-3 py-2',
            d.diaSemana === hoy && 'bg-volt-claro font-semibold',
          )}
        >
          <dt className={d.diaSemana === hoy ? 'text-primario-oscuro' : 'text-texto-suave'}>
            {NOMBRES_DIAS[d.diaSemana - 1]}
            {d.diaSemana === hoy && ' · hoy'}
          </dt>
          <dd className={d.cerrado ? 'text-texto-suave' : 'cifras font-semibold'}>
            {d.cerrado ? 'Cerrado' : `${d.apertura} – ${d.cierre}`}
          </dd>
        </div>
      ))}
    </dl>
  )
}
