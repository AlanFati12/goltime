import {
  Award,
  Building2,
  CalendarDays,
  CalendarX2,
  ChartColumn,
  ExternalLink,
  Flag,
  LayoutDashboard,
  ListChecks,
  Megaphone,
  Menu,
  Repeat,
  ScrollText,
  Settings,
  Shield,
  Shirt,
  Trophy,
  Users,
  X,
  Ban,
  type LucideIcon,
} from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link, NavLink, Outlet, useLocation } from 'react-router-dom'

import { CampanaNotificaciones } from '@/caracteristicas/notificaciones/CampanaNotificaciones'
import { useSolicitudesPendientes } from '@/caracteristicas/admin/consultasAdmin'
import { cn } from '@/lib/utils'

import { BarraCarga } from './BarraCarga'
import { Logo } from './Logo'
import { MenuUsuario } from './MenuUsuario'

interface Enlace {
  a: string
  texto: string
  icono: LucideIcon
  fin?: boolean
}

/** El menú va agrupado por tarea para que nadie tenga que recorrer 17 opciones planas. */
const GRUPOS: { titulo?: string; enlaces: Enlace[] }[] = [
  { enlaces: [{ a: '/admin', texto: 'Inicio', icono: LayoutDashboard, fin: true }] },
  {
    titulo: 'Operación',
    enlaces: [
      { a: '/admin/agenda', texto: 'Agenda', icono: CalendarDays },
      { a: '/admin/reservas', texto: 'Reservas', icono: ListChecks },
      { a: '/admin/series', texto: 'Recurrentes', icono: Repeat },
      { a: '/admin/bloqueos', texto: 'Bloqueos', icono: Ban },
    ],
  },
  {
    titulo: 'Competencias',
    enlaces: [
      { a: '/admin/torneos', texto: 'Torneos', icono: Trophy },
      { a: '/admin/equipos', texto: 'Equipos', icono: Shirt },
    ],
  },
  {
    titulo: 'Crecimiento',
    enlaces: [
      { a: '/admin/estadisticas', texto: 'Estadísticas', icono: ChartColumn },
      { a: '/admin/promociones', texto: 'Promociones', icono: Megaphone },
      { a: '/admin/premios', texto: 'Premios', icono: Award },
    ],
  },
  {
    titulo: 'Negocio',
    enlaces: [
      { a: '/admin/sedes', texto: 'Sedes', icono: Building2 },
      { a: '/admin/canchas', texto: 'Canchas', icono: Flag },
      { a: '/admin/festivos', texto: 'Festivos', icono: CalendarX2 },
    ],
  },
  {
    titulo: 'Sistema',
    enlaces: [
      { a: '/admin/clientes', texto: 'Clientes', icono: Users },
      { a: '/admin/administradores', texto: 'Administradores', icono: Shield },
      { a: '/admin/configuracion', texto: 'Configuración', icono: Settings },
      { a: '/admin/auditoria', texto: 'Auditoría', icono: ScrollText },
    ],
  },
]

/** Panel del administrador: barra lateral agrupada en escritorio y cajón desplegable en móvil. */
export function LayoutAdmin() {
  const [abierto, setAbierto] = useState(false)
  const pendientes = useSolicitudesPendientes().data?.totalElementos ?? 0
  const { pathname } = useLocation()

  useEffect(() => {
    window.scrollTo(0, 0)
  }, [pathname])

  const navegacion = (
    <nav
      aria-label="Administración"
      className="flex flex-col gap-5 px-3 pb-6"
      onClick={() => setAbierto(false)}
    >
      {GRUPOS.map((g, i) => (
        <div key={g.titulo ?? i} className="flex flex-col gap-0.5">
          {g.titulo && (
            <p className="px-3 pb-1.5 text-xs font-semibold text-white/35">{g.titulo}</p>
          )}
          {g.enlaces.map((e) => (
            <NavLink
              key={e.a}
              to={e.a}
              end={e.fin}
              className={({ isActive }) =>
                cn(
                  'group flex items-center gap-3 rounded-xl px-3 py-2 text-sm font-semibold transition-colors duration-150',
                  isActive
                    ? 'bg-volt text-noche'
                    : 'text-white/65 hover:bg-white/[0.06] hover:text-white',
                )
              }
            >
              <e.icono className="size-4 shrink-0" aria-hidden />
              <span className="flex-1">{e.texto}</span>
              {e.a === '/admin/reservas' && pendientes > 0 && (
                <span className="bg-acento text-noche rounded-full px-2 text-xs font-bold">
                  {pendientes}
                </span>
              )}
            </NavLink>
          ))}
        </div>
      ))}
    </nav>
  )

  return (
    <div className="flex min-h-screen">
      {/* Barra lateral fija en escritorio */}
      <aside className="bg-noche sticky top-0 hidden h-screen w-64 shrink-0 [scrollbar-color:#26324a_transparent] flex-col overflow-y-auto lg:flex">
        <Link to="/admin" className="flex items-center gap-2 px-6 pt-6 pb-7">
          <Logo claro />
          <span className="bg-volt/15 text-volt-vivo rounded-full px-2 py-0.5 text-xs font-bold">
            Admin
          </span>
        </Link>
        {navegacion}
      </aside>

      {/* Cajón en móvil y tableta */}
      {abierto && (
        <div className="fixed inset-0 z-40 lg:hidden">
          <div
            className="bg-noche/50 animate-velo absolute inset-0 backdrop-blur-[2px]"
            onClick={() => setAbierto(false)}
            aria-hidden
          />
          <aside className="bg-noche animate-cajon absolute inset-y-0 left-0 flex w-72 flex-col overflow-y-auto shadow-xl">
            <div className="flex items-center justify-between px-5 py-5">
              <Logo claro />
              <button
                type="button"
                className="presionable rounded-full p-2 text-white/70 hover:bg-white/10 hover:text-white"
                aria-label="Cerrar menú"
                onClick={() => setAbierto(false)}
              >
                <X className="size-5" aria-hidden />
              </button>
            </div>
            {navegacion}
          </aside>
        </div>
      )}

      <div className="flex min-w-0 flex-1 flex-col">
        <BarraCarga />
        <header className="border-borde/70 sticky top-0 z-30 flex h-16 items-center justify-between gap-3 border-b bg-white/85 px-4 backdrop-blur-md md:px-8">
          <div className="flex items-center gap-2">
            <button
              type="button"
              className="hover:bg-tenue presionable -ml-2 rounded-full p-2 lg:hidden"
              aria-label="Abrir menú"
              aria-expanded={abierto}
              onClick={() => setAbierto(true)}
            >
              <Menu className="size-5" aria-hidden />
            </button>
            <Link
              to="/"
              className="text-texto-suave hover:text-texto hover:bg-tenue inline-flex items-center gap-1.5 rounded-full px-3 py-1.5 text-sm font-semibold transition-colors"
            >
              <ExternalLink className="size-4" aria-hidden />
              Ver el sitio
            </Link>
          </div>
          <div className="flex items-center gap-2">
            <CampanaNotificaciones />
            <MenuUsuario />
          </div>
        </header>
        <main className="flex-1 px-4 py-6 md:px-8 md:py-8">
          <div key={pathname} className="animate-entrada-pagina mx-auto w-full max-w-7xl">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  )
}
