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
  const { data } = useNotificaciones(0, 6)
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
        className="hover:bg-primario-claro text-texto relative inline-flex size-9 items-center justify-center rounded-full"
      >
        <svg
          viewBox="0 0 24 24"
          className="size-5"
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
          strokeLinecap="round"
          strokeLinejoin="round"
          aria-hidden
        >
          <path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9" />
          <path d="M10.3 21a1.94 1.94 0 0 0 3.4 0" />
        </svg>
        {noLeidas > 0 && (
          <span className="bg-peligro absolute -top-0.5 -right-0.5 flex min-w-5 items-center justify-center rounded-full px-1 text-[0.65rem] font-bold text-white">
            {noLeidas > 99 ? '99+' : noLeidas}
          </span>
        )}
      </button>

      {abierta && (
        <div className="border-borde bg-superficie rounded-tarjeta absolute right-0 z-40 mt-2 w-80 max-w-[calc(100vw-2rem)] overflow-hidden border shadow-lg">
          <div className="border-borde flex items-center justify-between border-b px-3 py-2 text-sm">
            <span className="font-semibold">Notificaciones</span>
            {noLeidas > 0 && (
              <button
                type="button"
                onClick={() => leerTodas.mutate()}
                className="text-primario text-xs underline"
              >
                Marcar todas como leídas
              </button>
            )}
          </div>
          {data?.contenido.length === 0 && (
            <p className="text-texto-suave px-3 py-6 text-center text-sm">
              No tienes notificaciones.
            </p>
          )}
          <ul className="max-h-80 overflow-y-auto">
            {data?.contenido.map((n) => (
              <li key={n.id}>
                <button
                  type="button"
                  onClick={() => abrir(n)}
                  className={cn(
                    'hover:bg-primario-claro w-full px-3 py-2 text-left text-sm',
                    !n.leida && 'bg-primario-claro/50',
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
                      <span className="text-texto-suave block text-xs">{n.mensaje}</span>
                      <span className="text-texto-suave block text-[0.7rem] capitalize">
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
            className="border-borde text-primario block border-t px-3 py-2 text-center text-sm underline"
          >
            Ver todas
          </Link>
        </div>
      )}
    </div>
  )
}
