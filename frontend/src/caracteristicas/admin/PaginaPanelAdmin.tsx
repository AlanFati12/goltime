import {
  AlarmClock,
  CalendarCheck,
  CalendarDays,
  ChevronRight,
  CircleCheck,
  Coffee,
  HandCoins,
  Plus,
  Trophy,
  Undo2,
  UserPlus,
  Wallet,
  type LucideIcon,
} from 'lucide-react'
import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'

import { nombreCompleto, useSesion } from '@/caracteristicas/autenticacion/almacenSesion'
import { useResumen } from '@/caracteristicas/estadisticas/consultas'
import { InsigniaEstadoReserva } from '@/caracteristicas/reservas/InsigniaEstadoReserva'
import { useAccionesReservaAdmin } from '@/caracteristicas/reservas/consultas'
import { rangoHorario } from '@/caracteristicas/reservas/utilidades'
import { avisar } from '@/componentes/ui/almacenAvisos'
import { Boton } from '@/componentes/ui/Boton'
import { Cargando } from '@/componentes/ui/Cargando'
import { EnlaceBoton } from '@/componentes/ui/EnlaceBoton'
import { Estadistica } from '@/componentes/ui/Estadistica'
import { Insignia } from '@/componentes/ui/Insignia'
import { Tarjeta, TituloTarjeta } from '@/componentes/ui/Tarjeta'
import { mensajeDeError } from '@/lib/api'
import {
  formatearCOP,
  formatearDia,
  formatearFechaHora,
  formatearHora,
  hoyEnBogota,
} from '@/lib/formato'
import { cn } from '@/lib/utils'

import {
  useReservasDelDia,
  useSolicitudesPendientes,
  useTablero,
  type Tablero,
} from './consultasAdmin'
import { ImagenSitio } from '@/componentes/ui/ImagenSitio'

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

const fechaLarga = () =>
  new Intl.DateTimeFormat('es-CO', {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
    timeZone: 'America/Bogota',
  }).format(new Date())

function Indicador({ a, ...props }: Parameters<typeof Estadistica>[0] & { a: string }) {
  return (
    <Link to={a} className="group">
      <Estadistica
        {...props}
        className="group-hover:border-texto-suave/30 group-hover:border-primario/40 h-full transition-colors"
      />
    </Link>
  )
}

/** Aviso corto que pide una acción; solo aparece si hay algo que hacer. */
function Pendiente({
  a,
  icono: Icono,
  tono,
  children,
}: {
  a: string
  icono: LucideIcon
  tono: 'ambar' | 'rojo' | 'azul'
  children: ReactNode
}) {
  return (
    <Link
      to={a}
      className={cn(
        'presionable inline-flex items-center gap-2 rounded-full px-4 py-2 text-sm font-semibold backdrop-blur-md transition-colors',
        tono === 'ambar' && 'bg-amber-400/15 text-amber-200 hover:bg-amber-400/25',
        tono === 'rojo' && 'bg-red-500/15 text-red-200 hover:bg-red-500/25',
        tono === 'azul' && 'bg-blue-400/15 text-blue-200 hover:bg-blue-400/25',
      )}
    >
      <Icono className="size-4" aria-hidden />
      {children}
      <ChevronRight className="size-4 opacity-60" aria-hidden />
    </Link>
  )
}

