import type { VariantProps } from 'class-variance-authority'
import type { ButtonHTMLAttributes } from 'react'

import { cn } from '@/lib/utils'

import { estilosBoton } from './estilosBoton'

export interface PropsBoton
  extends ButtonHTMLAttributes<HTMLButtonElement>, VariantProps<typeof estilosBoton> {}

export function Boton({ className, variante, tamano, type = 'button', ...resto }: PropsBoton) {
  return (
    <button type={type} className={cn(estilosBoton({ variante, tamano }), className)} {...resto} />
  )
}
