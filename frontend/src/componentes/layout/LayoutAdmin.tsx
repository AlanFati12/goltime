import { useState } from 'react'
import { Link, NavLink, Outlet } from 'react-router-dom'

import { CampanaNotificaciones } from '@/caracteristicas/notificaciones/CampanaNotificaciones'
import { useSolicitudesPendientes } from '@/caracteristicas/admin/consultasAdmin'
import { cn } from '@/lib/utils'

import { MenuUsuario } from './MenuUsuario'

interface Enlace {
  a: string
  texto: string
  fin?: boolean
}

/** El menú va agrupado por tarea para que nadie tenga que recorrer 17 opciones planas. */
const GRUPOS: { titulo?: string; enlaces: Enlace[] }[] = [
  { enlaces: [{ a: '/admin', texto: 'Inicio', fin: true }] },
  {
    titulo: 'Operación',
    enlaces: [
      { a: '/admin/agenda', texto: 'Agenda' },
      { a: '/admin/reservas', texto: 'Reservas' },
      { a: '/admin/series', texto: 'Recurrentes' },
      { a: '/admin/bloqueos', texto: 'Bloqueos' },
    ],
  },
  {
    titulo: 'Competencias',
    enlaces: [
      { a: '/admin/torneos', texto: 'Torneos' },
      { a: '/admin/equipos', texto: 'Equipos' },
    ],
  },
  {
    titulo: 'Crecimiento',
    enlaces: [
      { a: '/admin/estadisticas', texto: 'Estadísticas' },
      { a: '/admin/promociones', texto: 'Promociones' },
      { a: '/admin/premios', texto: 'Premios' },
    ],
  },
  {
    titulo: 'Negocio',
    enlaces: [
      { a: '/admin/sedes', texto: 'Sedes' },
      { a: '/admin/canchas', texto: 'Canchas' },
      { a: '/admin/festivos', texto: 'Festivos' },
    ],
  },
  {
    titulo: 'Sistema',
    enlaces: [
      { a: '/admin/clientes', texto: 'Clientes' },
      { a: '/admin/administradores', texto: 'Administradores' },
      { a: '/admin/configuracion', texto: 'Configuración' },
      { a: '/admin/auditoria', texto: 'Auditoría' },
    ],
  },
]

/** Panel del administrador: barra lateral agrupada en escritorio y cajón desplegable en móvil. */
export function LayoutAdmin() {
  const [abierto, setAbierto] = useState(false)
  const pendientes = useSolicitudesPendientes().data?.totalElementos ?? 0

  return (
    <div className="flex min-h-screen flex-col md:flex-row">
      <aside
        className={cn(
          'bg-primario-oscuro text-white md:sticky md:top-0 md:h-screen md:w-60 md:shrink-0 md:overflow-y-auto',
          !abierto && 'max-md:sticky max-md:top-0 max-md:z-30',
        )}
      >
        <div className="flex items-center justify-between px-4 py-3 md:px-5 md:py-5">
          <Link to="/admin" className="flex flex-col leading-tight">
            <span className="font-titulo text-xl font-bold">Goltime</span>
            <span className="text-xs text-white/60">Administración</span>
          </Link>
          <button
            type="button"
            className="inline-flex size-9 items-center justify-center rounded-full hover:bg-white/10 md:hidden"
            aria-label={abierto ? 'Cerrar menú' : 'Abrir menú'}
            aria-expanded={abierto}
            onClick={() => setAbierto((a) => !a)}
          >
            <svg
              viewBox="0 0 24 24"
              className="size-5"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              aria-hidden
            >
              {abierto ? <path d="M6 6l12 12M18 6 6 18" /> : <path d="M4 7h16M4 12h16M4 17h16" />}
            </svg>
          </button>
        </div>
        <nav
          aria-label="Administración"
          onClick={() => setAbierto(false)}
          className={cn('flex-col gap-5 px-3 pb-6', abierto ? 'flex' : 'hidden md:flex')}
        >
          {GRUPOS.map((g, i) => (
            <div key={g.titulo ?? i} className="flex flex-col gap-0.5">
              {g.titulo && (
                <p className="px-3 pb-1 text-[0.7rem] font-semibold tracking-wider text-white/50 uppercase">
                  {g.titulo}
                </p>
              )}
              {g.enlaces.map((e) => (
                <NavLink
                  key={e.a}
                  to={e.a}
                  end={e.fin}
                  className={({ isActive }) =>
                    cn(
                      'rounded-boton flex items-center justify-between px-3 py-2 text-sm font-medium text-white/80 hover:bg-white/10 hover:text-white',
                      isActive && 'bg-white/15 text-white',
                    )
                  }
                >
                  {e.texto}
                  {e.a === '/admin/reservas' && pendientes > 0 && (
                    <span className="bg-acento text-texto rounded-full px-2 text-xs font-bold">
                      {pendientes}
                    </span>
                  )}
                </NavLink>
              ))}
            </div>
          ))}
        </nav>
      </aside>
      <div className="flex min-w-0 flex-1 flex-col">
        <header className="border-borde bg-superficie flex items-center justify-between gap-3 border-b px-4 py-2.5 md:px-8">
          <Link to="/" className="text-texto-suave hover:text-primario text-sm font-medium">
            ← Ver el sitio
          </Link>
          <div className="flex items-center gap-2">
            <CampanaNotificaciones />
            <MenuUsuario />
          </div>
        </header>
        <main className="flex-1 p-4 md:p-8">
          <div className="mx-auto w-full max-w-6xl">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  )
}
