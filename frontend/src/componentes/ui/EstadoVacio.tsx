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
        'bg-superficie rounded-tarjeta flex flex-col items-center gap-4 px-6 py-14 text-center',
        className,
      )}
    >
      <span className="bg-primario-claro text-primario flex size-14 items-center justify-center rounded-full">
        <Icono className="size-6" aria-hidden />
      </span>
      <div>
        <p className="font-titulo text-xl font-bold uppercase">{titulo}</p>
        {descripcion && <p className="text-texto-suave mx-auto mt-1 max-w-md">{descripcion}</p>}
      </div>
      {accion}
    </div>
  )
}
