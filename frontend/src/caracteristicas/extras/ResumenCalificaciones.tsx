import { Tarjeta } from '@/componentes/ui/Tarjeta'
import { formatearDia, fechaEnBogota } from '@/lib/formato'

import { useCalificacionesCancha } from './consultas'
import { Estrellas } from './Estrellas'

/** Promedio, distribución y últimas opiniones de una cancha. */
export function ResumenCalificaciones({ canchaId }: { canchaId: number }) {
  const { data } = useCalificacionesCancha(canchaId)
  if (!data) return null
  if (data.total === 0) {
    return <p className="text-texto-suave text-sm">Aún no tiene calificaciones.</p>
  }
  return (
    <Tarjeta className="flex flex-col gap-3">
      <div className="flex flex-wrap items-center gap-3">
        <span className="text-3xl font-bold">{data.promedio.toFixed(1)}</span>
        <Estrellas valor={data.promedio} className="text-xl" />
        <span className="text-texto-suave text-sm">
          {data.total} {data.total === 1 ? 'calificación' : 'calificaciones'}
        </span>
      </div>
      <ul className="flex flex-col gap-1 text-xs" aria-label="Distribución de calificaciones">
        {[5, 4, 3, 2, 1].map((n) => (
          <li key={n} className="flex items-center gap-2">
            <span className="w-3">{n}</span>
            <span className="bg-borde h-2 flex-1 overflow-hidden rounded-full" aria-hidden="true">
              <span
                className="bg-acento block h-full"
                style={{ width: `${(100 * data.distribucion[n - 1]) / data.total}%` }}
              />
            </span>
            <span className="w-6 text-right">{data.distribucion[n - 1]}</span>
          </li>
        ))}
      </ul>
      {data.ultimas.length > 0 && (
        <ul className="flex flex-col gap-2 text-sm">
          {data.ultimas.map((o, i) => (
            <li key={i} className="border-borde border-t pt-2">
              <Estrellas valor={o.estrellas} /> <span className="font-medium">{o.autor}</span>
              <span className="text-texto-suave"> · {formatearDia(fechaEnBogota(o.fecha))}</span>
              <p>{o.comentario}</p>
            </li>
          ))}
        </ul>
      )}
    </Tarjeta>
  )
}
