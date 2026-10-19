import { ArrowLeft } from 'lucide-react'
import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'

/** Título de página con descripción corta y acciones a la derecha; da el mismo ritmo a todas las pantallas. */
export function EncabezadoPagina({
  titulo,
  descripcion,
  acciones,
  volver,
}: {
  titulo: ReactNode
  descripcion?: ReactNode
  acciones?: ReactNode
  /** Enlace de regreso que aparece sobre el título. */
  volver?: { a: string; texto: string }
}) {
  return (
    <div className="flex flex-col gap-3">
      {volver && (
        <Link
          to={volver.a}
          className="text-texto-suave hover:text-texto group inline-flex items-center gap-1.5 self-start text-sm font-semibold"
        >
          <ArrowLeft
            className="size-4 transition-transform duration-200 group-hover:-translate-x-0.5"
            aria-hidden
          />
          {volver.texto}
        </Link>
      )}
      <div className="flex flex-wrap items-end justify-between gap-x-6 gap-y-3">
        <div className="min-w-0">
          <h1 className="text-[2rem] leading-none font-extrabold uppercase sm:text-[2.6rem]">
            {titulo}
          </h1>
          {descripcion && <p className="text-texto-suave mt-2 max-w-2xl">{descripcion}</p>}
        </div>
        {acciones && <div className="flex flex-wrap items-center gap-2">{acciones}</div>}
      </div>
    </div>
  )
}
