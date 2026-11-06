import { Bell, BellOff } from 'lucide-react'
import { useEffect, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

import { useSesion } from '@/caracteristicas/autenticacion/almacenSesion'
import {
  useAccionesNotificaciones,
  useContadorNotificaciones,
  useNotificaciones,
} from '@/caracteristicas/reservas/consultas'
import type { Notificacion } from '@/caracteristicas/reservas/tipos'
import { formatearFechaHora } from '@/lib/formato'
import { cn } from '@/lib/utils'

/** A dónde lleva una notificación según su entidad y el rol del usuario. */
export function rutaDeNotificacion(
  n: Pick<Notificacion, 'entidad' | 'entidadId'>,
  rol: 'ADMIN' | 'CLIENTE',
): string | null {
  if (n.entidad === 'reserva' && n.entidadId)
    return rol === 'ADMIN' ? `/admin/reservas/${n.entidadId}` : `/mis-reservas/${n.entidadId}`
  return null
}

/** Campana con contador de no leídas y desplegable con las más recientes. */
export function CampanaNotificaciones() {
  const usuario = useSesion((s) => s.usuario)
  const navegar = useNavigate()
  const [abierta, setAbierta] = useState(false)
  const contenedor = useRef<HTMLDivElement>(null)
  const { data: noLeidas = 0 } = useContadorNotificaciones(!!usuario)
  // La lista solo se pide al abrir el desplegable y con sesión; el contador sí se consulta siempre (con sesión).
  const { data } = useNotificaciones(0, 6, !!usuario && abierta)
  const { leer, leerTodas } = useAccionesNotificaciones()

  useEffect(() => {
    if (!abierta) return
    const alClicFuera = (e: MouseEvent) =>
      !contenedor.current?.contains(e.target as Node) && setAbierta(false)
    const alTeclear = (e: KeyboardEvent) => e.key === 'Escape' && setAbierta(false)
    document.addEventListener('mousedown', alClicFuera)
    document.addEventListener('keydown', alTeclear)
    return () => {
      document.removeEventListener('mousedown', alClicFuera)
      document.removeEventListener('keydown', alTeclear)
    }
  }, [abierta])

  if (!usuario) return null

  const abrir = (n: Notificacion) => {
    if (!n.leida) leer.mutate(n.id)
    setAbierta(false)
    const ruta = rutaDeNotificacion(n, usuario.rol)
    if (ruta) navegar(ruta)
  }

  return (
    <div ref={contenedor} className="relative">
      <button
        type="button"
        onClick={() => setAbierta((a) => !a)}
        aria-label={noLeidas > 0 ? `Notificaciones, ${noLeidas} sin leer` : 'Notificaciones'}
        aria-expanded={abierta}
        className="hover:bg-tenue text-texto-suave hover:text-texto relative inline-flex size-9 items-center justify-center rounded-full"
      >
        <Bell className="size-5" aria-hidden />
        {noLeidas > 0 && (
          <span className="bg-peligro ring-superficie absolute top-0.5 right-0.5 flex h-4 min-w-4 items-center justify-center rounded-full px-1 text-[0.6rem] font-bold text-white ring-2">
            {noLeidas > 99 ? '99+' : noLeidas}
          </span>
        )}
      </button>

      {abierta && (
        <div className="border-borde bg-superficie rounded-tarjeta shadow-flotante absolute right-0 z-40 mt-2 w-96 max-w-[calc(100vw-2rem)] overflow-hidden border">
          <div className="border-borde flex items-center justify-between border-b px-4 py-3 text-sm">
            <span className="font-semibold">Notificaciones</span>
            {noLeidas > 0 && (
              <button
                type="button"
                onClick={() => leerTodas.mutate()}
                className="text-primario hover:text-primario-oscuro text-xs font-medium"
              >
                Marcar todas como leídas
              </button>
            )}
          </div>
          {data?.contenido.length === 0 && (
            <div className="text-texto-suave flex flex-col items-center gap-2 px-4 py-10 text-center text-sm">
              <BellOff className="size-5" aria-hidden />
              No tienes notificaciones.
            </div>
          )}
          <ul className="max-h-80 overflow-y-auto">
            {data?.contenido.map((n) => (
              <li key={n.id}>
                <button
                  type="button"
                  onClick={() => abrir(n)}
                  className={cn(
                    'hover:bg-tenue border-borde/60 w-full border-b px-4 py-3 text-left text-sm',
                    !n.leida && 'bg-primario-claro/40',
                  )}
                >
                  <span className="flex items-start gap-2">
                    {!n.leida && (
                      <span
                        className="bg-primario mt-1.5 size-2 shrink-0 rounded-full"
                        aria-label="Sin leer"
                      />
                    )}
                    <span>
                      <span className="block font-medium">{n.titulo}</span>
                      <span className="text-texto-suave mt-0.5 block text-xs">{n.mensaje}</span>
                      <span className="text-texto-suave/80 mt-1 block text-[0.7rem] capitalize">
                        {formatearFechaHora(n.creadoEn)}
                      </span>
                    </span>
                  </span>
                </button>
              </li>
            ))}
          </ul>
          <Link
            to="/notificaciones"
            onClick={() => setAbierta(false)}
            className="text-primario hover:bg-tenue block px-4 py-2.5 text-center text-sm font-medium"
          >
            Ver todas
          </Link>
        </div>
      )}
    </div>
  )
}
