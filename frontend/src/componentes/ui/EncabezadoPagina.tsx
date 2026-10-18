import type { ReactNode } from 'react'

/** Título de página con descripción corta y acciones a la derecha; da el mismo ritmo a todas las pantallas. */
export function EncabezadoPagina({
  titulo,
  descripcion,
  acciones,
}: {
  titulo: string
  descripcion?: string
  acciones?: ReactNode
}) {
  return (
    <div className="flex flex-wrap items-end justify-between gap-x-6 gap-y-3">
      <div className="min-w-0">
        <h1 className="text-2xl font-bold sm:text-3xl">{titulo}</h1>
        {descripcion && <p className="text-texto-suave mt-1 max-w-2xl text-sm">{descripcion}</p>}
      </div>
      {acciones && <div className="flex flex-wrap items-center gap-2">{acciones}</div>}
    </div>
  )
}
