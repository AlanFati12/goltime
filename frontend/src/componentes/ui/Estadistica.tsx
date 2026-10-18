import type { LucideIcon } from 'lucide-react'
import type { ReactNode } from 'react'

import { cn } from '@/lib/utils'

/** Indicador clave: etiqueta pequeña, valor grande y una nota opcional. */
export function Estadistica({
  etiqueta,
  valor,
  nota,
  icono: Icono,
  destacado,
  className,
}: {
  etiqueta: string
  valor: ReactNode
  nota?: ReactNode
  icono?: LucideIcon
  destacado?: boolean
  className?: string
}) {
  return (
    <div
      className={cn(
        'border-borde bg-superficie rounded-tarjeta shadow-suave flex flex-col gap-2 border p-5',
        destacado && 'border-acento/50 bg-amber-50/60',
        className,
      )}
    >
      <div className="flex items-center justify-between gap-2">
        <p className="text-texto-suave text-sm font-medium">{etiqueta}</p>
        {Icono && (
          <span className="bg-tenue text-texto-suave flex size-8 items-center justify-center rounded-lg">
            <Icono className="size-4" aria-hidden />
          </span>
        )}
      </div>
      <p className="font-titulo text-2xl font-bold tracking-tight">{valor}</p>
      {nota && <p className="text-texto-suave text-xs">{nota}</p>}
    </div>
  )
}
