import { ChevronRight } from 'lucide-react'
import { useState, type ReactNode } from 'react'

import { cn } from '@/lib/utils'

/** Sección que oculta su contenido hasta que se pide: para filtros avanzados y bloques secundarios. */
export function Plegable({
  titulo,
  resumen,
  abiertoInicial = false,
  className,
  children,
}: {
  titulo: string
  resumen?: ReactNode
  abiertoInicial?: boolean
  className?: string
  children: ReactNode
}) {
  const [abierto, setAbierto] = useState(abiertoInicial)
  return (
    <div className={className}>
      <button
        type="button"
        onClick={() => setAbierto((a) => !a)}
        aria-expanded={abierto}
        className="text-texto-suave hover:text-texto flex items-center gap-1.5 text-sm font-medium"
      >
        <ChevronRight
          className={cn('size-4 transition-transform duration-200', abierto && 'rotate-90')}
          aria-hidden
        />
        {titulo}
        {resumen && !abierto && <span className="text-xs font-normal">{resumen}</span>}
      </button>
      {abierto && <div className="animate-menu mt-3">{children}</div>}
    </div>
  )
}
