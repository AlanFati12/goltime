import { NOMBRES_DIAS } from './constantes'
import type { HorarioDia } from './tipos'

/** Horario semanal de solo lectura. */
export function HorarioTabla({ dias }: { dias: HorarioDia[] }) {
  return (
    <dl className="divide-borde/70 divide-y text-sm">
      {dias.map((d) => (
        <div key={d.diaSemana} className="flex justify-between gap-3 py-1.5">
          <dt className="text-texto-suave">{NOMBRES_DIAS[d.diaSemana - 1]}</dt>
          <dd className={d.cerrado ? 'text-texto-suave' : 'font-medium tabular-nums'}>
            {d.cerrado ? 'Cerrado' : `${d.apertura} – ${d.cierre}`}
          </dd>
        </div>
      ))}
    </dl>
  )
}
