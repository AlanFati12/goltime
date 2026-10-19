import type { HTMLAttributes, ReactNode } from 'react'

import { cn } from '@/lib/utils'

export function Tarjeta({ className, ...resto }: HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      className={cn('border-borde/80 bg-superficie rounded-tarjeta border p-5 sm:p-6', className)}
      {...resto}
    />
  )
}

/** Encabezado interno de una tarjeta: título, descripción opcional y acciones a la derecha. */
export function TituloTarjeta({
  titulo,
  descripcion,
  acciones,
  className,
}: {
  titulo: ReactNode
  descripcion?: ReactNode
  acciones?: ReactNode
  className?: string
}) {
  return (
    <div className={cn('mb-4 flex flex-wrap items-start justify-between gap-3', className)}>
      <div className="min-w-0">
        <h2 className="text-xl font-bold">{titulo}</h2>
        {descripcion && <p className="text-texto-suave mt-0.5 text-sm">{descripcion}</p>}
      </div>
      {acciones && <div className="flex items-center gap-2">{acciones}</div>}
    </div>
  )
}
