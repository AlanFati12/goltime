import { ArrowRight, ArrowUpRight, Clock, MapPin, Trophy } from 'lucide-react'
import { Link } from 'react-router-dom'

import { useSesion } from '@/caracteristicas/autenticacion/almacenSesion'
import { usePromocionesPublicas } from '@/caracteristicas/promociones/consultas'
import { useSedesPublicas } from '@/caracteristicas/sedes/consultas'
import { horarioDeHoy, modalidadesDe, precioDesde } from '@/caracteristicas/sedes/horario'
import type { Sede } from '@/caracteristicas/sedes/tipos'
import { useProximosPartidos, useTorneos } from '@/caracteristicas/torneos/consultas'
import type { Partido } from '@/caracteristicas/torneos/tipos'
import { marcador, nombreLado, textoRonda } from '@/caracteristicas/torneos/utilidades'
import { Esqueleto } from '@/componentes/ui/Cargando'
import { EnlaceBoton } from '@/componentes/ui/EnlaceBoton'
import { Revelar } from '@/componentes/ui/Revelar'
import { diaDelMes, diaSemanaCorto, formatearCOP, formatearHora } from '@/lib/formato'
import { cn } from '@/lib/utils'
import { ImagenSitio } from '@/componentes/ui/ImagenSitio'

const PASOS = [
  {
    titulo: 'Elige sede y cancha',
    texto: 'Compara modalidades, precios por hora y servicios de cada sede.',
  },
  {
    titulo: 'Escoge tu horario',
    texto: 'Toca un espacio libre del calendario y mira el valor antes de enviar.',
  },
  {
    titulo: 'Llega y juega',
    texto: 'La sede confirma tu solicitud y te avisamos por correo y en la app.',
  },
]

const FRANJA = [
  'Reserva en línea',
  'Canchas iluminadas',
  'Torneos',
  'Premios por jugar',
  'Promociones',
  'Neiva',
]

export function PaginaInicio() {
  return (
    <>
      <Portada />
      <Franja />
      <ComoFunciona />
      <Sedes />
      <Torneos />
      <Premios />
      <Cierre />
    </>
  )
}

/* ------------------------------------------------------------------ Portada */

function Portada() {
  const usuario = useSesion((s) => s.usuario)
  const esAdmin = usuario?.rol === 'ADMIN'
  const sedes = useSedesPublicas()

  return (
    <section className="bg-noche relative isolate flex min-h-[100svh] items-end overflow-hidden text-white">
      {/* Foto de una cancha enmallada de noche, con un desplazamiento lento al hacer scroll. */}
      <div className="grano absolute inset-0 -z-10">
        <ImagenSitio
          src="/img/hero-cancha-nocturna.webp"
          alt=""
          prioridad
          className="paralaje absolute inset-0 size-full object-cover object-[60%_center]"
        />
        <div className="from-noche via-noche/80 absolute inset-0 bg-gradient-to-r to-transparent" />
        <div className="from-noche absolute inset-x-0 bottom-0 h-2/3 bg-gradient-to-t to-transparent" />
        {/* El reflector: un halo lima que respira sobre la cancha. */}
        <div
          className="animate-latido absolute top-[8%] right-[12%] size-[38rem] rounded-full motion-reduce:animate-none"
          style={{
            background:
              'radial-gradient(closest-side, color-mix(in srgb, var(--color-volt) 28%, transparent), transparent)',
          }}
          aria-hidden="true"
        />
      </div>

      <div className="contenedor pt-36 pb-10 sm:pb-14">
        <h1 className="font-titulo text-[clamp(3.4rem,9vw,8.25rem)] leading-[0.86] font-extrabold tracking-[-0.025em] uppercase">
          <span className="animate-subir block [animation-delay:380ms]">
            Reserva tu cancha<span className="text-volt">,</span>
          </span>
          <span className="animate-subir text-volt-vivo block [animation-delay:460ms]">
            juega hoy.
          </span>
        </h1>
        <p className="animate-subir mt-7 max-w-xl text-lg leading-relaxed text-white/80 [animation-delay:560ms] sm:text-xl">
          Canchas de fútbol sintético en Neiva. Elige sede, horario y modalidad, y envía tu
          solicitud de reserva en menos de un minuto.
        </p>
        <div className="animate-subir mt-9 flex flex-wrap gap-3 [animation-delay:640ms]">
          {!esAdmin && (
            <EnlaceBoton to="/reservar" tamano="xl" variante="volt">
              Reservar una cancha
              <ArrowRight aria-hidden />
            </EnlaceBoton>
          )}
          <EnlaceBoton to="/sedes" tamano="xl" variante={esAdmin ? 'volt' : 'claro'}>
            Ver sedes
          </EnlaceBoton>
        </div>

        {/* Lo único que cambia cada visita: qué sedes están abiertas en este momento. */}
        <div className="animate-subir border-noche-borde/80 mt-14 grid grid-cols-[repeat(auto-fit,minmax(min(100%,18rem),1fr))] gap-px overflow-hidden rounded-2xl border bg-white/5 backdrop-blur-md [animation-delay:720ms] sm:mt-20">
          {sedes.isPending &&
            [0, 1, 2].map((i) => <Esqueleto key={i} className="h-20 rounded-none opacity-10" />)}
          {sedes.data?.slice(0, 3).map((s) => (
            <SedeEnVivo key={s.id} sede={s} />
          ))}
        </div>
      </div>
    </section>
  )
}

