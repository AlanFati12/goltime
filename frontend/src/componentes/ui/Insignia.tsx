import { cva, type VariantProps } from 'class-variance-authority'
import type { HTMLAttributes } from 'react'

import { cn } from '@/lib/utils'

const estilos = cva(
  'inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-xs font-semibold whitespace-nowrap ring-1 ring-inset',
  {
    variants: {
      tono: {
        verde: 'bg-volt-claro text-primario-oscuro ring-primario/20',
        rojo: 'bg-peligro-claro text-peligro ring-peligro/15',
        ambar: 'bg-alerta-claro text-alerta ring-alerta/20',
        azul: 'bg-info-claro text-info ring-info/15',
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
