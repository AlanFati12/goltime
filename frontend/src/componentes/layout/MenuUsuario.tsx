import { useQueryClient } from '@tanstack/react-query'
import {
  CalendarCheck,
  Gift,
  LayoutDashboard,
  LogOut,
  UserRound,
  UserCog,
  type LucideIcon,
} from 'lucide-react'
import { Link, useNavigate } from 'react-router-dom'

import { nombreCompleto, useSesion } from '@/caracteristicas/autenticacion/almacenSesion'
import { salir } from '@/caracteristicas/autenticacion/servicioAutenticacion'
import { MenuDesplegable } from '@/componentes/ui/MenuDesplegable'
import { estiloItemMenu } from '@/componentes/ui/estilosMenu'

const ENLACES_CLIENTE: { a: string; texto: string; icono: LucideIcon }[] = [
  { a: '/mis-reservas', texto: 'Mis reservas', icono: CalendarCheck },
  { a: '/mis-premios', texto: 'Mis premios', icono: Gift },
  { a: '/mi-cuenta', texto: 'Mi cuenta', icono: UserCog },
]

/** Menú de la persona con sesión: reúne sus accesos personales y el cierre de sesión. */
export function MenuUsuario() {
  const usuario = useSesion((s) => s.usuario)
  const navegar = useNavigate()
  const clienteConsultas = useQueryClient()
  if (!usuario) return null

  const nombre = nombreCompleto(usuario)
  const esAdmin = usuario.rol === 'ADMIN'

  const cerrarSesion = async () => {
    await salir().catch(() => undefined)
    clienteConsultas.clear()
    navegar(esAdmin ? '/ingresar' : '/', { replace: true })
  }

  return (
    <MenuDesplegable
      etiqueta="Menú de la cuenta"
      boton={
        <>
          <span
            className="bg-primario flex size-8 items-center justify-center rounded-full text-sm font-semibold text-white"
            aria-hidden
          >
            {nombre.trim().charAt(0).toUpperCase()}
          </span>
          <span className="hidden max-w-32 truncate text-sm font-medium lg:inline">{nombre}</span>
        </>
      }
    >
      <div className="border-borde mb-1 border-b px-3 pt-2 pb-3">
        <p className="truncate text-sm font-semibold">{nombre}</p>
        <p className="text-texto-suave truncate text-xs">{usuario.correo}</p>
      </div>
      {esAdmin && (
        <Link role="menuitem" to="/admin" className={estiloItemMenu}>
          <LayoutDashboard aria-hidden />
          Panel de administración
        </Link>
      )}
      {!esAdmin &&
        ENLACES_CLIENTE.map((e) => (
          <Link key={e.a} role="menuitem" to={e.a} className={estiloItemMenu}>
            <e.icono aria-hidden />
            {e.texto}
          </Link>
        ))}
      <Link role="menuitem" to="/perfil" className={estiloItemMenu}>
        <UserRound aria-hidden />
        Mi perfil
      </Link>
      <div className="border-borde my-1 border-t" />
      <button
        type="button"
        role="menuitem"
        onClick={cerrarSesion}
        className={`${estiloItemMenu} text-peligro [&_svg]:text-peligro`}
      >
        <LogOut aria-hidden />
        Cerrar sesión
      </button>
    </MenuDesplegable>
  )
}
