import { useState } from 'react'

import { cn } from '@/lib/utils'

import type { Foto } from './tipos'

/** Foto principal con miniaturas; si no hay fotos muestra un marcador. */
export function Galeria({
  fotos,
  alto = 'aspect-video',
  className,
}: {
  fotos: Foto[]
  alto?: string
  /** Clases extra para la imagen principal (por ejemplo, quitar el redondeo dentro de una tarjeta). */
  className?: string
}) {
  const [actual, setActual] = useState(0)
  if (fotos.length === 0) {
    return (
      <div
        className={cn(
          'text-primario/50 rounded-tarjeta flex items-center justify-center bg-gradient-to-br from-emerald-50 to-teal-100/70',
          alto,
          className,
        )}
        role="img"
        aria-label="Sin fotos"
      >
        <svg
          viewBox="0 0 24 24"
          className="size-12"
          fill="none"
          stroke="currentColor"
          strokeWidth="1.5"
          strokeLinecap="round"
          strokeLinejoin="round"
          aria-hidden
        >
          <circle cx="12" cy="12" r="9" />
          <path d="m12 8 3.5 2.5-1.3 4h-4.4l-1.3-4Z" />
          <path d="M12 3v5M20 9.5l-4.5 1M17.5 19l-3.3-4.5M6.5 19l3.3-4.5M4 9.5l4.5 1" />
        </svg>
      </div>
    )
  }
  const foto = fotos[Math.min(actual, fotos.length - 1)]
  return (
    <div className="flex flex-col gap-2">
      <img
        src={foto.url}
        alt=""
        className={cn('rounded-tarjeta bg-tenue w-full object-cover', alto, className)}
      />
      {fotos.length > 1 && (
        <ul className="flex gap-2 overflow-x-auto">
          {fotos.map((f, i) => (
            <li key={f.id}>
              <button
                type="button"
                onClick={() => setActual(i)}
                aria-label={`Ver foto ${i + 1}`}
                className={cn(
                  'rounded-boton overflow-hidden ring-2 ring-offset-2 transition-opacity',
                  i === actual ? 'ring-primario' : 'opacity-70 ring-transparent hover:opacity-100',
                )}
              >
                <img src={f.url} alt="" className="h-14 w-20 object-cover" loading="lazy" />
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
