import { cn } from '@/lib/utils'

/** Marca de Goltime: balón estilizado sobre un cuadro verde y el nombre al lado. */
export function Logo({ className, claro }: { className?: string; claro?: boolean }) {
  return (
    <span className={cn('inline-flex items-center gap-2', className)}>
      <svg viewBox="0 0 32 32" className="size-8 shrink-0" aria-hidden>
        <rect width="32" height="32" rx="9" fill="#059669" />
        <polygon points="16,10 21,13.6 19.1,19.5 12.9,19.5 11,13.6" fill="#fff" />
        <path
          d="M16 10V5.5M21 13.6l4.6-1.8M19.1 19.5l3 4.1M12.9 19.5l-3 4.1M11 13.6l-4.6-1.8"
          stroke="#fff"
          strokeWidth="2"
          strokeLinecap="round"
        />
      </svg>
      <span
        className={cn(
          'font-titulo text-xl font-extrabold tracking-tight',
          claro ? 'text-white' : 'text-texto',
        )}
      >
        Gol<span className={claro ? 'text-emerald-300' : 'text-primario'}>time</span>
      </span>
    </span>
  )
}