function Atencion({ t, pendientes }: { t: Tablero; pendientes: number }) {
  const avisos: ReactNode[] = []
  if (pendientes > 0)
    avisos.push(
      <Pendiente key="r" a="/admin/reservas" icono={AlarmClock} tono="ambar">
        {pendientes} {pendientes === 1 ? 'reserva por aprobar' : 'reservas por aprobar'}
        {t.pendientesPorVencer > 0 && ` · ${t.pendientesPorVencer} vencen pronto`}
      </Pendiente>,
    )
  if (t.torneos.solicitudesPendientes > 0)
    avisos.push(
      <Pendiente key="s" a="/admin/torneos" icono={Trophy} tono="ambar">
        {t.torneos.solicitudesPendientes === 1
          ? '1 solicitud de inscripción a torneos'
          : `${t.torneos.solicitudesPendientes} solicitudes de inscripción a torneos`}
      </Pendiente>,
    )
  if (t.torneos.partidosSinResultado > 0)
    avisos.push(
      <Pendiente key="p" a="/admin/torneos" icono={Trophy} tono="azul">
        {t.torneos.partidosSinResultado === 1
          ? '1 partido jugado sin resultado'
          : `${t.torneos.partidosSinResultado} partidos jugados sin resultado`}
      </Pendiente>,
    )
  if (t.cobros.reembolsosPendientes > 0)
    avisos.push(
      <Pendiente key="d" a="/admin/reservas" icono={Undo2} tono="rojo">
        {t.cobros.reembolsosPendientes} reembolsos por hacer (
        {formatearCOP(t.cobros.montoReembolsos)})
      </Pendiente>,
    )
  if (avisos.length === 0) {
    return (
      <div className="text-volt-vivo flex items-center gap-2 text-sm font-semibold">
        <CircleCheck className="size-4" aria-hidden />
        Nada urgente: no hay solicitudes, reembolsos ni partidos pendientes.
      </div>
    )
  }
  return <div className="flex flex-wrap gap-2">{avisos}</div>
}

function CanchasAhora({ canchas }: { canchas: Tablero['canchas'] }) {
  return (
    <Tarjeta className="flex flex-col p-0 sm:p-0">
      <TituloTarjeta
        className="mb-0 px-5 pt-5 pb-3"
        titulo="Canchas ahora"
        acciones={
          <Link to="/admin/agenda" className="enlace text-sm">
            Agenda
          </Link>
        }
      />
      {canchas.length === 0 && (
        <p className="text-texto-suave px-5 pb-5 text-sm">
          No hay canchas activas. Crea una sede y sus canchas.
        </p>
      )}
      <ul className="divide-borde divide-y">
        {canchas.map((c) => (
          <li key={c.canchaId} className="flex items-center gap-3 px-5 py-3">
            <span className="relative flex size-2.5 shrink-0" aria-hidden>
              {c.enUso && c.estado !== 'MANTENIMIENTO' && (
                <span className="bg-peligro absolute inset-0 animate-ping rounded-full opacity-50 motion-reduce:animate-none" />
              )}
              <span
                className={cn(
                  'relative size-2.5 rounded-full',
                  c.estado === 'MANTENIMIENTO'
                    ? 'bg-amber-500'
                    : c.enUso
                      ? 'bg-peligro'
                      : 'bg-volt',
                )}
              />
            </span>
            <div className="min-w-0 flex-1">
              <p className="truncate text-sm font-medium">{c.cancha}</p>
              <p className="text-texto-suave truncate text-xs">{c.sede}</p>
            </div>
            <div className="text-right text-xs">
              {c.estado === 'MANTENIMIENTO' ? (
                <Insignia tono="ambar">Mantenimiento</Insignia>
              ) : c.enUso ? (
                <span className="text-peligro font-medium">
                  En uso hasta {formatearHora(c.ocupadaHasta!)}
                </span>
              ) : (
                <span className="text-primario font-medium">Libre</span>
              )}
              <p className="text-texto-suave mt-0.5">
                {c.proximoInicio
                  ? `Próxima: ${fechaCorta(c.proximoInicio)}`
                  : 'Sin próximas reservas'}
              </p>
            </div>
          </li>
        ))}
      </ul>
    </Tarjeta>
  )
}

/** "hoy 18:00", "mañana 07:00" o "mar 21 oct 18:00". */
function fechaCorta(iso: string): string {
  const dia = new Intl.DateTimeFormat('en-CA', { timeZone: 'America/Bogota' }).format(new Date(iso))
  const hoy = hoyEnBogota()
  const hora = formatearHora(iso)
  if (dia === hoy) return `hoy ${hora}`
  const manana = new Date(`${hoy}T12:00:00`)
  manana.setDate(manana.getDate() + 1)
  if (dia === manana.toISOString().slice(0, 10)) return `mañana ${hora}`
  return formatearFechaHora(iso)
}

