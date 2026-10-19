import { useIsFetching } from '@tanstack/react-query'

import { cn } from '@/lib/utils'

/** Línea fina en la parte superior mientras hay consultas en curso. */
export function BarraCarga() {
  const cargando = useIsFetching() > 0
  return (
    <div
      aria-hidden
      className={cn(
        'pointer-events-none fixed inset-x-0 top-0 z-[70] h-[3px] overflow-hidden transition-opacity duration-300',
        cargando ? 'opacity-100' : 'opacity-0',
      )}
    >
      <div className="bg-volt animate-barrido h-full w-1/3 shadow-[0_0_12px_var(--color-volt)]" />
    </div>
  )
}
