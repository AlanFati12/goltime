import { useEffect, type ReactNode } from 'react'

import { Cargando } from '@/componentes/ui/Cargando'

import { useSesion } from './almacenSesion'
import { restaurarSesion } from './servicioSesion'

/** Al abrir la app intenta recuperar la sesión con la cookie de refresh antes de pintar las rutas. */
export function ProveedorSesion({ children }: { children: ReactNode }) {
  const inicializada = useSesion((s) => s.inicializada)

  useEffect(() => {
    if (!useSesion.getState().inicializada) void restaurarSesion()
  }, [])

  if (!inicializada) return <Cargando texto="Cargando Goltime…" className="justify-center" />
  return children
}
