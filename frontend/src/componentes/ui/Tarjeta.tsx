import type { HTMLAttributes } from 'react'

import { cn } from '@/lib/utils'

export function Tarjeta({ className, ...resto }: HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      className={cn(
        'border-borde bg-superficie rounded-tarjeta border p-5 shadow-[0_1px_2px_rgb(15_23_42/0.04)] sm:p-6',
        className,
      )}
      {...resto}
    />
  )
}
