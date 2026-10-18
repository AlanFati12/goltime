import { NavLink, Outlet } from 'react-router-dom'

import { nombreCompleto, useSesion } from '@/caracteristicas/autenticacion/almacenSesion'
import { cn } from '@/lib/utils'

import { SECCIONES_CUENTA } from './seccionesCuenta'

/** Área personal del cliente: menú lateral fijo en escritorio y pestañas desplazables en móvil. */
export function LayoutCuenta() {
  const usuario = useSesion((s) => s.usuario)
  // El administrador también ve su perfil y notificaciones aquí, sin las secciones de cliente.
  const secciones =
    usuario?.rol === 'ADMIN'
      ? SECCIONES_CUENTA.filter((s) => s.a === '/perfil' || s.a === '/notificaciones')
      : SECCIONES_CUENTA

  return (
    <div className="flex flex-col gap-6 lg:flex-row lg:gap-10">
      <aside className="lg:w-56 lg:shrink-0 print:hidden">
        {usuario && (
          <div className="mb-5 hidden items-center gap-3 lg:flex">
            <span className="bg-primario flex size-10 items-center justify-center rounded-full font-semibold text-white">
              {nombreCompleto(usuario).trim().charAt(0).toUpperCase()}
            </span>
            <div className="min-w-0">
              <p className="truncate text-sm font-semibold">{nombreCompleto(usuario)}</p>
              <p className="text-texto-suave truncate text-xs">{usuario.correo}</p>
            </div>
          </div>
        )}
        <nav
          aria-label="Mi cuenta"
          className="-mx-4 flex gap-1 overflow-x-auto px-4 pb-1 lg:mx-0 lg:flex-col lg:overflow-visible lg:px-0 lg:pb-0"
        >
          {secciones.map((s) => (
            <NavLink
              key={s.a}
              to={s.a}
              className={({ isActive }) =>
                cn(
                  'rounded-boton flex shrink-0 items-center gap-2.5 px-3 py-2 text-sm font-medium whitespace-nowrap transition-colors',
                  isActive
                    ? 'bg-superficie text-texto shadow-suave ring-borde ring-1'
                    : 'text-texto-suave hover:bg-tenue hover:text-texto',
                )
              }
            >
              {({ isActive }) => (
                <>
                  <s.icono
                    className={cn('size-4', isActive ? 'text-primario' : 'text-texto-suave')}
                    aria-hidden
                  />
                  {s.texto}
                </>
              )}
            </NavLink>
          ))}
        </nav>
      </aside>
      <div className="min-w-0 flex-1">
        <Outlet />
      </div>
    </div>
  )
}
