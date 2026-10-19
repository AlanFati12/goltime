import { cn } from '@/lib/utils'

export interface DatosPestana<T extends string> {
  id: T
  texto: string
  /** Número pequeño junto al título (por ejemplo, solicitudes por atender). */
  conteo?: number
}

/** Pestañas con subrayado: separan secciones largas para mostrar una sola a la vez. */
export function Pestanas<T extends string>({
  etiqueta,
  pestanas,
  actual,
  alCambiar,
}: {
  etiqueta: string
  pestanas: DatosPestana<T>[]
  actual: T
  alCambiar: (id: T) => void
}) {
  return (
    <div
      role="tablist"
      aria-label={etiqueta}
      className="border-borde flex gap-6 overflow-x-auto border-b"
    >
      {pestanas.map((p) => (
        <button
          key={p.id}
          type="button"
          role="tab"
          aria-selected={actual === p.id}
          onClick={() => alCambiar(p.id)}
          className={cn(
            '-mb-px flex items-center gap-2 border-b-[3px] pt-1 pb-3 text-[0.95rem] font-semibold whitespace-nowrap transition-colors duration-200',
            actual === p.id
              ? 'border-volt text-texto'
              : 'text-texto-suave hover:text-texto border-transparent',
          )}
        >
          {p.texto}
          {p.conteo != null && p.conteo > 0 && (
            <span
              className={cn(
                'rounded-full px-1.5 text-xs font-semibold',
                actual === p.id ? 'bg-noche text-volt-vivo' : 'bg-tenue text-texto-suave',
              )}
            >
              {p.conteo}
            </span>
          )}
        </button>
      ))}
    </div>
  )
}

/** Control segmentado: para alternar entre pocas vistas de lo mismo (día/semana, lista/tarjetas). */
export function Segmentado<T extends string>({
  etiqueta,
  opciones,
  actual,
  alCambiar,
}: {
  etiqueta: string
  opciones: { id: T; texto: string }[]
  actual: T
  alCambiar: (id: T) => void
}) {
  return (
    <div role="group" aria-label={etiqueta} className="bg-tenue inline-flex rounded-full p-1">
      {opciones.map((o) => (
        <button
          key={o.id}
          type="button"
          aria-pressed={actual === o.id}
          onClick={() => alCambiar(o.id)}
          className={cn(
            'rounded-full px-4 py-1.5 text-sm font-semibold transition-[background-color,color,box-shadow] duration-200',
            actual === o.id ? 'bg-noche text-white' : 'text-texto-suave hover:text-texto',
          )}
        >
          {o.texto}
        </button>
      ))}
    </div>
  )
}
