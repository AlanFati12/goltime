import { cn } from '@/lib/utils'

interface PropsInterruptor {
  activo: boolean
  alCambiar: (activo: boolean) => void
  etiqueta: string
  deshabilitado?: boolean
}

export function Interruptor({ activo, alCambiar, etiqueta, deshabilitado }: PropsInterruptor) {
  return (
    <button
      type="button"
      role="switch"
      aria-checked={activo}
      aria-label={etiqueta}
      disabled={deshabilitado}
      onClick={() => alCambiar(!activo)}
      className={cn(
        'relative inline-flex h-6 w-11 shrink-0 cursor-pointer items-center rounded-full transition-colors duration-200 disabled:cursor-not-allowed disabled:opacity-50',
        activo ? 'bg-primario' : 'bg-slate-300',
      )}
    >
      <span
        className={cn(
          'inline-block size-5 rounded-full bg-white shadow transition-transform duration-300 ease-[var(--ease-salida)]',
          activo ? 'translate-x-5' : 'translate-x-0.5',
        )}
      />
    </button>
  )
}
