import { Menu, X } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link, NavLink, Outlet, useLocation } from 'react-router-dom'

import { useSesion } from '@/caracteristicas/autenticacion/almacenSesion'
import { CampanaNotificaciones } from '@/caracteristicas/notificaciones/CampanaNotificaciones'
import { EnlaceBoton } from '@/componentes/ui/EnlaceBoton'
import { cn } from '@/lib/utils'

import { Logo } from './Logo'
import { MenuUsuario } from './MenuUsuario'

const ENLACES = [
  { a: '/sedes', texto: 'Sedes' },
  { a: '/torneos', texto: 'Torneos' },
  { a: '/promociones', texto: 'Promociones' },
]

const ENLACES_CLIENTE = [{ a: '/mis-reservas', texto: 'Mis reservas' }]

const estiloEnlace = ({ isActive }: { isActive: boolean }) =>
  cn(
    'rounded-boton px-3 py-2 text-sm font-medium transition-colors',
    isActive ? 'text-texto bg-tenue' : 'text-texto-suave hover:text-texto',
  )

/** Rutas de acceso que se muestran centradas, sin el ancho completo del sitio. */
const RUTAS_ACCESO = [
  '/ingresar',
  '/registro',
  '/olvide-clave',
  '/restablecer-clave',
  '/verificar-correo',
]

export function LayoutPublico() {
  const usuario = useSesion((s) => s.usuario)
  const [menuAbierto, setMenuAbierto] = useState(false)
  const { pathname } = useLocation()
  const esCliente = usuario && usuario.rol !== 'ADMIN'
  const enlaces = esCliente ? [...ENLACES, ...ENLACES_CLIENTE] : ENLACES
  const esAcceso = RUTAS_ACCESO.includes(pathname)

  useEffect(() => {
    window.scrollTo(0, 0)
  }, [pathname])

  return (
    <div className="flex min-h-screen flex-col">
      <header className="border-borde/80 bg-superficie/85 sticky top-0 z-30 border-b backdrop-blur-md">
        <div className="mx-auto flex h-16 max-w-6xl items-center gap-2 px-4 sm:px-6">
          <Link to="/" aria-label="Goltime, ir al inicio" className="mr-2 shrink-0 sm:mr-6">
            <Logo />
          </Link>
          <nav aria-label="Principal" className="hidden items-center gap-1 md:flex">
            {enlaces.map((e) => (
              <NavLink key={e.a} to={e.a} className={estiloEnlace}>
                {e.texto}
              </NavLink>
            ))}
          </nav>
          <div className="ml-auto flex items-center gap-1.5 sm:gap-2">
            {usuario?.rol !== 'ADMIN' && (
              <EnlaceBoton to="/reservar" tamano="sm" className="hidden sm:inline-flex">
                Reservar cancha
              </EnlaceBoton>
            )}
            {usuario?.rol === 'ADMIN' && (
              <EnlaceBoton
                to="/admin"
                tamano="sm"
                variante="secundario"
                className="hidden sm:inline-flex"
              >
                Ir al panel
              </EnlaceBoton>
            )}
            {usuario ? (
              <>
                <CampanaNotificaciones />
                <MenuUsuario />
              </>
            ) : (
              <>
                <EnlaceBoton
                  to="/ingresar"
                  tamano="sm"
                  variante="fantasma"
                  className="hidden sm:inline-flex"
                >
                  Ingresar
                </EnlaceBoton>
                <EnlaceBoton
                  to="/registro"
                  tamano="sm"
                  variante="secundario"
                  className="hidden sm:inline-flex"
                >
                  Crear cuenta
                </EnlaceBoton>
              </>
            )}
            <button
              type="button"
              className="hover:bg-tenue rounded-boton inline-flex size-9 items-center justify-center md:hidden"
              aria-label={menuAbierto ? 'Cerrar menú' : 'Abrir menú'}
              aria-expanded={menuAbierto}
              onClick={() => setMenuAbierto((a) => !a)}
            >
              {menuAbierto ? (
                <X className="size-5" aria-hidden />
              ) : (
                <Menu className="size-5" aria-hidden />
              )}
            </button>
          </div>
        </div>
        {menuAbierto && (
          <nav
            aria-label="Principal móvil"
            className="border-borde flex flex-col gap-1 border-t px-4 py-3 md:hidden"
            onClick={() => setMenuAbierto(false)}
          >
            {usuario?.rol !== 'ADMIN' && (
              <NavLink to="/reservar" className={estiloEnlace}>
                Reservar cancha
              </NavLink>
            )}
            {usuario?.rol === 'ADMIN' && (
              <NavLink to="/admin" className={estiloEnlace}>
                Panel de administración
              </NavLink>
            )}
            {enlaces.map((e) => (
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
                  Crear cuenta
                </NavLink>
              </>
            )}
          </nav>
        )}
      </header>
      <main
        className={cn(
          'mx-auto w-full flex-1 px-4 sm:px-6',
          esAcceso
            ? 'flex max-w-6xl items-start justify-center py-10 sm:py-16'
            : 'max-w-6xl py-8 sm:py-10',
        )}
      >
        <Outlet />
      </main>
      <footer className="border-borde bg-superficie border-t">
        <div className="text-texto-suave mx-auto flex max-w-6xl flex-col items-center justify-between gap-3 px-4 py-6 text-sm sm:flex-row sm:px-6">
          <div className="flex items-center gap-2">
            <img src="/favicon.svg" alt="" className="size-5" />
            <span>Goltime · Fútbol sintético en Neiva</span>
          </div>
          <span className="text-xs">Universidad Surcolombiana · INNOVASOFT IX</span>
        </div>
      </footer>
    </div>
  )
}
