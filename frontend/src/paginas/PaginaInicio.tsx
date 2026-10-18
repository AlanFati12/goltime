import {
  ArrowRight,
  CalendarClock,
  MapPin,
  Megaphone,
  PartyPopper,
  Search,
  Trophy,
  type LucideIcon,
} from 'lucide-react'
import { Link } from 'react-router-dom'

import { useSesion } from '@/caracteristicas/autenticacion/almacenSesion'
import { useProximosPartidos } from '@/caracteristicas/torneos/consultas'
import { TarjetaPartido } from '@/caracteristicas/torneos/TarjetaPartido'
import { EnlaceBoton } from '@/componentes/ui/EnlaceBoton'

const PASOS: { icono: LucideIcon; titulo: string; texto: string }[] = [
  { icono: Search, titulo: 'Elige tu cancha', texto: 'Mira las sedes, modalidades y precios.' },
  {
    icono: CalendarClock,
    titulo: 'Escoge el horario',
    texto: 'Toca un espacio libre en el calendario.',
  },
  { icono: PartyPopper, titulo: 'Juega', texto: 'Te confirmamos la reserva y te avisamos.' },
]

const ACCESOS: { a: string; icono: LucideIcon; titulo: string; texto: string }[] = [
  { a: '/sedes', icono: MapPin, titulo: 'Sedes y canchas', texto: 'Ubicación, horarios y tarifas' },
  { a: '/torneos', icono: Trophy, titulo: 'Torneos', texto: 'Posiciones, fixture y resultados' },
  {
    a: '/promociones',
    icono: Megaphone,
    titulo: 'Promociones',
    texto: 'Descuentos y premios vigentes',
  },
]

/** Líneas de una cancha de fútbol, decorativas, para el fondo del encabezado. */
function LineasCancha() {
  return (
    <svg
      viewBox="0 0 600 360"
      className="pointer-events-none absolute inset-y-0 right-0 h-full w-auto opacity-[0.13]"
      fill="none"
      stroke="white"
      strokeWidth="3"
      aria-hidden
    >
      <rect x="20" y="20" width="560" height="320" rx="6" />
      <line x1="300" y1="20" x2="300" y2="340" />
      <circle cx="300" cy="180" r="56" />
      <circle cx="300" cy="180" r="4" fill="white" />
      <rect x="20" y="100" width="90" height="160" />
      <rect x="20" y="140" width="34" height="80" />
      <rect x="490" y="100" width="90" height="160" />
      <rect x="546" y="140" width="34" height="80" />
    </svg>
  )
}

export function PaginaInicio() {
  const proximos = useProximosPartidos(4)
  const usuario = useSesion((s) => s.usuario)

  return (
    <div className="flex flex-col gap-14 sm:gap-20">
      <section className="relative overflow-hidden rounded-3xl bg-gradient-to-br from-emerald-700 via-emerald-600 to-teal-600 px-6 py-14 text-white sm:px-12 sm:py-20">
        <LineasCancha />
        <div className="relative flex max-w-2xl flex-col items-start gap-6">
          <span className="rounded-full bg-white/15 px-3 py-1 text-xs font-medium ring-1 ring-white/25 backdrop-blur">
            Fútbol sintético en Neiva
          </span>
          <h1 className="text-4xl leading-[1.1] font-extrabold sm:text-6xl">
            Reserva tu cancha, juega tu partido.
          </h1>
          <p className="max-w-xl text-lg text-emerald-50/90">
            Elige sede, horario y modalidad. Reservas en línea en menos de un minuto.
          </p>
          <div className="flex flex-wrap gap-3">
            {usuario?.rol !== 'ADMIN' && (
              <EnlaceBoton
                to="/reservar"
                tamano="lg"
                className="bg-white text-emerald-800 hover:bg-emerald-50"
              >
                Reservar una cancha
                <ArrowRight aria-hidden />
              </EnlaceBoton>
            )}
            <EnlaceBoton
              to="/sedes"
              tamano="lg"
              variante="fantasma"
              className="text-white ring-1 ring-white/30 hover:bg-white/10 hover:text-white"
            >
              Ver sedes
            </EnlaceBoton>
          </div>
        </div>
      </section>

      <section aria-labelledby="titulo-como" className="flex flex-col gap-6">
        <h2 id="titulo-como" className="text-2xl font-bold">
          Así de fácil
        </h2>
        <ol className="grid gap-4 sm:grid-cols-3">
          {PASOS.map((p, i) => (
            <li
              key={p.titulo}
              className="border-borde bg-superficie rounded-tarjeta shadow-suave flex flex-col gap-4 border p-6"
            >
              <div className="flex items-center justify-between">
                <span className="bg-primario-claro text-primario flex size-10 items-center justify-center rounded-xl">
                  <p.icono className="size-5" aria-hidden />
                </span>
                <span className="text-texto-suave/50 font-titulo text-3xl font-extrabold">
                  {i + 1}
                </span>
              </div>
              <div>
                <h3 className="font-semibold">{p.titulo}</h3>
                <p className="text-texto-suave mt-1 text-sm">{p.texto}</p>
              </div>
            </li>
          ))}
        </ol>
      </section>

      <section aria-label="Explorar" className="grid gap-4 sm:grid-cols-3">
        {ACCESOS.map((a) => (
          <Link
            key={a.a}
            to={a.a}
            className="border-borde bg-superficie rounded-tarjeta shadow-suave hover:border-primario/40 group flex items-center gap-4 border p-5 transition-all hover:shadow-md"
          >
            <span className="bg-tenue text-texto-suave group-hover:bg-primario-claro group-hover:text-primario flex size-11 shrink-0 items-center justify-center rounded-xl transition-colors">
              <a.icono className="size-5" aria-hidden />
            </span>
            <span className="min-w-0 flex-1">
              <span className="block font-semibold">{a.titulo}</span>
              <span className="text-texto-suave block text-sm">{a.texto}</span>
            </span>
            <ArrowRight
              className="text-texto-suave size-4 transition-transform group-hover:translate-x-0.5"
              aria-hidden
            />
          </Link>
        ))}
      </section>

      {proximos.data && proximos.data.length > 0 && (
        <section className="flex flex-col gap-5" aria-labelledby="titulo-partidos">
          <div className="flex items-end justify-between gap-4">
            <div>
              <h2 id="titulo-partidos" className="text-2xl font-bold">
                Próximos partidos
              </h2>
              <p className="text-texto-suave mt-1 text-sm">Lo que se juega en los torneos.</p>
            </div>
            <EnlaceBoton to="/torneos" variante="fantasma" tamano="sm">
              Ver torneos
              <ArrowRight aria-hidden />
            </EnlaceBoton>
          </div>
          <div className="grid gap-4 md:grid-cols-2">
            {proximos.data.map((p) => (
              <TarjetaPartido
                key={p.id}
                partido={p}
                ruta={`/torneos/partidos/${p.id}`}
                mostrarTorneo
              />
            ))}
          </div>
        </section>
      )}
    </div>
  )
}
