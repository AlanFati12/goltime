import { useState } from 'react'

import { cn } from '@/lib/utils'

import type { Foto } from './tipos'

/** Foto principal con miniaturas; si no hay fotos muestra un marcador. */
export function Galeria({ fotos, alto = 'aspect-video' }: { fotos: Foto[]; alto?: string }) {
  const [actual, setActual] = useState(0)
  if (fotos.length === 0) {
    return (
      <div
        className={cn(
          'bg-primario-claro text-primario-oscuro rounded-tarjeta flex items-center justify-center text-sm',
          alto,
        )}
        role="img"
        aria-label="Sin fotos"
      >
        Sin fotos todavía
      </div>
    )
  }
  const foto = fotos[Math.min(actual, fotos.length - 1)]
  return (
    <div className="flex flex-col gap-2">
      <img src={foto.url} alt="" className={cn('rounded-tarjeta w-full object-cover', alto)} />
      {fotos.length > 1 && (
        <ul className="flex gap-2 overflow-x-auto">
          {fotos.map((f, i) => (
            <li key={f.id}>
              <button
                type="button"
                onClick={() => setActual(i)}
                aria-label={`Ver foto ${i + 1}`}
                className={cn(
                  'rounded-boton overflow-hidden border-2',
                  i === actual ? 'border-primario' : 'border-transparent',
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
