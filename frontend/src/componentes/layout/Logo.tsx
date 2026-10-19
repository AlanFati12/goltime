import { cn } from '@/lib/utils'

/**
 * Marca de Goltime: el círculo central de la cancha con las agujas de un reloj adentro
 * (gol + time) y la línea media que lo atraviesa. Con `animado`, las agujas giran: sirve de
 * indicador de carga con la misma identidad.
 */
export function MarcaGoltime({
  className,
  fondo = true,
  animado,
}: {
  className?: string
  /** Cuadro azul medianoche detrás del símbolo. */
  fondo?: boolean
  animado?: boolean
}) {
  return (
    <svg viewBox="0 0 32 32" className={cn('size-9 shrink-0', className)} aria-hidden>
      {fondo && <rect width="32" height="32" rx="9" className="fill-noche" />}
      <path
        d="M16 2.5v5M16 24.5v5"
        className="stroke-volt"
        strokeWidth="1.6"
        strokeLinecap="round"
        opacity=".5"
      />
      <circle cx="16" cy="16" r="8.5" fill="none" className="stroke-volt" strokeWidth="2.2" />
      <path
        d="M16 16V10.6"
        className={cn('stroke-volt-vivo origin-center', animado && 'animate-girar-lento')}
        strokeWidth="2.2"
        strokeLinecap="round"
      />
      <path
        d="M16 16l3.4 2.2"
        className="stroke-volt-vivo"
        strokeWidth="2.2"
        strokeLinecap="round"
      />
      <circle cx="16" cy="16" r="1.7" className="fill-volt-vivo" />
    </svg>
  )
}

/** Logo completo: símbolo y nombre. `claro` es para fondos oscuros. */
export function Logo({ className, claro }: { className?: string; claro?: boolean }) {
  return (
    <span className={cn('inline-flex items-center gap-2.5', className)}>
      <MarcaGoltime className={claro ? 'rounded-[9px] ring-1 ring-white/10' : undefined} />
      <span
        className={cn(
          'font-titulo text-[1.65rem] leading-none font-extrabold tracking-[-0.01em] uppercase',
          claro ? 'text-white' : 'text-texto',
        )}
      >
        Gol<span className={claro ? 'text-volt-vivo' : 'text-primario'}>time</span>
      </span>
    </span>
  )
}