function SedeEnVivo({ sede }: { sede: Sede }) {
  const hoy = horarioDeHoy(sede.horario)
  return (
    <Link
      to={`/sedes/${sede.id}`}
      className="group bg-noche/40 flex items-center gap-4 px-5 py-4 transition-colors duration-200 hover:bg-white/10"
    >
      <span className="relative flex size-3 shrink-0" aria-hidden>
        {hoy.abiertaAhora && (
          <span className="bg-volt absolute inset-0 animate-ping rounded-full opacity-60 motion-reduce:animate-none" />
        )}
        <span
          className={cn(
            'relative size-3 rounded-full',
            hoy.abiertaAhora ? 'bg-volt' : 'bg-white/30',
          )}
        />
      </span>
      <span className="min-w-0 flex-1">
        <span className="block truncate font-semibold">{sede.nombre}</span>
        <span className="block text-sm text-white/60">
          {hoy.abiertaAhora ? 'Bajo los reflectores · ' : ''}
          {hoy.texto}
        </span>
      </span>
      <ArrowUpRight
        className="size-5 text-white/40 transition-[transform,color] duration-200 group-hover:translate-x-0.5 group-hover:-translate-y-0.5 group-hover:text-white"
        aria-hidden
      />
    </Link>
  )
}

/* ------------------------------------------------------------------ Franja */

/** Cinta lima que se desplaza sin fin: separa la portada del resto con energía. */
function Franja() {
  const elementos = [...FRANJA, ...FRANJA]
  return (
    <div className="bg-volt text-noche overflow-hidden py-4" aria-hidden>
      <div className="animate-deslizar flex w-max motion-reduce:animate-none">
        {[0, 1].map((copia) => (
          <ul key={copia} className="flex shrink-0">
            {elementos.map((t, i) => (
              <li
                key={`${copia}-${i}`}
                className="font-titulo flex items-center gap-8 pr-8 text-2xl font-extrabold uppercase sm:text-3xl"
              >
                {t}
                <svg viewBox="0 0 16 16" className="size-4">
                  <circle cx="8" cy="8" r="6" fill="none" stroke="currentColor" strokeWidth="2.5" />
                </svg>
              </li>
            ))}
          </ul>
        ))}
      </div>
    </div>
  )
}

/* ------------------------------------------------------------------ Cómo funciona */

