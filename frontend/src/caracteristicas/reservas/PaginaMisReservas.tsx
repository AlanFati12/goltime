import { useState } from 'react'
import { Link } from 'react-router-dom'

import { Alerta } from '@/componentes/ui/Alerta'
import { Boton } from '@/componentes/ui/Boton'
import { Cargando } from '@/componentes/ui/Cargando'
import { EnlaceBoton } from '@/componentes/ui/EnlaceBoton'
import { Tarjeta } from '@/componentes/ui/Tarjeta'
import { mensajeDeError } from '@/lib/api'
import { formatearCOP, formatearFechaHora } from '@/lib/formato'
import { cn } from '@/lib/utils'

import { InsigniaEstadoReserva } from './InsigniaEstadoReserva'
import { useMisReservas } from './consultas'
import type { PestanaReservas } from './tipos'
import { ETIQUETA_PAGO, rangoHorario } from './utilidades'

const PESTANAS: { id: PestanaReservas; texto: string; vacio: string }[] = [
  { id: 'PROXIMAS', texto: 'Próximas', vacio: 'No tienes reservas próximas.' },
  { id: 'HISTORIAL', texto: 'Historial', vacio: 'Aún no has jugado ninguna reserva.' },
  {
    id: 'CANCELADAS',
    texto: 'Canceladas',
    vacio: 'No tienes reservas canceladas, rechazadas ni expiradas.',
  },
]

export function PaginaMisReservas() {
  const [pestana, setPestana] = useState<PestanaReservas>('PROXIMAS')
  const [pagina, setPagina] = useState(0)
  const { data, isPending, isError, error, isFetching } = useMisReservas(pestana, pagina)
  const actual = PESTANAS.find((p) => p.id === pestana)!

  return (
    <div className="mx-auto flex w-full max-w-3xl flex-col gap-5">
      <div className="flex items-center justify-between gap-2">
        <h1 className="text-3xl font-bold">Mis reservas</h1>
        <EnlaceBoton to="/reservar">Reservar</EnlaceBoton>
      </div>

      <div role="tablist" aria-label="Mis reservas" className="border-borde flex gap-1 border-b">
        {PESTANAS.map((p) => (
          <button
            key={p.id}
            role="tab"
            aria-selected={pestana === p.id}
            onClick={() => {
              setPestana(p.id)
              setPagina(0)
            }}
            className={cn(
              'border-b-2 px-4 py-2 text-sm font-medium',
              pestana === p.id
                ? 'border-primario text-primario'
                : 'text-texto-suave hover:text-texto border-transparent',
            )}
          >
            {p.texto}
          </button>
        ))}
      </div>

      {isPending && <Cargando texto="Cargando tus reservas…" />}
      {isError && <Alerta tipo="error">{mensajeDeError(error)}</Alerta>}
      {data?.contenido.length === 0 && (
        <Tarjeta className="text-texto-suave flex flex-col items-center gap-3 py-8 text-center text-sm">
          <p>{actual.vacio}</p>
          {pestana === 'PROXIMAS' && <EnlaceBoton to="/reservar">Reservar una cancha</EnlaceBoton>}
        </Tarjeta>
      )}

      <ul className={cn('flex flex-col gap-3', isFetching && 'opacity-60')}>
        {data?.contenido.map((r) => (
          <li key={r.id}>
            <Link to={`/mis-reservas/${r.id}`} className="block">
              <Tarjeta className="hover:border-primario flex flex-col gap-2 p-4 transition-colors">
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <span className="font-mono text-sm font-medium">{r.codigo}</span>
                  <InsigniaEstadoReserva estado={r.estado} />
                </div>
                <div>
                  <p className="font-semibold">
                    {r.sede.nombre} · {r.cancha.nombre} ({r.cancha.modalidad})
                  </p>
                  <p className="text-sm capitalize">
                    {formatearFechaHora(r.inicio)} · {rangoHorario(r.inicio, r.fin)}
                  </p>
                </div>
                <div className="text-texto-suave flex flex-wrap justify-between gap-2 text-sm">
                  <span>
                    {formatearCOP(r.precioFinal)} · {ETIQUETA_PAGO[r.estadoPago] ?? r.estadoPago}
                  </span>
                  {r.nombreEquipo && <span>Equipo: {r.nombreEquipo}</span>}
                </div>
              </Tarjeta>
            </Link>
          </li>
        ))}
      </ul>

      {data && data.totalPaginas > 1 && (
        <div className="flex items-center justify-between text-sm">
          <span className="text-texto-suave">
            Página {data.pagina + 1} de {data.totalPaginas}
          </span>
          <div className="flex gap-2">
            <Boton
              tamano="sm"
              variante="secundario"
              disabled={pagina === 0}
              onClick={() => setPagina((p) => p - 1)}
            >
              Anterior
            </Boton>
            <Boton
              tamano="sm"
              variante="secundario"
              disabled={pagina + 1 >= data.totalPaginas}
              onClick={() => setPagina((p) => p + 1)}
            >
              Siguiente
            </Boton>
          </div>
        </div>
      )}
    </div>
  )
}
