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
      className={cn('text-texto-suave flex items-center gap-2 py-6 text-sm', className)}
    >
      <span
        className="border-primario size-4 animate-spin rounded-full border-2 border-t-transparent"
        aria-hidden
      />
      {texto}
    </div>
  )
}