function ComoFunciona() {
  return (
    <section aria-labelledby="titulo-como" className="contenedor py-24 sm:py-36">
      <div className="grid items-center gap-12 lg:grid-cols-[1fr_1.05fr] lg:gap-20">
        <Revelar imagen className="relative aspect-[4/5] overflow-hidden rounded-[2rem] lg:order-2">
          <ImagenSitio
            src="/img/balon-pies.webp"
            alt="Jugadores con un balón sobre césped sintético"
            sizes="(min-width: 1024px) 42vw, 90vw"
            className="size-full object-cover"
          />
          <div className="bg-noche text-volt-vivo absolute bottom-5 left-5 flex items-center gap-2 rounded-full px-4 py-2 text-sm font-semibold">
            <Clock className="size-4" aria-hidden />
            Menos de un minuto
          </div>
        </Revelar>

        <div>
          <Revelar>
            <h2 id="titulo-como" className="titulo-seccion">
              Así de fácil
            </h2>
            <p className="text-texto-suave mt-5 max-w-md text-lg">
              Sin llamadas ni mensajes de ida y vuelta: ves los horarios libres y el valor antes de
              enviar la solicitud.
            </p>
          </Revelar>
          <ol className="mt-12 flex flex-col gap-2">
            {PASOS.map((p, i) => (
              <Revelar
                como="li"
                key={p.titulo}
                i={i + 1}
                className="group hover:bg-superficie flex gap-6 rounded-3xl p-5 transition-colors duration-300 sm:p-6"
              >
                <span
                  className="font-titulo text-primario group-hover:text-texto w-14 shrink-0 text-6xl leading-[0.8] font-extrabold transition-colors duration-300"
                  aria-hidden
                >
                  {i + 1}
                </span>
                <span>
                  <span className="font-titulo block text-2xl font-bold uppercase">{p.titulo}</span>
                  <span className="text-texto-suave mt-1 block">{p.texto}</span>
                </span>
              </Revelar>
            ))}
          </ol>
        </div>
      </div>
    </section>
  )
}

/* ------------------------------------------------------------------ Sedes */

function Sedes() {
  const { data, isPending } = useSedesPublicas()
  const sedes = data ?? []
  return (
    <section aria-labelledby="titulo-sedes" className="bg-superficie py-24 sm:py-36">
      <div className="contenedor">
        <Revelar className="flex flex-wrap items-end justify-between gap-6">
          <div>
            <h2 id="titulo-sedes" className="titulo-seccion">
              Nuestras sedes
            </h2>
            <p className="text-texto-suave mt-5 max-w-lg text-lg">
              Cada sede con sus canchas, horarios y precios por franja. Entra y mira cuál te queda
              mejor.
            </p>
          </div>
          <EnlaceBoton to="/sedes" variante="secundario" tamano="lg">
            Todas las sedes
            <ArrowRight aria-hidden />
          </EnlaceBoton>
        </Revelar>

        <div
          className={cn('mt-14 grid gap-6', sedes.length === 1 ? 'grid-cols-1' : 'md:grid-cols-2')}
        >
          {isPending && [0, 1].map((i) => <Esqueleto key={i} className="aspect-[16/10]" />)}
          {sedes.slice(0, 4).map((s, i) => (
            <TarjetaSede key={s.id} sede={s} i={i} ancha={sedes.length === 1} />
          ))}
        </div>
      </div>
    </section>
  )
}

