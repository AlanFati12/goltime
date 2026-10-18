import type { VariantProps } from 'class-variance-authority'
import { Link, type LinkProps } from 'react-router-dom'

import { cn } from '@/lib/utils'

import { estilosBoton } from './estilosBoton'

/** Enlace de navegación con aspecto de botón (evita anidar un botón dentro de un enlace). */
export function EnlaceBoton({
  className,
  variante,
  tamano,
  ...resto
}: LinkProps & VariantProps<typeof estilosBoton>) {
  return <Link className={cn(estilosBoton({ variante, tamano }), className)} {...resto} />
}
