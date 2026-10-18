import { useQueryClient } from '@tanstack/react-query'
import { NavLink, Outlet, useNavigate } from 'react-router-dom'

import { nombreCompleto, useSesion } from '@/caracteristicas/autenticacion/almacenSesion'
import { salir } from '@/caracteristicas/autenticacion/servicioAutenticacion'
import { CampanaNotificaciones } from '@/caracteristicas/notificaciones/CampanaNotificaciones'
import { Boton } from '@/componentes/ui/Boton'
import { cn } from '@/lib/utils'

const enlaces = [
  { a: '/admin', texto: 'Inicio', fin: true },
  { a: '/admin/agenda', texto: 'Agenda' },
  { a: '/admin/reservas', texto: 'Reservas' },
  { a: '/admin/estadisticas', texto: 'Estadísticas' },
  { a: '/admin/series', texto: 'Recurrentes' },
  { a: '/admin/bloqueos', texto: 'Bloqueos' },
  { a: '/admin/torneos', texto: 'Torneos' },
  { a: '/admin/promociones', texto: 'Promociones' },
  { a: '/admin/premios', texto: 'Premios' },
  { a: '/admin/equipos', texto: 'Equipos' },
  { a: '/admin/sedes', texto: 'Sedes' },
  { a: '/admin/canchas', texto: 'Canchas' },
  { a: '/admin/festivos', texto: 'Festivos' },
  { a: '/admin/clientes', texto: 'Clientes' },
  { a: '/admin/administradores', texto: 'Administradores' },
  { a: '/admin/auditoria', texto: 'Auditoría' },
]

/** Panel del administrador: barra lateral en escritorio y barra superior desplazable en móvil. */
export function LayoutAdmin() {
  const usuario = useSesion((s) => s.usuario)!
  const navegar = useNavigate()
  const clienteConsultas = useQueryClient()

  const cerrarSesion = async () => {
    await salir().catch(() => undefined)
    clienteConsultas.clear()
    navegar('/ingresar', { replace: true })
  }

  return (
    <div className="flex min-h-screen flex-col md:flex-row">
      <aside className="bg-primario-oscuro text-white md:w-60 md:shrink-0">
        <div className="flex items-center justify-between px-4 py-3 md:flex-col md:items-start md:gap-1 md:py-5">
          <span className="font-titulo text-xl font-bold">Goltime</span>
          <span className="text-xs text-white/70">Panel de administración</span>
        </div>
        <nav
          aria-label="Administración"
          className="flex gap-1 overflow-x-auto px-2 pb-2 md:flex-col md:px-3"
        >
          {enlaces.map((e) => (
            <NavLink
              key={e.a}
              to={e.a}
              end={e.fin}
              className={({ isActive }) =>
                cn(
                  'rounded-boton px-3 py-2 text-sm font-medium whitespace-nowrap hover:bg-white/10',
                  isActive && 'bg-white/20',
                )
              }
            >
              {e.texto}
            </NavLink>
          ))}
        </nav>
      </aside>
      <div className="flex min-w-0 flex-1 flex-col">
        <header className="border-borde bg-superficie flex items-center justify-end gap-3 border-b px-4 py-3 text-sm">
          <CampanaNotificaciones />
          <span className="text-texto-suave truncate">{nombreCompleto(usuario)}</span>
          <Boton tamano="sm" variante="secundario" onClick={cerrarSesion}>
            Salir
          </Boton>
        </header>
        <main className="flex-1 p-4 md:p-6">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
