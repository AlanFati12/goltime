import { Navigate, Outlet, useLocation } from 'react-router-dom'

import { useSesion, type Rol } from './almacenSesion'

/** Protege rutas por sesión, rol y perfil completo. La autorización real siempre se vuelve a verificar en el backend. */
export function RutaProtegida({
  rol,
  permitirIncompleto = false,
}: {
  rol?: Rol
  /** Solo la propia pantalla de «Completa tu perfil» la usa: ahí sí puede estar un cliente con el perfil incompleto. */
  permitirIncompleto?: boolean
}) {
  const usuario = useSesion((s) => s.usuario)
  const ubicacion = useLocation()

  if (!usuario) return <Navigate to="/ingresar" state={{ desde: ubicacion.pathname }} replace />
  if (rol && usuario.rol !== rol) return <Navigate to="/" replace />
  // Un cliente que entró con Google y aún no dio su WhatsApp ni sus apellidos pasa primero por completar el perfil.
  if (!permitirIncompleto && usuario.rol === 'CLIENTE' && !usuario.perfilCompleto) {
    return <Navigate to="/completar-perfil" state={{ desde: ubicacion.pathname }} replace />
  }
  return <Outlet />
}
