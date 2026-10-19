import { Link } from 'react-router-dom'

import { cn } from '@/lib/utils'

import type { Partido } from './tipos'
import { agruparPor, marcador, nombreLado, textoPenales } from './utilidades'

function Cruce({ p, ruta }: { p: Partido; ruta: (p: Partido) => string }) {
  const filas = [
    { lado: p.local, goles: p.golesLocal },
    { lado: p.visitante, goles: p.golesVisitante },
  ]
  return (
    <Link
      to={ruta(p)}
      className="border-borde bg-superficie hover:border-volt flex w-56 flex-col overflow-hidden rounded-2xl border text-sm transition-[border-color,transform] duration-200 hover:-translate-y-0.5"
      aria-label={`${nombreLado(p.local)} contra ${nombreLado(p.visitante)}: ${marcador(p)}`}
    >
      {filas.map((f, i) => (
        <span
          key={i}
          className={cn(
            'flex items-center justify-between gap-2 px-3 py-1.5',
            i === 0 && 'border-borde border-b',
            f.lado?.inscripcionId &&
              p.ganadorInscripcionId === f.lado.inscripcionId &&
              'bg-noche [&>span:last-child]:text-volt-vivo font-bold text-white',
          )}
        >
          <span className="truncate">
            {p.descanso && i === 1 ? 'Descansa' : nombreLado(f.lado)}
          </span>
          <span className="font-titulo text-base font-extrabold tabular-nums">{f.goles ?? ''}</span>
        </span>
      ))}
      {textoPenales(p) && (
        <span className="text-texto-suave px-3 pb-1 text-xs">{textoPenales(p)}</span>
      )}
    </Link>
  )
}

/** Cuadro de eliminación: una columna por ronda, de izquierda a derecha, y el tercer puesto aparte. */
export function Llaves({ partidos, ruta }: { partidos: Partido[]; ruta: (p: Partido) => string }) {
  const llaves = partidos.filter((p) => p.tipoFase === 'ELIMINATORIA')
  if (llaves.length === 0) return null
  const tercero = llaves.filter((p) => p.nombreRonda === 'Tercer puesto')
  const rondas = [
    ...agruparPor(
      llaves.filter((p) => p.nombreRonda !== 'Tercer puesto'),
      (p) => p.ronda,
    ).entries(),
  ].sort((a, b) => a[0] - b[0])
  return (
    <div className="flex flex-col gap-4">
      <div className="flex gap-6 overflow-x-auto pb-2">
        {rondas.map(([ronda, lista]) => (
          <section
            key={ronda}
            className="flex shrink-0 flex-col gap-2"
            aria-label={lista[0].nombreRonda ?? `Ronda ${ronda}`}
          >
            <h3 className="text-center text-lg font-extrabold uppercase">{lista[0].nombreRonda}</h3>
            <div className="flex flex-1 flex-col justify-around gap-4">
              {lista
                .sort((a, b) => a.numeroLlave - b.numeroLlave)
                .map((p) => (
                  <Cruce key={p.id} p={p} ruta={ruta} />
                ))}
            </div>
          </section>
        ))}
      </div>
      {tercero.map((p) => (
        <section key={p.id} className="flex flex-col gap-2">
          <h3 className="text-lg font-extrabold uppercase">Tercer puesto</h3>
          <Cruce p={p} ruta={ruta} />
        </section>
      ))}
    </div>
  )
}
