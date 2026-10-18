import { useState } from 'react'

import { Boton } from '@/componentes/ui/Boton'
import { cn } from '@/lib/utils'

import { HORAS, NOMBRES_DIAS } from './constantes'
import type { HorarioDia } from './tipos'

const porDefecto = (dia: number): HorarioDia => ({
  diaSemana: dia,
  cerrado: false,
  apertura: '08:00',
  cierre: dia === 7 ? '20:00' : '23:00',
})

/** Completa los 7 días: los que no vienen definidos se muestran como "sin definir" para que el admin los complete. */
function normalizar(dias: HorarioDia[]): HorarioDia[] {
  return NOMBRES_DIAS.map((_, i) => dias.find((d) => d.diaSemana === i + 1) ?? porDefecto(i + 1))
}

interface PropsEditor {
  dias: HorarioDia[]
  guardando?: boolean
  textoGuardar?: string
  alGuardar: (dias: HorarioDia[]) => void
  /** Solo en el horario propio de una cancha: vuelve a usar el de la sede. */
  alQuitar?: () => void
}

/** Editor del horario semanal: por día, abierto con apertura y cierre (hasta 24:00) o cerrado. */
export function EditorHorario({
  dias,
  guardando,
  textoGuardar = 'Guardar horario',
  alGuardar,
  alQuitar,
}: PropsEditor) {
  const [filas, setFilas] = useState<HorarioDia[]>(() => normalizar(dias))

  const cambiar = (dia: number, parcial: Partial<HorarioDia>) =>
    setFilas((f) => f.map((x) => (x.diaSemana === dia ? { ...x, ...parcial } : x)))

  const invalida = (f: HorarioDia) => !f.cerrado && (f.apertura ?? '') >= (f.cierre ?? '')
  const hayError = filas.some(invalida)

  return (
    <div className="flex flex-col gap-3">
      <ul className="flex flex-col gap-2">
        {filas.map((f) => (
          <li
            key={f.diaSemana}
            className="grid grid-cols-[6rem_1fr] items-center gap-2 sm:grid-cols-[7rem_auto_1fr]"
          >
            <span className="text-sm font-medium">{NOMBRES_DIAS[f.diaSemana - 1]}</span>
            <label className="flex items-center gap-2 text-sm">
              <input
                type="checkbox"
                className="accent-primario size-4"
                checked={f.cerrado}
                onChange={(e) => cambiar(f.diaSemana, { cerrado: e.target.checked })}
              />
              Cerrado
            </label>
            <div className={cn('flex items-center gap-2 text-sm', f.cerrado && 'invisible')}>
              <select
                aria-label={`Apertura ${NOMBRES_DIAS[f.diaSemana - 1]}`}
                value={f.apertura ?? '08:00'}
                onChange={(e) => cambiar(f.diaSemana, { apertura: e.target.value })}
                className="border-borde bg-superficie rounded-boton border px-2 py-1"
              >
                {HORAS.slice(0, 48).map((h) => (
                  <option key={h}>{h}</option>
                ))}
              </select>
              <span>a</span>
              <select
                aria-label={`Cierre ${NOMBRES_DIAS[f.diaSemana - 1]}`}
                value={f.cierre ?? '23:00'}
                onChange={(e) => cambiar(f.diaSemana, { cierre: e.target.value })}
                className="border-borde bg-superficie rounded-boton border px-2 py-1"
              >
                {HORAS.slice(1).map((h) => (
                  <option key={h}>{h}</option>
                ))}
              </select>
              {invalida(f) && (
                <span className="text-peligro text-xs">
                  El cierre debe ser posterior a la apertura
                </span>
              )}
            </div>
          </li>
        ))}
      </ul>
      <div className="flex gap-2">
        <Boton
          disabled={hayError || guardando}
          onClick={() =>
            alGuardar(
              filas.map((f) =>
                f.cerrado
                  ? { diaSemana: f.diaSemana, cerrado: true, apertura: null, cierre: null }
                  : f,
              ),
            )
          }
        >
          {guardando ? 'Guardando…' : textoGuardar}
        </Boton>
        {alQuitar && (
          <Boton variante="secundario" onClick={alQuitar} disabled={guardando}>
            Usar el horario de la sede
          </Boton>
        )}
      </div>
    </div>
  )
}
