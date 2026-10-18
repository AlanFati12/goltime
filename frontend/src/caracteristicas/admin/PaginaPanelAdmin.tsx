import { Link } from 'react-router-dom'

import { nombreCompleto, useSesion } from '@/caracteristicas/autenticacion/almacenSesion'
import { useResumen } from '@/caracteristicas/estadisticas/consultas'
import { InsigniaEstadoReserva } from '@/caracteristicas/reservas/InsigniaEstadoReserva'
import { useAccionesReservaAdmin } from '@/caracteristicas/reservas/consultas'
import { rangoHorario } from '@/caracteristicas/reservas/utilidades'
import { avisar } from '@/componentes/ui/almacenAvisos'
import { Boton } from '@/componentes/ui/Boton'
import { Cargando } from '@/componentes/ui/Cargando'
import { EncabezadoPagina } from '@/componentes/ui/EncabezadoPagina'
import { EnlaceBoton } from '@/componentes/ui/EnlaceBoton'
import { Tarjeta } from '@/componentes/ui/Tarjeta'
import { mensajeDeError } from '@/lib/api'
import { formatearCOP, formatearFechaHora, hoyEnBogota, sumarDias } from '@/lib/formato'
import { cn } from '@/lib/utils'

import { useReservasDelDia, useSolicitudesPendientes } from './consultasAdmin'

function Indicador({
  titulo,
  valor,
  detalle,
  a,
  destacado,
}: {
  titulo: string
  valor: string
  detalle?: string
  a?: string
  destacado?: boolean
}) {
  const contenido = (
    <Tarjeta
      className={cn(
        'flex h-full flex-col gap-1 p-4 sm:p-5',
        a && 'hover:border-primario transition-colors',
        destacado && 'border-alerta/40 bg-alerta/5',
      )}
    >
      <p className="text-texto-suave text-sm">{titulo}</p>
      <p className="text-xl font-bold whitespace-nowrap sm:text-2xl xl:text-3xl">{valor}</p>
      {detalle && <p className="text-texto-suave text-xs">{detalle}</p>}
    </Tarjeta>
  )
  return a ? <Link to={a}>{contenido}</Link> : contenido
}

function saludo(): string {
  const hora = Number(
    new Intl.DateTimeFormat('es-CO', {
      hour: 'numeric',
      hour12: false,
      timeZone: 'America/Bogota',
    }).format(new Date()),
  )
  return hora < 12 ? 'Buenos días' : hora < 19 ? 'Buenas tardes' : 'Buenas noches'
}

