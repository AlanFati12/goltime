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
        className="text-texto-suave hover:text-texto flex items-center gap-2 text-sm font-medium"
      >
        <svg
          viewBox="0 0 24 24"
          className={cn('size-4 transition-transform', abierto && 'rotate-90')}
          fill="none"
          stroke="currentColor"
          strokeWidth="2.5"
          strokeLinecap="round"
          strokeLinejoin="round"
          aria-hidden
        >
          <path d="m9 18 6-6-6-6" />
        </svg>
        {titulo}
        {resumen && !abierto && <span className="text-xs font-normal">{resumen}</span>}
      </button>
      {abierto && <div className="mt-3">{children}</div>}
    </div>
  )
}
