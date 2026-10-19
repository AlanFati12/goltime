import { ChevronLeft, ChevronRight } from 'lucide-react'

import { Boton } from './Boton'

/** Controles de página anterior/siguiente con el resumen de la posición actual. */
export function Paginacion({
  pagina,
  totalPaginas,
  totalElementos,
  unidad,
  alCambiar,
}: {
  /** Página actual, empezando en 0. */
  pagina: number
  totalPaginas: number
  totalElementos?: number
  /** Nombre en plural de lo que se lista (ej. "reservas"). */
  unidad?: string
  alCambiar: (pagina: number) => void
}) {
  if (totalPaginas <= 1) return null
  return (
    <div className="flex flex-wrap items-center justify-between gap-3 text-sm">
      <span className="text-texto-suave">
        Página <strong className="text-texto font-medium">{pagina + 1}</strong> de {totalPaginas}
        {totalElementos != null && unidad && ` · ${totalElementos} ${unidad}`}
      </span>
      <div className="flex gap-2">
        <Boton
          tamano="sm"
          variante="secundario"
          disabled={pagina === 0}
          onClick={() => alCambiar(pagina - 1)}
        >
          <ChevronLeft aria-hidden />
          Anterior
        </Boton>
        <Boton
          tamano="sm"
          variante="secundario"
          disabled={pagina + 1 >= totalPaginas}
          onClick={() => alCambiar(pagina + 1)}
        >
          Siguiente
          <ChevronRight aria-hidden />
        </Boton>
      </div>
    </div>
  )
}