/** Inicio del panel: lo que requiere atención ahora, lo de hoy y un resumen de la semana. */
export function PaginaPanelAdmin() {
  const usuario = useSesion((s) => s.usuario)!
  const hoy = hoyEnBogota()
  const pendientes = useSolicitudesPendientes()
  const delDia = useReservasDelDia(hoy)
  const semana = useResumen({ desde: sumarDias(hoy, -6), hasta: hoy, sedeId: null, canchaId: null })
  const { aprobar } = useAccionesReservaAdmin()

  const totalPendientes = pendientes.data?.totalElementos ?? 0
  const reservasHoy = delDia.data?.contenido ?? []
  const actual = semana.data?.actual

  return (
    <div className="flex flex-col gap-8">
      <EncabezadoPagina
        titulo={`${saludo()}, ${usuario.nombres.split(' ')[0] || nombreCompleto(usuario)}`}
        descripcion="Esto es lo que necesita tu atención hoy."
        acciones={
          <>
            <EnlaceBoton to="/admin/reservas/nueva">Nueva reserva</EnlaceBoton>
            <EnlaceBoton to="/admin/agenda" variante="secundario">
              Ver agenda
            </EnlaceBoton>
          </>
        }
      />

      <div className="grid grid-cols-2 gap-3 sm:gap-4 lg:grid-cols-4">
        <Indicador
          titulo="Por aprobar"
          valor={String(totalPendientes)}
          detalle={totalPendientes > 0 ? 'Solicitudes esperando respuesta' : 'Todo al día'}
          a="/admin/reservas"
          destacado={totalPendientes > 0}
        />
        <Indicador
          titulo="Reservas de hoy"
          valor={String(reservasHoy.length)}
          detalle="Confirmadas"
          a="/admin/agenda"
        />
        <Indicador
          titulo="Ingresos, últimos 7 días"
          valor={actual ? formatearCOP(actual.ingresosReales) : '—'}
          detalle="Solo reservas completadas"
          a="/admin/estadisticas"
        />
        <Indicador
          titulo="Ocupación, últimos 7 días"
          valor={actual ? `${actual.ocupacionPct}%` : '—'}
          detalle={actual ? `${actual.horasJugadas} horas jugadas` : undefined}
          a="/admin/estadisticas"
        />
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        <section aria-labelledby="titulo-pendientes" className="flex flex-col gap-3">
          <div className="flex items-baseline justify-between">
            <h2 id="titulo-pendientes" className="text-lg font-semibold">
              Solicitudes por aprobar
            </h2>
            {totalPendientes > 5 && (
              <Link to="/admin/reservas" className="text-primario text-sm font-medium">
                Ver las {totalPendientes}
              </Link>
            )}
          </div>
          <Tarjeta className="divide-borde divide-y p-0 sm:p-0">
            {pendientes.isPending && <Cargando texto="Cargando…" />}
            {pendientes.data?.contenido.length === 0 && (
              <p className="text-texto-suave p-6 text-center text-sm">
                No hay solicitudes pendientes. ¡Todo al día!
              </p>
            )}
            {pendientes.data?.contenido.map((r) => (
              <div key={r.id} className="flex flex-wrap items-center justify-between gap-3 p-4">
                <div className="min-w-0">
                  <Link to={`/admin/reservas/${r.id}`} className="font-medium hover:underline">
                    {r.cliente.nombre}
                  </Link>
                  <p className="text-texto-suave text-sm">
                    <span className="capitalize">{formatearFechaHora(r.inicio)}</span> ·{' '}
                    {r.cancha.nombre}
                  </p>
                </div>
                <Boton
                  tamano="sm"
                  disabled={aprobar.isPending}
                  onClick={() =>
                    aprobar.mutate(r.id, {
                      onSuccess: () => avisar.exito(`Reserva ${r.codigo} confirmada.`),
                      onError: (e) => avisar.error(mensajeDeError(e)),
                    })
                  }
                >
                  Aprobar
                </Boton>
              </div>
            ))}
          </Tarjeta>
        </section>

        <section aria-labelledby="titulo-hoy" className="flex flex-col gap-3">
          <h2 id="titulo-hoy" className="text-lg font-semibold">
            Hoy en las canchas
          </h2>
          <Tarjeta className="divide-borde divide-y p-0 sm:p-0">
            {delDia.isPending && <Cargando texto="Cargando…" />}
            {delDia.data && reservasHoy.length === 0 && (
              <p className="text-texto-suave p-6 text-center text-sm">
                No hay reservas confirmadas para hoy.
              </p>
            )}
            {reservasHoy.slice(0, 6).map((r) => (
              <Link
                key={r.id}
                to={`/admin/reservas/${r.id}`}
                className="hover:bg-primario-claro/40 flex items-center justify-between gap-3 p-4"
              >
                <div className="min-w-0">
                  <p className="font-medium">{rangoHorario(r.inicio, r.fin)}</p>
                  <p className="text-texto-suave truncate text-sm">
                    {r.cancha.nombre} · {r.cliente.nombre}
                  </p>
                </div>
                <InsigniaEstadoReserva estado={r.estado} />
              </Link>
            ))}
          </Tarjeta>
          {reservasHoy.length > 6 && (
            <Link to="/admin/agenda" className="text-primario text-sm font-medium">
              Ver las {reservasHoy.length} reservas de hoy
            </Link>
          )}
        </section>
      </div>
    </div>
  )
}
