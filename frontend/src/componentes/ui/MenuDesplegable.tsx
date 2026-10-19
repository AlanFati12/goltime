import { useEffect, useRef, useState, type ReactNode } from 'react'

import { cn } from '@/lib/utils'

/** Botón que abre un panel flotante; se cierra al hacer clic fuera, con Escape o al elegir una opción. */
export function MenuDesplegable({
  etiqueta,
  boton,
  className,
  claseBoton = 'hover:bg-tenue flex items-center gap-2 rounded-full py-1 pr-2 pl-1',
  children,
}: {
  etiqueta: string
  boton: ReactNode
  className?: string
  /** Aspecto del botón que abre el menú; por defecto, el del avatar de la cuenta. */
  claseBoton?: string
  children: ReactNode
}) {
  const [abierto, setAbierto] = useState(false)
  const contenedor = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!abierto) return
    const alClicFuera = (e: MouseEvent) =>
      !contenedor.current?.contains(e.target as Node) && setAbierto(false)
    const alTeclear = (e: KeyboardEvent) => e.key === 'Escape' && setAbierto(false)
    document.addEventListener('mousedown', alClicFuera)
    document.addEventListener('keydown', alTeclear)
    return () => {
      document.removeEventListener('mousedown', alClicFuera)
      document.removeEventListener('keydown', alTeclear)
    }
  }, [abierto])

  return (
    <div ref={contenedor} className="relative">
      <button
        type="button"
        aria-label={etiqueta}
        aria-expanded={abierto}
        aria-haspopup="menu"
        onClick={() => setAbierto((a) => !a)}
        className={claseBoton}
      >
        {boton}
      </button>
      {abierto && (
        <div
          role="menu"
          onClick={() => setAbierto(false)}
          className={cn(
            'border-borde bg-superficie shadow-flotante animate-menu absolute right-0 z-40 mt-2 w-64 origin-top-right rounded-2xl border p-1.5',
            className,
          )}
        >
          {children}
        </div>
      )}
    </div>
  )
}
