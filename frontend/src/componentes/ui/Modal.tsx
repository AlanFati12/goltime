import { useEffect, useId, useRef, type ReactNode } from 'react'

interface PropsModal {
  abierto: boolean
  titulo: string
  alCerrar: () => void
  children: ReactNode
}

/** Diálogo modal accesible: cierra con Esc o al hacer clic fuera, y devuelve el foco al cerrar. */
export function Modal({ abierto, titulo, alCerrar, children }: PropsModal) {
  const idTitulo = useId()
  const contenedor = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!abierto) return
    const previo = document.activeElement as HTMLElement | null
    contenedor.current?.focus()
    const alTeclear = (e: KeyboardEvent) => e.key === 'Escape' && alCerrar()
    document.addEventListener('keydown', alTeclear)
    return () => {
      document.removeEventListener('keydown', alTeclear)
      previo?.focus()
    }
  }, [abierto, alCerrar])

  if (!abierto) return null
  return (
    <div
      className="fixed inset-0 z-50 flex items-end justify-center bg-black/50 p-4 sm:items-center"
      onMouseDown={(e) => e.target === e.currentTarget && alCerrar()}
    >
      <div
        ref={contenedor}
        role="dialog"
        aria-modal="true"
        aria-labelledby={idTitulo}
        tabIndex={-1}
        className="bg-superficie text-texto rounded-tarjeta max-h-[90vh] w-full max-w-lg overflow-y-auto p-6 shadow-xl outline-none"
      >
        <h2 id={idTitulo} className="mb-4 text-xl font-semibold">
          {titulo}
        </h2>
        {children}
      </div>
    </div>
  )
}
