import { cva, type VariantProps } from 'class-variance-authority'
import type { HTMLAttributes } from 'react'

import { cn } from '@/lib/utils'

const estilos = cva(
  'inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-xs font-semibold whitespace-nowrap ring-1 ring-inset',
  {
    variants: {
      tono: {
        verde: 'bg-volt-claro text-primario-oscuro ring-primario/20',
        rojo: 'bg-red-50 text-peligro ring-peligro/15',
        ambar: 'bg-amber-50 text-alerta ring-alerta/20',
        azul: 'bg-blue-50 text-info ring-info/15',
        gris: 'bg-tenue text-texto-suave ring-borde',
      },
    },
    defaultVariants: { tono: 'gris' },
  },
)

export function Insignia({
  tono,
  className,
  ...resto
}: HTMLAttributes<HTMLSpanElement> & VariantProps<typeof estilos>) {
  return <span className={cn(estilos({ tono }), className)} {...resto} />
}
