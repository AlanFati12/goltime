import { Navigate, Outlet, useLocation } from 'react-router-dom'

import { useSesion, type Rol } from './almacenSesion'

/** Protege rutas por sesión y rol. La autorización real siempre se vuelve a verificar en el backend. */
export function RutaProtegida({ rol }: { rol?: Rol }) {
  const usuario = useSesion((s) => s.usuario)
  const ubicacion = useLocation()

  if (!usuario) return <Navigate to="/ingresar" state={{ desde: ubicacion }} replace />
  if (rol && usuario.rol !== rol) return <Navigate to="/" replace />
  return <Outlet />
}
