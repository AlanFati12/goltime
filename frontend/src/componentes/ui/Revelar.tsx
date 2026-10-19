import type { CSSProperties, ElementType, ReactNode } from 'react'

import { cn } from '@/lib/utils'

import { useRevelar } from './useRevelar'

/** Envoltura que entra con un leve ascenso y desenfoque al aparecer; `i` escalona hermanos. */
export function Revelar({
  como: Componente = 'div',
  i = 0,
  imagen,
  className,
  style,
  children,
}: {
  como?: ElementType
  /** Posición entre hermanos: cada paso retrasa la entrada 80 ms. */
  i?: number
  /** Descubre el contenido de abajo hacia arriba (para fotos). */
  imagen?: boolean
  className?: string
  style?: CSSProperties
  children?: ReactNode
}) {
  const ref = useRevelar<HTMLElement>()
  return (
    <Componente
      ref={ref}
      className={cn(imagen ? 'revelar-imagen' : 'revelar', className)}
      style={{ '--i': i, ...style } as CSSProperties}
    >
      {children}
    </Componente>
  )
}
