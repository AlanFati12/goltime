import { NOMBRES_DIAS } from './constantes'
import type { HorarioDia } from './tipos'

/** Horario semanal de solo lectura. */
export function HorarioTabla({ dias }: { dias: HorarioDia[] }) {
  return (
    <dl className="grid grid-cols-[7rem_1fr] gap-x-3 gap-y-1 text-sm">
      {dias.map((d) => (
        <div key={d.diaSemana} className="contents">
          <dt className="text-texto-suave">{NOMBRES_DIAS[d.diaSemana - 1]}</dt>
          <dd>{d.cerrado ? 'Cerrado' : `${d.apertura} – ${d.cierre}`}</dd>
        </div>
      ))}
    </dl>
  )
}
