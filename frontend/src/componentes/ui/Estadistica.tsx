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
        'border-borde/80 bg-superficie rounded-tarjeta flex flex-col gap-2 border p-5',
        destacado &&
          'bg-noche border-noche [&_p.valor]:text-volt-vivo text-white [&_p]:text-white/70',
        className,
      )}
    >
      <div className="flex items-center justify-between gap-2">
        <p className="text-texto-suave text-sm font-medium">{etiqueta}</p>
        {Icono && (
          <span className="bg-primario-claro text-primario flex size-9 items-center justify-center rounded-full">
            <Icono className="size-4" aria-hidden />
          </span>
        )}
      </div>
      <p className="valor font-titulo cifras text-[2.1rem] leading-none font-extrabold">{valor}</p>
      {nota && <p className="text-texto-suave text-xs">{nota}</p>}
    </div>
  )
}
