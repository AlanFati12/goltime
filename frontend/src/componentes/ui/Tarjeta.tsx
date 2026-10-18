import type { HTMLAttributes } from 'react'

import { cn } from '@/lib/utils'

export function Tarjeta({ className, ...resto }: HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      className={cn('border-borde bg-superficie rounded-tarjeta border p-5 shadow-sm', className)}
      {...resto}
    />
  )
}