function TarjetaSede({ sede, i, ancha }: { sede: Sede; i: number; ancha: boolean }) {
  const desde = precioDesde(sede)
  const hoy = horarioDeHoy(sede.horario)
  const foto = sede.fotos[0]?.url ?? '/img/cancha-aerea.webp'
  return (
    <Revelar i={i}>
      <Link
        to={`/sedes/${sede.id}`}
        className={cn(
          'group bg-noche relative isolate flex overflow-hidden rounded-[2rem] text-white',
          ancha ? 'min-h-[30rem] md:min-h-[34rem]' : 'aspect-[4/5] sm:aspect-[16/12]',
        )}
      >
        <ImagenSitio
          src={foto}
          alt={`Sede ${sede.nombre}`}
          sizes="(min-width: 640px) 50vw, 100vw"
          className="absolute inset-0 -z-10 size-full object-cover transition-transform duration-[1200ms] ease-[var(--ease-salida)] group-hover:scale-105"
        />
        <div className="from-noche via-noche/55 absolute inset-0 -z-10 bg-gradient-to-t to-transparent" />
        <div className="mt-auto flex w-full flex-col gap-5 p-6 sm:p-9">
          <div className="flex flex-wrap gap-2">
            {modalidadesDe(sede).map((m) => (
              <span
                key={m}
                className="rounded-full bg-white/12 px-3 py-1 text-sm font-semibold backdrop-blur-sm"
              >
                {m}
              </span>
            ))}
            <span className="rounded-full bg-white/12 px-3 py-1 text-sm font-semibold backdrop-blur-sm">
              {sede.canchas.length} {sede.canchas.length === 1 ? 'cancha' : 'canchas'}
            </span>
          </div>
          <div className="flex items-end justify-between gap-6">
            <div className="min-w-0">
              <h3 className="font-titulo text-4xl leading-[0.95] font-extrabold uppercase sm:text-6xl">
                {sede.nombre}
              </h3>
              <p className="mt-3 flex items-center gap-1.5 text-white/75">
                <MapPin className="size-4 shrink-0" aria-hidden />
                {sede.direccion} · {sede.ciudad}
              </p>
              <p
                className={cn(
                  'mt-1 text-sm',
                  hoy.abiertaAhora ? 'text-volt-vivo' : 'text-white/60',
                )}
              >
                {hoy.texto}
              </p>
            </div>
            <div className="shrink-0 text-right">
              {desde != null && (
                <>
                  <p className="text-sm text-white/60">Desde</p>
                  <p className="font-titulo cifras text-volt-vivo text-3xl font-extrabold sm:text-4xl">
                    {formatearCOP(desde)}
                  </p>
                  <p className="text-sm text-white/60">por hora</p>
                </>
              )}
            </div>
          </div>
        </div>
        <span className="bg-volt text-noche absolute top-6 right-6 flex size-12 items-center justify-center rounded-full opacity-0 transition-[opacity,transform] duration-300 group-hover:opacity-100 group-focus-visible:opacity-100 sm:translate-y-2 sm:group-hover:translate-y-0">
          <ArrowUpRight className="size-5" aria-hidden />
        </span>
      </Link>
    </Revelar>
  )
}

/* ------------------------------------------------------------------ Torneos */

function Torneos() {
  const partidos = useProximosPartidos(4)
  const torneos = useTorneos(false)
  const enJuego = (torneos.data ?? []).filter(
    (t) => t.estado === 'EN_CURSO' || t.estado === 'INSCRIPCIONES',
  )
  const lista = partidos.data ?? []

  return (
    <section
      aria-labelledby="titulo-torneos"
      className="bg-noche relative isolate overflow-hidden py-24 text-white sm:py-36"
    >
      <div className="absolute inset-y-0 right-0 -z-10 w-full lg:w-[55%]">
        <ImagenSitio
          src="/img/jugadores-movimiento.webp"
          alt=""
          sizes="(min-width: 1024px) 55vw, 100vw"
          className="size-full object-cover opacity-60"
        />
        <div className="from-noche via-noche/70 absolute inset-0 bg-gradient-to-r to-transparent" />
        <div className="from-noche absolute inset-0 bg-gradient-to-t to-transparent to-50%" />
      </div>

      <div className="contenedor grid gap-14 lg:grid-cols-[1fr_1fr]">
        <Revelar className="flex flex-col items-start gap-6">
          <h2 id="titulo-torneos" className="titulo-seccion">
            Torneos bajo
            <br />
            <span className="text-volt-vivo">los reflectores</span>
          </h2>
          <p className="max-w-md text-lg text-white/70">
            Inscribe a tu equipo, sigue el fixture, la tabla de posiciones y los goleadores de cada
            torneo, partido a partido.
          </p>
          {enJuego.length > 0 && (
            <ul className="flex flex-wrap gap-2">
              {enJuego.slice(0, 4).map((t) => (
                <li key={t.id}>
                  <Link
                    to={`/torneos/${t.id}`}
                    className="border-noche-borde inline-flex items-center gap-2 rounded-full border bg-white/5 px-4 py-2 text-sm font-semibold transition-colors hover:border-white/40"
                  >
                    <Trophy className="text-volt size-4" aria-hidden />
                    {t.nombre}
                    <span className="text-white/50">
                      {t.estado === 'EN_CURSO' ? 'En curso' : 'Inscripciones abiertas'}
                    </span>
                  </Link>
                </li>
              ))}
            </ul>
          )}
          <EnlaceBoton to="/torneos" variante="volt" tamano="lg" className="mt-2">
            Ver torneos
            <ArrowRight aria-hidden />
          </EnlaceBoton>
        </Revelar>

        <div className="flex flex-col gap-3 lg:pt-4">
          <Revelar i={1}>
            <h3 className="text-sm font-semibold text-white/60">Próximos partidos</h3>
          </Revelar>
          {partidos.isPending &&
            [0, 1, 2].map((i) => <Esqueleto key={i} className="h-24 opacity-10" />)}
          {lista.length === 0 && !partidos.isPending && (
            <Revelar i={2} className="border-noche-borde rounded-3xl border bg-white/5 p-8">
              <p className="font-titulo text-2xl font-bold uppercase">Sin partidos programados</p>
              <p className="mt-1 text-white/60">
                Cuando se publique el fixture de un torneo, los partidos aparecerán aquí.
              </p>
            </Revelar>
          )}
          {lista.map((p, i) => (
            <Revelar key={p.id} i={i + 2}>
              <FilaPartido partido={p} />
            </Revelar>
          ))}
        </div>
      </div>
    </section>
  )
}

