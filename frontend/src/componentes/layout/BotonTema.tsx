import { Moon, Sun } from 'lucide-react'
import type { MouseEvent } from 'react'

import { cn } from '@/lib/utils'

import { aplicarTema, useTema } from './tema'

/**
 * Alterna entre modo claro y oscuro. Donde el navegador soporta View Transitions, el tema nuevo
 * se revela en un círculo que crece desde el botón; si no, o con movimiento reducido, cambia al
 * instante.
 */
export function BotonTema({ className }: { className?: string }) {
  const tema = useTema()
  const siguiente = tema === 'oscuro' ? 'claro' : 'oscuro'

  const alternar = (e: MouseEvent<HTMLButtonElement>) => {
    const reducido = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches
    if (!document.startViewTransition || reducido) {
      aplicarTema(siguiente)
      return
    }
    const caja = e.currentTarget.getBoundingClientRect()
    const x = caja.left + caja.width / 2
    const y = caja.top + caja.height / 2
    const radio = Math.hypot(Math.max(x, innerWidth - x), Math.max(y, innerHeight - y))
    const raiz = document.documentElement.style
    raiz.setProperty('--tema-x', `${x}px`)
    raiz.setProperty('--tema-y', `${y}px`)
    raiz.setProperty('--tema-r', `${radio}px`)
    document.startViewTransition(() => aplicarTema(siguiente))
  }

  return (
    <button
      type="button"
      onClick={alternar}
      aria-label={siguiente === 'oscuro' ? 'Activar modo oscuro' : 'Activar modo claro'}
      title={siguiente === 'oscuro' ? 'Modo oscuro' : 'Modo claro'}
      className={cn(
        'presionable text-texto-suave hover:bg-tenue hover:text-texto relative inline-flex size-9 items-center justify-center overflow-hidden rounded-full transition-colors',
        className,
      )}
    >
      <Sun
        aria-hidden
        className={cn(
          'absolute size-5 transition-[transform,opacity] duration-500 ease-[var(--ease-salida)]',
          tema === 'oscuro' ? 'scale-100 rotate-0 opacity-100' : 'scale-50 -rotate-90 opacity-0',
        )}
      />
      <Moon
        aria-hidden
        className={cn(
          'absolute size-5 transition-[transform,opacity] duration-500 ease-[var(--ease-salida)]',
          tema === 'oscuro' ? 'scale-50 rotate-90 opacity-0' : 'scale-100 rotate-0 opacity-100',
        )}
      />
    </button>
  )
}