function ProximosDias({ dias }: { dias: Tablero['proximosDias'] }) {
  const maximo = Math.max(1, ...dias.map((d) => d.reservas))
  return (
    <Tarjeta>
      <TituloTarjeta titulo="Próximos 7 días" descripcion="Reservas activas por día." />
      <ul className="grid grid-cols-7 items-end gap-2" aria-label="Reservas por día">
        {dias.map((d) => {
          const confirmadas = d.reservas - d.pendientes
          return (
            <li key={d.fecha} className="flex flex-col items-center gap-1.5">
              <span className="text-xs font-semibold tabular-nums">{d.reservas}</span>
              <div
                className="bg-tenue flex h-32 w-full max-w-10 flex-col justify-end overflow-hidden rounded-full"
                title={`${formatearDia(d.fecha)}: ${confirmadas} confirmadas, ${d.pendientes} pendientes`}
              >
                <div
                  className="bg-amber-400"
                  style={{ height: `${(100 * d.pendientes) / maximo}%` }}
                />
                <div className="bg-volt" style={{ height: `${(100 * confirmadas) / maximo}%` }} />
              </div>
              <span className="text-texto-suave text-[0.7rem] capitalize">
                {formatearDia(d.fecha).split(' ')[0]}
              </span>
            </li>
          )
        })}
      </ul>
      <div className="text-texto-suave mt-3 flex gap-4 text-xs" aria-hidden>
        <span className="flex items-center gap-1.5">
          <span className="bg-volt size-2.5 rounded-full" /> Confirmadas
        </span>
        <span className="flex items-center gap-1.5">
          <span className="size-2.5 rounded-full bg-amber-400" /> Pendientes
        </span>
      </div>
    </Tarjeta>
  )
}

function Torneos({ t }: { t: Tablero['torneos'] }) {
  const filas = [
    { texto: 'En curso', valor: t.enCurso },
    { texto: 'Con inscripciones abiertas', valor: t.conInscripcionesAbiertas },
    { texto: 'Solicitudes de inscripción', valor: t.solicitudesPendientes, alerta: true },
    { texto: 'Partidos sin programar', valor: t.partidosSinProgramar, alerta: true },
    { texto: 'Partidos jugados sin resultado', valor: t.partidosSinResultado, alerta: true },
  ]
  return (
    <Tarjeta>
      <TituloTarjeta
        titulo="Torneos"
        acciones={
          <Link to="/admin/torneos" className="enlace text-sm">
            Ver torneos
          </Link>
        }
      />
      <dl className="divide-borde/70 flex flex-col divide-y text-sm">
        {filas.map((f) => (
          <div key={f.texto} className="flex items-center justify-between py-2">
            <dt className="text-texto-suave">{f.texto}</dt>
            <dd
              className={cn('font-semibold tabular-nums', f.alerta && f.valor > 0 && 'text-alerta')}
            >
              {f.valor}
            </dd>
          </div>
        ))}
      </dl>
    </Tarjeta>
  )
}

