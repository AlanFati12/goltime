import { cn } from '@/lib/utils'

/** Estrellas de solo lectura (valor de 0 a 5). */
export function Estrellas({ valor, className }: { valor: number; className?: string }) {
  return (
    <span
      className={cn('text-acento tracking-tight', className)}
      role="img"
      aria-label={`${valor} de 5 estrellas`}
    >
      {[1, 2, 3, 4, 5].map((i) => (
        <span key={i} aria-hidden="true" className={i <= Math.round(valor) ? '' : 'opacity-25'}>
          ★
        </span>
      ))}
    </span>
  )
}
