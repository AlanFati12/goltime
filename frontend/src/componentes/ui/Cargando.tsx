import { MarcaGoltime } from '@/componentes/layout/Logo'
import { cn } from '@/lib/utils'

/** Indicador de carga con la marca: las agujas del reloj giran dentro del círculo central. */
export function Cargando({
  texto = 'Cargando…',
  className,
}: {
  texto?: string
  className?: string
}) {
  return (
    <div
      role="status"
      className={cn(
        'text-texto-suave flex flex-col items-center justify-center gap-3 py-14 text-sm font-medium',
        className,
      )}
    >
      <MarcaGoltime animado className="size-10" />
      {texto}
    </div>
  )
}

/** Bloque gris animado que ocupa el lugar del contenido mientras llega. */
export function Esqueleto({ className }: { className?: string }) {
  return (
    <div aria-hidden className={cn('bg-tenue relative overflow-hidden rounded-xl', className)}>
      <div className="animate-barrido absolute inset-y-0 w-1/2 bg-gradient-to-r from-transparent via-white/70 to-transparent motion-reduce:hidden" />
    </div>
  )
}
