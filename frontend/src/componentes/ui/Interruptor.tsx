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
        'relative inline-flex h-6 w-11 items-center rounded-full transition-colors disabled:opacity-50',
        activo ? 'bg-primario' : 'bg-borde',
      )}
    >
      <span
        className={cn(
          'inline-block size-5 rounded-full bg-white shadow transition-transform',
          activo ? 'translate-x-5' : 'translate-x-0.5',
        )}
      />
    </button>
  )
}
