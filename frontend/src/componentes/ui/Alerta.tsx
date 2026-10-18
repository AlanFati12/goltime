import { cva, type VariantProps } from 'class-variance-authority'
import type { HTMLAttributes } from 'react'

import { cn } from '@/lib/utils'

const estilos = cva('rounded-boton border px-3 py-2 text-sm', {
  variants: {
    tipo: {
      error: 'border-peligro/40 bg-peligro/10 text-peligro',
      exito: 'border-exito/40 bg-exito/10 text-primario-oscuro',
      aviso: 'border-alerta/40 bg-alerta/10 text-alerta',
      info: 'border-borde bg-primario-claro text-texto',
    },
  },
  defaultVariants: { tipo: 'info' },
})

export function Alerta({
  tipo,
  className,
  ...resto
}: HTMLAttributes<HTMLDivElement> & VariantProps<typeof estilos>) {
  return (
    <div
      role={tipo === 'error' ? 'alert' : 'status'}
      className={cn(estilos({ tipo }), className)}
      {...resto}
    />
  )
}
