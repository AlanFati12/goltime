import type { LucideIcon } from 'lucide-react'
import { Inbox } from 'lucide-react'
import type { ReactNode } from 'react'

import { cn } from '@/lib/utils'

/** Mensaje amable para listas sin resultados, con icono y una acción opcional. */
export function EstadoVacio({
  titulo,
  descripcion,
  icono: Icono = Inbox,
  accion,
  className,
}: {
  titulo: string
  descripcion?: ReactNode
  icono?: LucideIcon
  accion?: ReactNode
  className?: string
}) {
  return (
    <div
      className={cn(
        'border-borde bg-superficie rounded-tarjeta flex flex-col items-center gap-3 border border-dashed px-6 py-12 text-center',
        className,
      )}
    >
      <span className="bg-tenue text-texto-suave flex size-11 items-center justify-center rounded-full">
        <Icono className="size-5" aria-hidden />
      </span>
      <div>
        <p className="font-medium">{titulo}</p>
        {descripcion && <p className="text-texto-suave mt-1 text-sm">{descripcion}</p>}
      </div>
      {accion}
    </div>
  )
}
