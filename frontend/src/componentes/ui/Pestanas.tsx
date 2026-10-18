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
      className="border-borde flex gap-1 overflow-x-auto border-b"
    >
      {pestanas.map((p) => (
        <button
          key={p.id}
          type="button"
          role="tab"
          aria-selected={actual === p.id}
          onClick={() => alCambiar(p.id)}
          className={cn(
            '-mb-px flex items-center gap-2 border-b-2 px-4 py-2.5 text-sm font-medium whitespace-nowrap transition-colors',
            actual === p.id
              ? 'border-primario text-primario-oscuro'
              : 'text-texto-suave hover:text-texto border-transparent',
          )}
        >
          {p.texto}
          {p.conteo != null && p.conteo > 0 && (
            <span className="bg-acento text-texto rounded-full px-2 text-xs font-bold">
              {p.conteo}
            </span>
          )}
        </button>
      ))}
    </div>
  )
}