function Cobros({ c }: { c: Tablero['cobros'] }) {
  return (
    <Tarjeta>
      <TituloTarjeta
        titulo="Saldos por cobrar"
        descripcion="Reservas ya jugadas que no se pagaron completas."
      />
      {c.mayoresSaldos.length === 0 ? (
        <p className="text-texto-suave flex items-center gap-2 text-sm">
          <CircleCheck className="text-primario size-4" aria-hidden />
          Todas las reservas jugadas están pagadas.
        </p>
      ) : (
        <ul className="divide-borde/70 flex flex-col divide-y text-sm">
          {c.mayoresSaldos.map((r) => (
            <li key={r.reservaId}>
              <Link
                to={`/admin/reservas/${r.reservaId}`}
                className="hover:bg-tenue/60 -mx-2 flex items-center justify-between gap-3 rounded-md px-2 py-2"
              >
                <span className="min-w-0">
                  <span className="block truncate font-medium">{r.cliente ?? 'Sin cliente'}</span>
                  <span className="text-texto-suave block text-xs capitalize">
                    {r.codigo} · {formatearFechaHora(r.inicio)}
                  </span>
                </span>
                <span className="text-alerta font-semibold tabular-nums">
                  {formatearCOP(r.saldo)}
                </span>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </Tarjeta>
  )
}

/** Inicio del panel: lo que requiere atención, el día de hoy, las canchas ahora, la semana y los pendientes. */
export function PaginaPanelAdmin() {
  const usuario = useSesion((s) => s.usuario)!
  const hoy = hoyEnBogota()
  const tablero = useTablero()
  const pendientes = useSolicitudesPendientes()
  const delDia = useReservasDelDia(hoy)
  const mes = useResumen({
    desde: `${hoy.slice(0, 8)}01`,
    hasta: hoy,
    sedeId: null,
    canchaId: null,
  })
  const { aprobar } = useAccionesReservaAdmin()

  const totalPendientes = pendientes.data?.totalElementos ?? 0
  const reservasHoy = delDia.data?.contenido ?? []
  const t = tablero.data
  const ingresosMes = mes.data?.actual.ingresosReales
  const variacionMes = mes.data?.variaciones.ingresosReales

  return (
    <div className="flex flex-col gap-6">
      <header className="bg-noche relative isolate flex flex-col gap-6 overflow-hidden rounded-[2rem] p-6 text-white sm:p-9">
        <ImagenSitio
          src="/img/hero-cancha-nocturna.webp"
          alt=""
          prioridad
          className="absolute inset-0 -z-10 size-full object-cover opacity-30"
        />
        <div className="from-noche via-noche/85 absolute inset-0 -z-10 bg-gradient-to-r to-transparent" />
        <div className="flex flex-wrap items-end justify-between gap-6">
          <div>
            <p className="text-volt-vivo font-semibold first-letter:uppercase">{fechaLarga()}</p>
            <h1 className="mt-1 text-[clamp(2.4rem,5vw,4rem)] leading-[0.9] font-extrabold uppercase">
              {saludo()}, {usuario.nombres.split(' ')[0] || nombreCompleto(usuario)}
            </h1>
          </div>
          <div className="flex flex-wrap gap-2">
            <EnlaceBoton to="/admin/agenda" variante="claro">
              <CalendarDays aria-hidden />
              Ver agenda
            </EnlaceBoton>
            <EnlaceBoton to="/admin/reservas/nueva" variante="volt">
              <Plus aria-hidden />
              Nueva reserva
            </EnlaceBoton>
          </div>
        </div>
        {t && <Atencion t={t} pendientes={totalPendientes} />}
      </header>

      {tablero.isPending && <Cargando texto="Cargando el tablero…" />}

      {t && (
        <div className="grid grid-cols-2 gap-3 sm:gap-4 lg:grid-cols-4">
          <Indicador
            etiqueta="Reservas de hoy"
            icono={CalendarCheck}
            valor={t.hoy.confirmadas + t.hoy.completadas}
            nota={`${t.hoy.pendientes} pendientes · ${t.hoy.horasReservadas.toFixed(1)} h reservadas${t.hoy.partidosTorneo ? ` · ${t.hoy.partidosTorneo} de torneo` : ''}`}
            a="/admin/agenda"
          />
          <Indicador
            etiqueta="Recaudado hoy"
            icono={HandCoins}
            valor={formatearCOP(t.hoy.recaudado)}
            nota={`De ${formatearCOP(t.hoy.valorReservado)} en reservas de hoy`}
            a="/admin/reservas"
          />
          <Indicador
            etiqueta="Ingresos del mes"
            icono={Wallet}
            valor={ingresosMes != null ? formatearCOP(ingresosMes) : '—'}
            nota={
              variacionMes != null
                ? `${variacionMes >= 0 ? '+' : ''}${variacionMes}% vs. los días anteriores del mismo largo`
                : 'Solo reservas completadas'
            }
            a="/admin/estadisticas"
          />
          <Indicador
            etiqueta="Por cobrar"
            icono={Wallet}
            valor={formatearCOP(t.cobros.saldoPendiente)}
            nota={`${t.cobros.reservasConSaldo} reservas jugadas con saldo`}
            destacado={t.cobros.saldoPendiente > 0}
            a="/admin/reservas"
          />
        </div>
      )}

      <div className="grid items-start gap-6 lg:grid-cols-[minmax(0,1fr)_22rem]">
        <div className="flex flex-col gap-6">
          <section aria-labelledby="titulo-pendientes" className="flex flex-col gap-3">
            <div className="flex items-baseline justify-between">
              <h2 id="titulo-pendientes" className="text-2xl font-extrabold uppercase">
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
                <div className="text-texto-suave flex flex-col items-center gap-2 px-6 py-8 text-center text-sm">
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
                    <Link
                      to={`/admin/reservas/${r.id}`}
                      className="hover:text-primario font-medium"
                    >
                      {r.cliente.nombre}
                    </Link>
                    <p className="text-texto-suave text-sm">
                      <span className="capitalize">{formatearFechaHora(r.inicio)}</span> ·{' '}
                      {r.cancha.nombre} · {formatearCOP(r.precioFinal)}
                    </p>
                    {r.expiraEn && (
                      <p className="text-alerta text-xs">Vence {fechaCorta(r.expiraEn)}</p>
                    )}
                  </div>
                  <Boton
                    tamano="sm"
                    variante="volt"
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
            <h2 id="titulo-hoy" className="text-2xl font-extrabold uppercase">
              Hoy en las canchas
            </h2>
            <Tarjeta className="divide-borde divide-y p-0 sm:p-0">
              {delDia.isPending && <Cargando texto="Cargando…" />}
              {delDia.data && reservasHoy.length === 0 && (
                <div className="text-texto-suave flex flex-col items-center gap-2 px-6 py-8 text-center text-sm">
                  <Coffee className="size-6" aria-hidden />
                  No hay reservas confirmadas para hoy.
                </div>
              )}
              {reservasHoy.slice(0, 8).map((r) => (
                <Link
                  key={r.id}
                  to={`/admin/reservas/${r.id}`}
                  className="hover:bg-tenue/60 flex items-center justify-between gap-3 px-5 py-3.5"
                >
                  <div className="min-w-0">
                    <p className="font-titulo text-lg font-bold tabular-nums">
                      {rangoHorario(r.inicio, r.fin)}
                    </p>
                    <p className="text-texto-suave truncate text-sm">
                      {r.cancha.nombre} · {r.cliente.nombre}
                    </p>
                  </div>
                  <span className="flex items-center gap-2">
                    {r.precioFinal > r.montoPagado && (
                      <span className="text-alerta hidden text-xs font-medium sm:inline">
                        Saldo {formatearCOP(r.precioFinal - r.montoPagado)}
                      </span>
                    )}
                    <InsigniaEstadoReserva estado={r.estado} />
                    <ChevronRight className="text-texto-suave size-4" aria-hidden />
                  </span>
                </Link>
              ))}
            </Tarjeta>
            {reservasHoy.length > 8 && (
              <Link to="/admin/agenda" className="enlace text-sm">
                Ver las {reservasHoy.length} reservas de hoy
              </Link>
            )}
          </section>

          {t && (
            <div className="grid gap-6 md:grid-cols-2">
              <Cobros c={t.cobros} />
              <Torneos t={t.torneos} />
            </div>
          )}
        </div>

        {t && (
          <div className="flex flex-col gap-6">
            <CanchasAhora canchas={t.canchas} />
            <ProximosDias dias={t.proximosDias} />
            <Tarjeta>
              <TituloTarjeta
                titulo="Clientes"
                acciones={
                  <Link to="/admin/clientes" className="enlace text-sm">
                    Ver clientes
                  </Link>
                }
              />
              <div className="grid grid-cols-3 gap-2 text-center">
                {[
                  { texto: 'Activos', valor: t.clientes.activos },
                  { texto: 'Nuevos 7 días', valor: t.clientes.nuevosSemana },
                  { texto: 'Nuevos del mes', valor: t.clientes.nuevosMes },
                ].map((x) => (
                  <div key={x.texto} className="bg-tenue rounded-2xl px-2 py-3">
                    <p className="font-titulo text-3xl leading-none font-extrabold">{x.valor}</p>
                    <p className="text-texto-suave text-xs">{x.texto}</p>
                  </div>
                ))}
              </div>
              <p className="text-texto-suave mt-3 flex items-center gap-1.5 text-xs">
                <UserPlus className="size-3.5" aria-hidden />
                Los clientes se registran desde el sitio público.
              </p>
            </Tarjeta>
          </div>
        )}
      </div>
    </div>
  )
}
