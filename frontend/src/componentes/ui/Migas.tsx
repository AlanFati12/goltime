import { ChevronRight } from 'lucide-react'
import { Fragment } from 'react'
import { Link } from 'react-router-dom'

/** Ruta de navegación (breadcrumbs); el último elemento es la página actual. */
export function Migas({ items }: { items: { texto: string; a?: string }[] }) {
  return (
    <nav aria-label="Ruta" className="text-texto-suave flex flex-wrap items-center gap-1 text-sm">
      {items.map((it, i) => (
        <Fragment key={i}>
          {i > 0 && <ChevronRight className="size-3.5" aria-hidden />}
          {it.a ? (
            <Link to={it.a} className="hover:text-texto">
              {it.texto}
            </Link>
          ) : (
            <span className="text-texto font-medium">{it.texto}</span>
          )}
        </Fragment>
      ))}
    </nav>
  )
}
