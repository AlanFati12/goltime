import { useState } from 'react'
import { Link, NavLink, Outlet } from 'react-router-dom'

import { useSesion } from '@/caracteristicas/autenticacion/almacenSesion'
import { CampanaNotificaciones } from '@/caracteristicas/notificaciones/CampanaNotificaciones'
import { EnlaceBoton } from '@/componentes/ui/EnlaceBoton'
import { cn } from '@/lib/utils'

import { MenuUsuario } from './MenuUsuario'

const ENLACES = [
  { a: '/sedes', texto: 'Sedes' },
  { a: '/torneos', texto: 'Torneos' },
  { a: '/promociones', texto: 'Promociones' },
]

const estiloEnlace = ({ isActive }: { isActive: boolean }) =>
  cn(
    'rounded-boton px-3 py-2 text-sm font-medium transition-colors',
    isActive ? 'text-primario-oscuro bg-primario-claro' : 'text-texto-suave hover:text-texto',
  )

export function LayoutPublico() {
  const usuario = useSesion((s) => s.usuario)
  const [menuAbierto, setMenuAbierto] = useState(false)

  return (
    <div className="flex min-h-screen flex-col">
      <header className="border-borde bg-superficie/90 sticky top-0 z-30 border-b backdrop-blur">
        <div className="mx-auto flex max-w-6xl items-center gap-2 px-4 py-3">
          <Link to="/" aria-label="Goltime, ir al inicio" className="mr-4">
            <img src="/logo.svg" alt="Goltime" className="h-9" />
          </Link>
          <nav aria-label="Principal" className="hidden items-center gap-1 md:flex">
            {ENLACES.map((e) => (
              <NavLink key={e.a} to={e.a} className={estiloEnlace}>
                {e.texto}
              </NavLink>
            ))}
          </nav>
          <div className="ml-auto flex items-center gap-2">
            {usuario?.rol !== 'ADMIN' && (
              <EnlaceBoton to="/reservar" tamano="sm">
                Reservar
              </EnlaceBoton>
            )}
            {usuario ? (
              <>
                <CampanaNotificaciones />
                <MenuUsuario />
              </>
            ) : (
              <>
                <Link
                  to="/ingresar"
                  className="text-texto-suave hover:text-texto hidden px-2 text-sm font-medium sm:block"
                >
                  Ingresar
                </Link>
                <EnlaceBoton
                  to="/registro"
                  tamano="sm"
                  variante="secundario"
                  className="hidden sm:inline-flex"
                >
                  Registrarme
                </EnlaceBoton>
              </>
            )}
            <button
              type="button"
              className="hover:bg-primario-claro rounded-boton inline-flex size-9 items-center justify-center md:hidden"
              aria-label={menuAbierto ? 'Cerrar menú' : 'Abrir menú'}
              aria-expanded={menuAbierto}
              onClick={() => setMenuAbierto((a) => !a)}
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
                {menuAbierto ? (
                  <path d="M6 6l12 12M18 6 6 18" />
                ) : (
                  <path d="M4 7h16M4 12h16M4 17h16" />
                )}
              </svg>
            </button>
          </div>
        </div>
        {menuAbierto && (
          <nav
            aria-label="Principal móvil"
            className="border-borde flex flex-col gap-1 border-t px-4 py-3 md:hidden"
            onClick={() => setMenuAbierto(false)}
          >
            {ENLACES.map((e) => (
              <NavLink key={e.a} to={e.a} className={estiloEnlace}>
                {e.texto}
              </NavLink>
            ))}
            {!usuario && (
              <>
                <NavLink to="/ingresar" className={estiloEnlace}>
                  Ingresar
                </NavLink>
                <NavLink to="/registro" className={estiloEnlace}>
                  Registrarme
                </NavLink>
              </>
            )}
          </nav>
        )}
      </header>
      <main className="mx-auto w-full max-w-6xl flex-1 px-4 py-8 sm:py-10">
        <Outlet />
      </main>
      <footer className="border-borde text-texto-suave border-t py-6 text-center text-xs">
        Goltime · Universidad Surcolombiana · INNOVASOFT IX
      </footer>
    </div>
  )
}
