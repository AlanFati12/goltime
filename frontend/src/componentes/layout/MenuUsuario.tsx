import { useQueryClient } from '@tanstack/react-query'
import { Link, useNavigate } from 'react-router-dom'

import { nombreCompleto, useSesion } from '@/caracteristicas/autenticacion/almacenSesion'
import { salir } from '@/caracteristicas/autenticacion/servicioAutenticacion'
import { MenuDesplegable } from '@/componentes/ui/MenuDesplegable'
import { estiloItemMenu } from '@/componentes/ui/estilosMenu'

const ENLACES_CLIENTE = [
  { a: '/mis-reservas', texto: 'Mis reservas' },
  { a: '/mis-premios', texto: 'Mis premios' },
  { a: '/mi-cuenta', texto: 'Más de mi cuenta' },
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
            className="bg-primario-claro text-primario-oscuro flex size-8 items-center justify-center rounded-full text-sm font-semibold"
            aria-hidden
          >
            {nombre.trim().charAt(0).toUpperCase()}
          </span>
          <span className="hidden max-w-32 truncate text-sm font-medium lg:inline">{nombre}</span>
        </>
      }
    >
      <div className="border-borde mb-1 border-b px-3 pt-1 pb-2">
        <p className="truncate text-sm font-semibold">{nombre}</p>
        <p className="text-texto-suave text-xs">{esAdmin ? 'Administrador' : 'Cliente'}</p>
      </div>
      {esAdmin && (
        <Link role="menuitem" to="/admin" className={estiloItemMenu}>
          Panel de administración
        </Link>
      )}
      {!esAdmin &&
        ENLACES_CLIENTE.map((e) => (
          <Link key={e.a} role="menuitem" to={e.a} className={estiloItemMenu}>
            {e.texto}
          </Link>
        ))}
      <Link role="menuitem" to="/perfil" className={estiloItemMenu}>
        Mi perfil
      </Link>
      <button
        type="button"
        role="menuitem"
        onClick={cerrarSesion}
        className={`${estiloItemMenu} text-peligro`}
      >
        Cerrar sesión
      </button>
    </MenuDesplegable>
  )
}
