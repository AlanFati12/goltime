import { Footprints, Hand } from 'lucide-react'

import { cn } from '@/lib/utils'

import type { TipoEvento } from './tipos'

/** Balón dibujado con el mismo trazo de los iconos de la interfaz. */
export function IconoBalon({ className }: { className?: string }) {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      className={cn('size-4', className)}
      aria-hidden
    >
      <circle cx="12" cy="12" r="9" />
      <path d="m12 8 3.5 2.5-1.3 4h-4.4l-1.3-4Z" />
      <path d="M12 3v5M20.5 9.5l-5 1M17.5 19.5l-3.3-5M6.5 19.5l3.3-5M3.5 9.5l5 1" />
    </svg>
  )
}

/** Tarjeta de amonestación (amarilla o roja), inclinada como cuando el árbitro la muestra. */
export function IconoTarjeta({ roja, className }: { roja?: boolean; className?: string }) {
  return (
    <span
      aria-hidden
      className={cn(
        'inline-block h-4 w-3 shrink-0 rotate-[8deg] rounded-[2px] shadow-sm',
        roja ? 'bg-red-600' : 'bg-amber-400',
        className,
      )}
    />
  )
}

/** Icono de cada tipo de evento de la planilla. */
export function IconoEvento({ tipo, className }: { tipo: TipoEvento; className?: string }) {
  switch (tipo) {
    case 'GOL':
    case 'GOL_PENAL':
      return <IconoBalon className={cn('text-primario', className)} />
    case 'AUTOGOL':
      return <IconoBalon className={cn('text-peligro', className)} />
    case 'ASISTENCIA':
      return <Footprints className={cn('text-info size-4', className)} aria-hidden />
    case 'AMARILLA':
      return <IconoTarjeta className={className} />
    case 'ROJA':
      return <IconoTarjeta roja className={className} />
    case 'FALTA':
      return <Hand className={cn('text-texto-suave size-4', className)} aria-hidden />
  }
}
