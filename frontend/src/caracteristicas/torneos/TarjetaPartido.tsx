import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'

import { Insignia } from '@/componentes/ui/Insignia'
import { Tarjeta } from '@/componentes/ui/Tarjeta'
import { formatearFechaHora } from '@/lib/formato'
import { cn } from '@/lib/utils'

import { ETIQUETA_ESTADO_PARTIDO } from './etiquetas'
import type { Partido } from './tipos'
import { marcador, nombreLado, textoPenales, textoRonda } from './utilidades'

interface Props {
  partido: Partido
  /** Ruta del detalle del partido (pública o de administración). */
  ruta: string
  /** Acciones extra (administración). */
  acciones?: ReactNode
  mostrarTorneo?: boolean
}

/** Tarjeta de un partido: rivales, marcador, fecha y cancha. */
export function TarjetaPartido({ partido: p, ruta, acciones, mostrarTorneo }: Props) {
  const gana = (id: number | null | undefined) =>
    p.ganadorInscripcionId != null && p.ganadorInscripcionId === id
  return (
    <Tarjeta className="flex flex-col gap-2 p-4">
      <div className="text-texto-suave flex flex-wrap items-center justify-between gap-2 text-xs">
        <span>{mostrarTorneo ? `${p.torneo} · ${textoRonda(p)}` : textoRonda(p)}</span>
        <Insignia
          tono={
            p.estado === 'FINALIZADO' || p.estado === 'WO'
              ? 'verde'
              : p.estado === 'APLAZADO'
                ? 'ambar'
                : 'gris'
          }
        >
          {ETIQUETA_ESTADO_PARTIDO[p.estado]}
        </Insignia>
      </div>
      <Link to={ruta} className="grid grid-cols-[1fr_auto_1fr] items-center gap-3 hover:opacity-90">
        <span
          className={cn(
            'text-right font-medium',
            gana(p.local?.inscripcionId) && 'text-primario-oscuro font-bold',
          )}
        >
          {nombreLado(p.local)}
        </span>
        <span className="bg-primario-claro rounded-boton min-w-16 px-3 py-1 text-center text-lg font-bold">
          {marcador(p)}
        </span>
        <span
          className={cn(
            'font-medium',
            gana(p.visitante?.inscripcionId) && 'text-primario-oscuro font-bold',
          )}
        >
          {nombreLado(p.visitante)}
        </span>
      </Link>
      <div className="text-texto-suave flex flex-wrap items-center justify-between gap-2 text-xs">
        <span>
          {p.inicio
            ? `${formatearFechaHora(p.inicio)}${p.cancha ? ` · ${p.cancha}` : ''}`
            : 'Fecha por definir'}
          {textoPenales(p) && ` · ${textoPenales(p)}`}
        </span>
        {acciones}
      </div>
    </Tarjeta>
  )
}
