import { CalendarX, ChevronRight, Plus } from 'lucide-react'
import { useState } from 'react'
import { Link } from 'react-router-dom'

import { Alerta } from '@/componentes/ui/Alerta'
import { Cargando } from '@/componentes/ui/Cargando'
import { EncabezadoPagina } from '@/componentes/ui/EncabezadoPagina'
import { EnlaceBoton } from '@/componentes/ui/EnlaceBoton'
import { EstadoVacio } from '@/componentes/ui/EstadoVacio'
import { Pestanas } from '@/componentes/ui/Pestanas'
import { Tarjeta } from '@/componentes/ui/Tarjeta'
import { Paginacion } from '@/componentes/ui/Paginacion'
import { mensajeDeError } from '@/lib/api'
import { formatearCOP } from '@/lib/formato'
import { cn } from '@/lib/utils'

import { BloqueFecha } from './BloqueFecha'
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
    <div className="flex flex-col gap-6">
      <EncabezadoPagina
        titulo="Mis reservas"
        descripcion="Consulta, reprograma o cancela tus partidos."
        acciones={
          <EnlaceBoton to="/reservar">
            <Plus aria-hidden />
            Nueva reserva
          </EnlaceBoton>
        }
      />

      <Pestanas
        etiqueta="Mis reservas"
        pestanas={PESTANAS}
        actual={pestana}
        alCambiar={(id) => {
          setPestana(id)
          setPagina(0)
        }}
      />

      {isPending && <Cargando texto="Cargando tus reservas…" variante="lista" />}
      {isError && <Alerta tipo="error">{mensajeDeError(error)}</Alerta>}
      {data?.contenido.length === 0 && (
        <EstadoVacio
          icono={CalendarX}
          titulo={actual.vacio}
          accion={
            pestana === 'PROXIMAS' && (
              <EnlaceBoton to="/reservar" tamano="sm">
                Reservar una cancha
              </EnlaceBoton>
            )
          }
        />
      )}

      <ul className={cn('flex flex-col gap-3', isFetching && 'opacity-60')}>
        {data?.contenido.map((r) => (
          <li key={r.id}>
            <Link to={`/mis-reservas/${r.id}`} className="group block">
              <Tarjeta className="group-hover:border-primario/40 flex items-center gap-4 p-4 transition-colors duration-200 sm:p-5">
                <BloqueFecha fecha={r.inicio} />
                <div className="flex min-w-0 flex-1 flex-col gap-1">
                  <div className="flex flex-wrap items-center gap-2">
                    <p className="font-semibold">
                      {r.cancha.nombre}{' '}
                      <span className="text-texto-suave font-normal">· {r.cancha.modalidad}</span>
                    </p>
                    <InsigniaEstadoReserva estado={r.estado} />
                  </div>
                  <p className="text-texto-suave text-sm">
                    {r.sede.nombre} · {rangoHorario(r.inicio, r.fin)}
                  </p>
                  <p className="text-texto-suave text-xs">
                    <span className="cifras font-semibold tracking-wide">{r.codigo}</span> ·{' '}
                    {formatearCOP(r.precioFinal)} · {ETIQUETA_PAGO[r.estadoPago] ?? r.estadoPago}
                    {r.nombreEquipo && ` · ${r.nombreEquipo}`}
                  </p>
                </div>
                <ChevronRight
                  className="text-texto-suave size-5 shrink-0 transition-transform group-hover:translate-x-0.5"
                  aria-hidden
                />
              </Tarjeta>
            </Link>
          </li>
        ))}
      </ul>

      {data && (
        <Paginacion pagina={data.pagina} totalPaginas={data.totalPaginas} alCambiar={setPagina} />
      )}
    </div>
  )
}
