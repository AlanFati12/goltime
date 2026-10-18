import { X } from 'lucide-react'
import { useEffect, useId, useRef, type ReactNode } from 'react'

import { cn } from '@/lib/utils'

interface PropsModal {
  abierto: boolean
  titulo: string
  alCerrar: () => void
  children: ReactNode
  /** Ancho máximo del diálogo; por defecto mediano. */
  ancho?: 'md' | 'lg'
}

/** Diálogo modal accesible: cierra con Esc o al hacer clic fuera, y devuelve el foco al cerrar. */
export function Modal({ abierto, titulo, alCerrar, children, ancho = 'md' }: PropsModal) {
  const idTitulo = useId()
  const contenedor = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!abierto) return
    const previo = document.activeElement as HTMLElement | null
    contenedor.current?.focus()
    const alTeclear = (e: KeyboardEvent) => e.key === 'Escape' && alCerrar()
    document.addEventListener('keydown', alTeclear)
    const desbordeAnterior = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    return () => {
      document.removeEventListener('keydown', alTeclear)
      document.body.style.overflow = desbordeAnterior
      previo?.focus()
    }
  }, [abierto, alCerrar])

  if (!abierto) return null
  return (
    <div
      className="fixed inset-0 z-50 flex items-end justify-center bg-slate-900/40 p-0 backdrop-blur-[2px] sm:items-center sm:p-4"
      onMouseDown={(e) => e.target === e.currentTarget && alCerrar()}
    >
      <div
        ref={contenedor}
        role="dialog"
        aria-modal="true"
        aria-labelledby={idTitulo}
        tabIndex={-1}
        className={cn(
          'bg-superficie text-texto shadow-flotante max-h-[92vh] w-full overflow-y-auto rounded-t-2xl p-6 outline-none sm:rounded-2xl',
          ancho === 'lg' ? 'max-w-2xl' : 'max-w-lg',
        )}
      >
        <div className="mb-5 flex items-start justify-between gap-4">
          <h2 id={idTitulo} className="text-lg font-semibold">
            {titulo}
          </h2>
          <button
            type="button"
            onClick={alCerrar}
            aria-label="Cerrar diálogo"
            className="text-texto-suave hover:bg-tenue hover:text-texto -mt-1 -mr-2 rounded-lg p-1.5"
          >
            <X className="size-4" aria-hidden />
          </button>
        </div>
        {children}
      </div>
    </div>
  )
}
