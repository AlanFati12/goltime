import { useQueryClient } from '@tanstack/react-query'
import { Link, Outlet, useNavigate } from 'react-router-dom'

import { useSesion, nombreCompleto } from '@/caracteristicas/autenticacion/almacenSesion'
import { salir } from '@/caracteristicas/autenticacion/servicioAutenticacion'
import { CampanaNotificaciones } from '@/caracteristicas/notificaciones/CampanaNotificaciones'
import { Boton } from '@/componentes/ui/Boton'
import { EnlaceBoton } from '@/componentes/ui/EnlaceBoton'

export function LayoutPublico() {
  const usuario = useSesion((s) => s.usuario)
  const navegar = useNavigate()
  const clienteConsultas = useQueryClient()

  const cerrarSesion = async () => {
    await salir().catch(() => undefined)
    clienteConsultas.clear()
    navegar('/', { replace: true })
  }

  return (
    <div className="flex min-h-screen flex-col">
      <header className="border-borde bg-superficie border-b">
        <div className="mx-auto flex max-w-6xl items-center justify-between gap-4 px-4 py-3">
          <Link to="/" aria-label="Goltime, ir al inicio">
            <img src="/logo.svg" alt="Goltime" className="h-9" />
          </Link>
          <nav aria-label="Principal" className="flex items-center gap-3 text-sm font-medium">
            <Link to="/sedes" className="text-texto-suave hover:text-primario">
              Sedes
            </Link>
            <Link to="/torneos" className="text-texto-suave hover:text-primario">
              Torneos
            </Link>
            <Link to="/promociones" className="text-texto-suave hover:text-primario">
              Promociones
            </Link>
            <Link to="/reservar" className="text-texto-suave hover:text-primario">
              Reservar
            </Link>
            {usuario?.rol === 'CLIENTE' && (
              <>
                <Link to="/mis-reservas" className="text-texto-suave hover:text-primario">
                  Mis reservas
                </Link>
                <Link to="/mis-premios" className="text-texto-suave hover:text-primario">
                  Mis premios
                </Link>
              </>
            )}
            {usuario ? (
              <>
                {usuario.rol === 'ADMIN' && (
                  <Link to="/admin" className="text-primario hover:underline">
                    Panel
                  </Link>
                )}
                <CampanaNotificaciones />
                <Link
                  to="/perfil"
                  className="text-texto-suave hover:text-primario max-w-40 truncate"
                >
                  {nombreCompleto(usuario)}
                </Link>
                <Boton tamano="sm" variante="secundario" onClick={cerrarSesion}>
                  Salir
                </Boton>
              </>
            ) : (
              <>
                <Link to="/ingresar" className="text-texto-suave hover:text-primario">
                  Ingresar
                </Link>
                <EnlaceBoton to="/registro" tamano="sm">
                  Registrarme
                </EnlaceBoton>
              </>
            )}
          </nav>
        </div>
      </header>
      <main className="mx-auto w-full max-w-6xl flex-1 px-4 py-8">
        <Outlet />
      </main>
      <footer className="border-borde text-texto-suave border-t py-4 text-center text-xs">
        Goltime · Universidad Surcolombiana · INNOVASOFT IX
      </footer>
    </div>
  )
}