/** Partido como fila de marcador: fecha en lima, equipos y resultado o "vs". */
function FilaPartido({ partido: p }: { partido: Partido }) {
  const conResultado = p.golesLocal != null && p.golesVisitante != null
  return (
    <Link
      to={`/torneos/partidos/${p.id}`}
      className="group border-noche-borde bg-noche/70 flex items-center gap-5 rounded-3xl border p-4 backdrop-blur-md transition-[border-color,background-color] duration-200 hover:border-white/30 hover:bg-white/10 sm:p-5"
    >
      <div className="bg-volt text-noche flex w-16 shrink-0 flex-col items-center rounded-2xl py-2 sm:w-[4.5rem]">
        {p.inicio ? (
          <>
            <span className="text-xs font-bold uppercase">{diaSemanaCorto(p.inicio)}</span>
            <span className="font-titulo cifras text-3xl leading-none font-extrabold">
              {diaDelMes(p.inicio)}
            </span>
            <span className="text-xs font-semibold">{formatearHora(p.inicio)}</span>
          </>
        ) : (
          <span className="px-1 py-2 text-center text-xs font-bold">Por definir</span>
        )}
      </div>
      <div className="min-w-0 flex-1">
        <p className="truncate text-sm text-white/55">
          {p.torneo}
          {textoRonda(p) && ` · ${textoRonda(p)}`}
        </p>
        <p className="mt-1 flex flex-wrap items-baseline gap-x-2.5 font-semibold sm:text-lg">
          <span>{nombreLado(p.local)}</span>
          <span
            className={cn(
              'font-titulo cifras',
              conResultado ? 'text-volt-vivo text-xl font-extrabold' : 'text-sm text-white/40',
            )}
          >
            {conResultado ? marcador(p) : 'vs'}
          </span>
          <span>{nombreLado(p.visitante)}</span>
        </p>
        {p.cancha && <p className="mt-0.5 text-sm text-white/45">{p.cancha}</p>}
      </div>
      <ArrowUpRight
        className="size-5 shrink-0 text-white/30 transition-[transform,color] duration-200 group-hover:translate-x-0.5 group-hover:-translate-y-0.5 group-hover:text-white"
        aria-hidden
      />
    </Link>
  )
}

/* ------------------------------------------------------------------ Premios */

