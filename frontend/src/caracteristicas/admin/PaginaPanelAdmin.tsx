import {
  CalendarCheck,
  CalendarDays,
  ChartNoAxesColumn,
  Hourglass,
  Plus,
  Wallet,
  ChevronRight,
  CircleCheck,
  Coffee,
} from 'lucide-react'
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
import { Estadistica } from '@/componentes/ui/Estadistica'
import { Tarjeta } from '@/componentes/ui/Tarjeta'
import { mensajeDeError } from '@/lib/api'
import { formatearCOP, formatearFechaHora, hoyEnBogota, sumarDias } from '@/lib/formato'

import { useReservasDelDia, useSolicitudesPendientes } from './consultasAdmin'

function Indicador({ a, ...props }: Parameters<typeof Estadistica>[0] & { a: string }) {
  return (
    <Link to={a} className="group">
      <Estadistica
        {...props}
        className="group-hover:border-texto-suave/30 h-full transition-colors group-hover:shadow-md"
      />
    </Link>
  )
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
            <EnlaceBoton to="/admin/agenda" variante="secundario">
              <CalendarDays aria-hidden />
              Ver agenda
            </EnlaceBoton>
            <EnlaceBoton to="/admin/reservas/nueva">
              <Plus aria-hidden />
              Nueva reserva
            </EnlaceBoton>
          </>
        }
      />

      <div className="grid grid-cols-2 gap-3 sm:gap-4 lg:grid-cols-4">
        <Indicador
          etiqueta="Por aprobar"
          icono={Hourglass}
          valor={totalPendientes}
          nota={totalPendientes > 0 ? 'Solicitudes esperando respuesta' : 'Todo al día'}
          a="/admin/reservas"
          destacado={totalPendientes > 0}
        />
        <Indicador
          etiqueta="Reservas de hoy"
          icono={CalendarCheck}
          valor={reservasHoy.length}
          nota="Confirmadas"
          a="/admin/agenda"
        />
        <Indicador
          etiqueta="Ingresos · 7 días"
          icono={Wallet}
          valor={actual ? formatearCOP(actual.ingresosReales) : '—'}
          nota="Solo reservas completadas"
          a="/admin/estadisticas"
        />
        <Indicador
          etiqueta="Ocupación · 7 días"
          icono={ChartNoAxesColumn}
          valor={actual ? `${actual.ocupacionPct}%` : '—'}
          nota={actual ? `${actual.horasJugadas} horas jugadas` : undefined}
          a="/admin/estadisticas"
        />
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        <section aria-labelledby="titulo-pendientes" className="flex flex-col gap-3">
          <div className="flex items-baseline justify-between">
            <h2 id="titulo-pendientes" className="text-base font-semibold">
              Solicitudes por aprobar
            </h2>
            {totalPendientes > 5 && (
              <Link to="/admin/reservas" className="enlace text-sm">
                Ver las {totalPendientes}
              </Link>
            )}
          </div>
          <Tarjeta className="divide-borde divide-y p-0 sm:p-0">
            {pendientes.isPending && <Cargando texto="Cargando…" />}
            {pendientes.data?.contenido.length === 0 && (
              <div className="text-texto-suave flex flex-col items-center gap-2 px-6 py-10 text-center text-sm">
                <CircleCheck className="text-primario size-6" aria-hidden />
                No hay solicitudes pendientes. ¡Todo al día!
              </div>
            )}
            {pendientes.data?.contenido.map((r) => (
              <div
                key={r.id}
                className="flex flex-wrap items-center justify-between gap-3 px-5 py-4"
              >
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
          <h2 id="titulo-hoy" className="text-base font-semibold">
            Hoy en las canchas
          </h2>
          <Tarjeta className="divide-borde divide-y p-0 sm:p-0">
            {delDia.isPending && <Cargando texto="Cargando…" />}
            {delDia.data && reservasHoy.length === 0 && (
              <div className="text-texto-suave flex flex-col items-center gap-2 px-6 py-10 text-center text-sm">
                <Coffee className="size-6" aria-hidden />
                No hay reservas confirmadas para hoy.
              </div>
            )}
            {reservasHoy.slice(0, 6).map((r) => (
              <Link
                key={r.id}
                to={`/admin/reservas/${r.id}`}
                className="hover:bg-tenue/60 group flex items-center justify-between gap-3 px-5 py-4"
              >
                <div className="min-w-0">
                  <p className="font-medium">{rangoHorario(r.inicio, r.fin)}</p>
                  <p className="text-texto-suave truncate text-sm">
                    {r.cancha.nombre} · {r.cliente.nombre}
                  </p>
                </div>
                <span className="flex items-center gap-2">
                  <InsigniaEstadoReserva estado={r.estado} />
                  <ChevronRight className="text-texto-suave size-4" aria-hidden />
                </span>
              </Link>
            ))}
          </Tarjeta>
          {reservasHoy.length > 6 && (
            <Link to="/admin/agenda" className="enlace text-sm">
              Ver las {reservasHoy.length} reservas de hoy
            </Link>
          )}
        </section>
      </div>
    </div>
  )
}
