import { LoaderCircle } from 'lucide-react'

import { cn } from '@/lib/utils'

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
        'text-texto-suave flex items-center justify-center gap-2 py-10 text-sm',
        className,
      )}
    >
      <LoaderCircle className="text-primario size-4 animate-spin" aria-hidden />
      {texto}
    </div>
  )
}