function Premios() {
  const { data } = usePromocionesPublicas()
  const esAdmin = useSesion((s) => s.usuario?.rol === 'ADMIN')
  const promos = (data ?? []).slice(0, 3)
  return (
    <section aria-labelledby="titulo-premios" className="contenedor py-24 sm:py-36">
      <div className="bg-noche relative isolate grid overflow-hidden rounded-[2.5rem] text-white lg:grid-cols-2">
        <Revelar imagen className="relative min-h-[22rem] lg:order-2 lg:min-h-full">
          <ImagenSitio
            src="/img/guayos-sintetica.webp"
            alt="Guayos sobre una cancha sintética de noche"
            sizes="(min-width: 1024px) 50vw, 100vw"
            className="absolute inset-0 size-full object-cover"
          />
          <div className="from-noche absolute inset-0 bg-gradient-to-t to-transparent to-40% lg:bg-gradient-to-r" />
        </Revelar>
        <div className="flex flex-col gap-6 p-8 sm:p-14">
          <Revelar>
            <h2 id="titulo-premios" className="titulo-seccion">
              Jugar tiene
              <br />
              <span className="text-volt-vivo">premio</span>
            </h2>
            <p className="mt-5 max-w-md text-lg text-white/70">
              Cada partido que juegas suma a tu progreso de fidelidad. Al completar la meta, ganas
              premios que usas en tus próximas reservas.
            </p>
          </Revelar>
          {promos.length > 0 && (
            <ul className="flex flex-col gap-3">
              {promos.map((p, i) => (
                <Revelar
                  como="li"
                  key={p.id}
                  i={i + 1}
                  className="border-noche-borde flex items-center gap-5 rounded-2xl border bg-white/5 p-4"
                >
                  <span className="font-titulo text-volt-vivo cifras min-w-24 text-3xl leading-none font-extrabold">
                    {p.descuentoTexto}
                  </span>
                  <span className="min-w-0">
                    <span className="block font-semibold">{p.nombre}</span>
                    <span className="block text-sm text-white/55">
                      {p.codigo ? `Código ${p.codigo}` : 'Se aplica sola al reservar'}
                    </span>
                  </span>
                </Revelar>
              ))}
            </ul>
          )}
          <Revelar i={4} className="mt-auto flex flex-wrap gap-3 pt-2">
            <EnlaceBoton to="/promociones" variante="volt" tamano="lg">
              Ver promociones
              <ArrowRight aria-hidden />
            </EnlaceBoton>
            {!esAdmin && (
              <EnlaceBoton to="/mis-premios" variante="claro" tamano="lg">
                Mis premios
              </EnlaceBoton>
            )}
          </Revelar>
        </div>
      </div>
    </section>
  )
}

/* ------------------------------------------------------------------ Cierre */

function Cierre() {
  const usuario = useSesion((s) => s.usuario)
  return (
    <section
      aria-labelledby="titulo-cierre"
      className="bg-noche relative isolate flex min-h-[80svh] items-center overflow-hidden text-white"
    >
      <ImagenSitio
        src="/img/arco-niebla.webp"
        alt=""
        className="paralaje absolute inset-0 -z-10 size-full object-cover opacity-70"
      />
      <div className="from-noche via-noche/40 absolute inset-0 -z-10 bg-gradient-to-t to-transparent" />
      <Revelar className="contenedor flex flex-col items-center gap-8 py-28 text-center">
        <h2
          id="titulo-cierre"
          className="font-titulo text-[clamp(3rem,9vw,8rem)] leading-[0.88] font-extrabold tracking-[-0.025em] uppercase"
        >
          ¿Armamos
          <br />
          <span className="text-volt-vivo">el partido?</span>
        </h2>
        <p className="max-w-md text-lg text-white/75">
          Reúne a tu equipo, elige la cancha y deja el resto en nuestras manos.
        </p>
        {usuario?.rol === 'ADMIN' ? (
          <EnlaceBoton to="/admin" variante="volt" tamano="xl">
            Ir al panel
            <ArrowRight aria-hidden />
          </EnlaceBoton>
        ) : (
          <EnlaceBoton to="/reservar" variante="volt" tamano="xl">
            Reservar una cancha
            <ArrowRight aria-hidden />
          </EnlaceBoton>
        )}
      </Revelar>
    </section>
  )
}
