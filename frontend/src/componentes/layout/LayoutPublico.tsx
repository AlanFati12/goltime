import { useIsFetching } from '@tanstack/react-query'
import { ArrowUpRight, Menu, X } from 'lucide-react'
import { useEffect, useState, type CSSProperties } from 'react'
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

/** Rutas de acceso: se muestran a pantalla completa con su propia composición. */
const RUTAS_ACCESO = [
  '/ingresar',
  '/registro',
  '/olvide-clave',
  '/restablecer-clave',
  '/verificar-correo',
]

/** Fotografías usadas en el sitio (licencia Unsplash); su crédito va en el pie. */
const CREDITOS_FOTOS = [
  'Tahamie Farooqui',
  'Mantis Saywhat',
  'Izuddin Helmi Adnan',
  'Michal Balog',
  'NO DIRECTION',
  'My Profit Tutor',
  'Fachry Zella Devandra',
  'Tim Winkler',
]

/** Línea fina en la parte superior mientras hay consultas en curso. */
function BarraCarga() {
  const cargando = useIsFetching() > 0
  return (
    <div
      aria-hidden
      className={cn(
        'pointer-events-none fixed inset-x-0 top-0 z-[70] h-[3px] overflow-hidden transition-opacity duration-300',
        cargando ? 'opacity-100' : 'opacity-0',
      )}
    >
      <div className="bg-volt animate-barrido h-full w-1/3 shadow-[0_0_12px_var(--color-volt)]" />
    </div>
  )
}

export function LayoutPublico() {
  const usuario = useSesion((s) => s.usuario)
  const [menuAbierto, setMenuAbierto] = useState(false)
  const [arriba, setArriba] = useState(true)
  const { pathname } = useLocation()
  const esCliente = usuario && usuario.rol !== 'ADMIN'
  const enlaces = esCliente ? [...ENLACES, ...ENLACES_CLIENTE] : ENLACES
  const esAcceso = RUTAS_ACCESO.includes(pathname)
  const esInicio = pathname === '/'
  // En la portada la barra flota transparente sobre la foto hasta que se hace scroll.
  const transparente = esInicio && arriba && !menuAbierto

  useEffect(() => {
    window.scrollTo(0, 0)
  }, [pathname])

  useEffect(() => {
    const alDesplazar = () => setArriba(window.scrollY < 24)
    alDesplazar()
    window.addEventListener('scroll', alDesplazar, { passive: true })
    return () => window.removeEventListener('scroll', alDesplazar)
  }, [])

  useEffect(() => {
    document.body.style.overflow = menuAbierto ? 'hidden' : ''
    return () => {
      document.body.style.overflow = ''
    }
  }, [menuAbierto])

  const estiloEnlace = ({ isActive }: { isActive: boolean }) =>
    cn(
      'relative rounded-full px-3.5 py-2 text-[0.95rem] font-semibold transition-colors duration-200',
      transparente
        ? isActive
          ? 'text-white'
          : 'text-white/75 hover:text-white'
        : isActive
          ? 'text-texto'
          : 'text-texto-suave hover:text-texto',
      isActive &&
        'after:bg-volt after:absolute after:inset-x-3.5 after:-bottom-0.5 after:h-[3px] after:rounded-full',
    )

  return (
    <div className="flex min-h-screen flex-col">
      <BarraCarga />
      <a
        href="#contenido"
        className="bg-volt text-noche sr-only z-[80] rounded-full px-4 py-2 font-semibold focus:not-sr-only focus:fixed focus:top-3 focus:left-3"
      >
        Saltar al contenido
      </a>
      <header
        className={cn(
          'fixed inset-x-0 top-0 z-40 transition-[background-color,border-color,box-shadow] duration-300 print:hidden',
          transparente
            ? 'border-b border-transparent bg-transparent'
            : 'border-borde/70 border-b bg-white/90 backdrop-blur-md',
        )}
      >
        <div className="contenedor flex h-[72px] items-center gap-2">
          <Link to="/" aria-label="Goltime, ir al inicio" className="mr-3 shrink-0 sm:mr-8">
            <Logo claro={transparente} />
          </Link>
          <nav aria-label="Principal" className="hidden items-center gap-1 md:flex">
            {enlaces.map((e) => (
              <NavLink key={e.a} to={e.a} className={estiloEnlace}>
                {e.texto}
              </NavLink>
            ))}
          </nav>
          <div
            className={cn(
              'ml-auto flex items-center gap-1.5 sm:gap-2',
              // Solo los botones que abren la campana y el menú, no las opciones de adentro.
              transparente && '[&>div>button]:text-white/85 [&>div>button:hover]:bg-white/10',
            )}
          >
            {usuario?.rol !== 'ADMIN' && (
              <EnlaceBoton
                to="/reservar"
                tamano="sm"
                variante="volt"
                className="hidden sm:inline-flex"
              >
                Reservar cancha
              </EnlaceBoton>
            )}
            {usuario?.rol === 'ADMIN' && (
              <EnlaceBoton
                to="/admin"
                tamano="sm"
                variante={transparente ? 'claro' : 'secundario'}
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
                  variante={transparente ? 'claro' : 'fantasma'}
                  className="hidden sm:inline-flex"
                >
                  Ingresar
                </EnlaceBoton>
                <EnlaceBoton
                  to="/registro"
                  tamano="sm"
                  variante={transparente ? 'claro' : 'secundario'}
                  className="hidden lg:inline-flex"
                >
                  Crear cuenta
                </EnlaceBoton>
              </>
            )}
            <button
              type="button"
              className={cn(
                'presionable inline-flex size-10 items-center justify-center rounded-full md:hidden',
                transparente ? 'text-white hover:bg-white/10' : 'hover:bg-tenue',
              )}
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
      </header>

      {/* Menú móvil a pantalla completa, con los enlaces entrando escalonados. */}
      {menuAbierto && (
        <nav
          aria-label="Principal móvil"
          onClick={() => setMenuAbierto(false)}
          className="bg-noche animate-velo fixed inset-x-0 top-[72px] bottom-0 z-30 flex flex-col overflow-y-auto px-6 pt-8 pb-10 md:hidden"
        >
          <ul className="flex flex-col">
            {[
              ...(usuario?.rol !== 'ADMIN' ? [{ a: '/reservar', texto: 'Reservar cancha' }] : []),
              ...(usuario?.rol === 'ADMIN'
                ? [{ a: '/admin', texto: 'Panel de administración' }]
                : []),
              ...enlaces,
              ...(!usuario
                ? [
                    { a: '/ingresar', texto: 'Ingresar' },
                    { a: '/registro', texto: 'Crear cuenta' },
                  ]
                : []),
            ].map((e, i) => (
              <li
                key={e.a}
                className="animate-subir border-noche-borde border-b"
                style={{ animationDelay: `${60 + i * 55}ms` } as CSSProperties}
              >
                <NavLink
                  to={e.a}
                  className={({ isActive }) =>
                    cn(
                      'font-titulo flex items-center justify-between py-4 text-[2rem] leading-none font-extrabold uppercase',
                      isActive ? 'text-volt-vivo' : 'text-white',
                    )
                  }
                >
                  {e.texto}
                  <ArrowUpRight className="size-6 opacity-50" aria-hidden />
                </NavLink>
              </li>
            ))}
          </ul>
          <p className="mt-auto pt-10 text-sm text-white/50">Fútbol sintético en Neiva</p>
        </nav>
      )}

      <main
        id="contenido"
        className={cn(
          'flex-1',
          esInicio ? '' : esAcceso ? 'pt-[72px]' : 'contenedor pt-[104px] pb-16 sm:pt-[120px]',
        )}
      >
        <div key={pathname} className="animate-entrada-pagina">
          <Outlet />
        </div>
      </main>

      <PiePagina />
    </div>
  )
}

