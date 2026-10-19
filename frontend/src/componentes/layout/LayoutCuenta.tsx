import { NavLink, Outlet } from 'react-router-dom'

import { nombreCompleto, useSesion } from '@/caracteristicas/autenticacion/almacenSesion'
import { cn } from '@/lib/utils'

import { SECCIONES_CUENTA } from './seccionesCuenta'

/** Área personal del cliente: tarjeta de perfil y menú lateral en escritorio, pestañas en móvil. */
export function LayoutCuenta() {
  const usuario = useSesion((s) => s.usuario)
  // El administrador también ve su perfil y notificaciones aquí, sin las secciones de cliente.
  const secciones =
    usuario?.rol === 'ADMIN'
      ? SECCIONES_CUENTA.filter((s) => s.a === '/perfil' || s.a === '/notificaciones')
      : SECCIONES_CUENTA

  return (
    <div className="flex flex-col gap-6 lg:flex-row lg:gap-10">
      <aside className="flex flex-col gap-4 lg:sticky lg:top-24 lg:w-64 lg:shrink-0 lg:self-start print:hidden">
        {usuario && (
          <div className="bg-noche relative isolate hidden overflow-hidden rounded-[1.5rem] p-5 text-white lg:block">
            <span
              className="bg-volt/30 absolute -top-10 -right-10 -z-10 size-32 rounded-full blur-2xl"
              aria-hidden
            />
            <span className="bg-volt text-noche font-titulo flex size-12 items-center justify-center rounded-full text-2xl font-extrabold uppercase">
              {nombreCompleto(usuario).trim().charAt(0)}
            </span>
            <p className="font-titulo mt-3 truncate text-2xl leading-none font-extrabold uppercase">
              {nombreCompleto(usuario)}
            </p>
            <p className="mt-1 truncate text-sm text-white/55">{usuario.correo}</p>
          </div>
        )}
        <nav
          aria-label="Mi cuenta"
          className="-mx-4 flex gap-1.5 overflow-x-auto px-4 pb-1 lg:mx-0 lg:flex-col lg:gap-1 lg:overflow-visible lg:px-0 lg:pb-0"
        >
          {secciones.map((s) => (
            <NavLink
              key={s.a}
              to={s.a}
              className={({ isActive }) =>
                cn(
                  'flex shrink-0 items-center gap-2.5 rounded-full px-4 py-2.5 text-sm font-semibold whitespace-nowrap transition-colors duration-200 lg:rounded-2xl',
                  isActive
                    ? 'bg-noche text-white'
                    : 'text-texto-suave hover:bg-superficie hover:text-texto',
                )
              }
            >
              {({ isActive }) => (
                <>
                  <s.icono
                    className={cn('size-4', isActive ? 'text-volt-vivo' : 'text-texto-suave')}
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
