import { cva } from 'class-variance-authority'

export const estilosBoton = cva(
  'inline-flex items-center justify-center gap-2 rounded-boton font-medium transition-colors disabled:pointer-events-none disabled:opacity-50',
  {
    variants: {
      variante: {
        primario: 'bg-primario text-white hover:bg-primario-oscuro',
        secundario: 'border border-borde bg-superficie text-texto hover:bg-primario-claro',
        peligro: 'bg-peligro text-white hover:opacity-90',
        fantasma: 'text-texto hover:bg-primario-claro',
      },
      tamano: {
        sm: 'h-8 px-3 text-sm',
        md: 'h-10 px-4 text-sm',
        lg: 'h-12 px-6 text-base',
      },
    },
    defaultVariants: { variante: 'primario', tamano: 'md' },
  },
)