function PiePagina() {
  const usuario = useSesion((s) => s.usuario)
  return (
    <footer className="bg-noche relative overflow-hidden text-white print:hidden">
      <div className="contenedor grid gap-12 pt-20 pb-10 md:grid-cols-[1.4fr_1fr_1fr]">
        <div className="max-w-sm">
          <Logo claro />
          <p className="mt-5 text-white/65">
            Canchas sintéticas en Neiva para jugar de día o bajo los reflectores. Reserva en línea,
            sigue tus torneos y acumula premios.
          </p>
        </div>
        <div>
          <h2 className="text-volt-vivo text-lg font-bold uppercase">Explora</h2>
          <ul className="mt-4 flex flex-col gap-2.5">
            {[...ENLACES, { a: '/reservar', texto: 'Reservar' }].map((e) => (
              <li key={e.a}>
                <Link to={e.a} className="text-white/75 transition-colors hover:text-white">
                  {e.texto}
                </Link>
              </li>
            ))}
          </ul>
        </div>
        <div>
          <h2 className="text-volt-vivo text-lg font-bold uppercase">Tu cuenta</h2>
          <ul className="mt-4 flex flex-col gap-2.5">
            {(usuario
              ? usuario.rol === 'ADMIN'
                ? [{ a: '/admin', texto: 'Panel de administración' }]
                : [
                    { a: '/mis-reservas', texto: 'Mis reservas' },
                    { a: '/mis-premios', texto: 'Mis premios' },
                    { a: '/perfil', texto: 'Mi perfil' },
                  ]
              : [
                  { a: '/ingresar', texto: 'Ingresar' },
                  { a: '/registro', texto: 'Crear cuenta' },
                ]
            ).map((e) => (
              <li key={e.a}>
                <Link to={e.a} className="text-white/75 transition-colors hover:text-white">
                  {e.texto}
                </Link>
              </li>
            ))}
          </ul>
        </div>
      </div>

      {/* El nombre vuelve a todo lo ancho como cierre de la página. */}
      <div className="contenedor" aria-hidden>
        <p className="font-titulo -mb-[0.16em] text-[22vw] leading-[0.8] font-extrabold tracking-[-0.03em] text-white/[0.06] uppercase select-none lg:text-[17rem]">
          Goltime
        </p>
      </div>

      <div className="border-noche-borde border-t">
        <div className="contenedor flex flex-col gap-2 py-6 text-xs text-white/45 sm:flex-row sm:items-center sm:justify-between">
          <span>Universidad Surcolombiana · INNOVASOFT IX · 2026</span>
          <span>Fotografías: {CREDITOS_FOTOS.join(', ')} (Unsplash).</span>
        </div>
      </div>
    </footer>
  )
}
