import { cva, type VariantProps } from 'class-variance-authority'
import { CircleAlert, CircleCheck, Info, TriangleAlert } from 'lucide-react'
import type { HTMLAttributes } from 'react'

import { cn } from '@/lib/utils'

const estilos = cva('rounded-boton flex items-start gap-2.5 border px-3.5 py-3 text-sm', {
  variants: {
    tipo: {
      error: 'border-peligro/25 bg-peligro/5 text-peligro',
      exito: 'border-exito/25 bg-primario-claro text-primario-oscuro',
      aviso: 'border-alerta/25 bg-amber-50 text-alerta',
      info: 'border-info/20 bg-blue-50 text-blue-900',
    },
  },
  defaultVariants: { tipo: 'info' },
})

const ICONOS = { error: CircleAlert, exito: CircleCheck, aviso: TriangleAlert, info: Info }

export function Alerta({
  tipo,
  className,
  children,
  ...resto
}: HTMLAttributes<HTMLDivElement> & VariantProps<typeof estilos>) {
  const Icono = ICONOS[tipo ?? 'info']
  return (
    <div
      role={tipo === 'error' ? 'alert' : 'status'}
      className={cn(estilos({ tipo }), className)}
      {...resto}
    >
      <Icono className="mt-px size-4 shrink-0" aria-hidden />
      <div className="min-w-0 flex-1">{children}</div>
    </div>
  )
}
