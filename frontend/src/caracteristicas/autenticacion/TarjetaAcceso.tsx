import type { ReactNode } from 'react'

import { cn } from '@/lib/utils'

/** Marco común de las pantallas de acceso: título arriba y una tarjeta centrada. */
export function TarjetaAcceso({
  titulo,
  descripcion,
  ancho = 'md',
  pie,
  children,
}: {
  titulo?: string
  descripcion?: ReactNode
  ancho?: 'md' | 'lg'
  pie?: ReactNode
  children: ReactNode
}) {
  return (
    <div className={cn('flex w-full flex-col gap-6', ancho === 'lg' ? 'max-w-xl' : 'max-w-md')}>
      <div className="text-center">
        {titulo && (
          <div>
            <h1 className="text-2xl font-bold">{titulo}</h1>
            {descripcion && <p className="text-texto-suave mt-1.5 text-sm">{descripcion}</p>}
          </div>
        )}
      </div>
      <div className="border-borde bg-superficie rounded-tarjeta shadow-suave border p-6 sm:p-8">
        {children}
      </div>
      {pie && <p className="text-texto-suave text-center text-sm">{pie}</p>}
    </div>
  )
}
