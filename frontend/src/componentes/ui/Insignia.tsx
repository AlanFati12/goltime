import { cva, type VariantProps } from 'class-variance-authority'
import type { HTMLAttributes } from 'react'

import { cn } from '@/lib/utils'

const estilos = cva('inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium', {
  variants: {
    tono: {
      verde: 'bg-primario-claro text-primario-oscuro',
      rojo: 'bg-peligro/10 text-peligro',
      ambar: 'bg-alerta/10 text-alerta',
      gris: 'bg-borde text-texto-suave',
    },
  },
  defaultVariants: { tono: 'gris' },
})

export function Insignia({
  tono,
  className,
  ...resto
}: HTMLAttributes<HTMLSpanElement> & VariantProps<typeof estilos>) {
  return <span className={cn(estilos({ tono }), className)} {...resto} />
}
